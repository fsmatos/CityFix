package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentPhase
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.model.canRecordOperatorNearby
import pt.isec.amov.cityfix.model.closeIfExpired
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.isActive
import pt.isec.amov.cityfix.model.transition
import java.text.Collator

class FirestoreTicketsRepository(
    private val firestore: FirebaseFirestore,
    private val uploader: AttachmentUploader,
) : TicketsRepository {

    override fun publicTickets(): Flow<List<Ticket>> =
        firestore.collection(PUBLIC_TICKETS)
            .whereIn("currentStatus", ACTIVE_STATUS_NAMES)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toPublicTicket() }).getAllSorted()
            }

    override fun participantTickets(userId: String): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .whereArrayContains("participantIds", userId)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    override fun allTickets(): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    override suspend fun participantTicketsOnce(userId: String): List<Ticket> =
        firestore.collection(TICKETS)
            .whereArrayContains("participantIds", userId)
            .get(Source.SERVER)
            .await()
            .documents.map { it.toTicket() }

    override suspend fun allTicketsOnce(): List<Ticket> =
        firestore.collection(TICKETS)
            .get(Source.SERVER)
            .await()
            .documents.map { it.toTicket() }

    override fun ticketsFromServer(userId: String, isOperator: Boolean): Flow<List<Ticket>> {
        val query: Query = if (isOperator) {
            firestore.collection(TICKETS)
        } else {
            firestore.collection(TICKETS).whereArrayContains("participantIds", userId)
        }
        return callbackFlow {
            // INCLUDE: Firestore also calls back when the same data is confirmed by the server.
            val registration = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.metadata.isFromCache) {
                    trySend(snapshot.documents.map { it.toTicket() })
                }
            }
            awaitClose { registration.remove() }
        }
    }

    override fun stats(): Flow<DashboardStats> =
        firestore.document(STATS_SUMMARY)
            .listen { snapshot -> snapshot.toDashboardStats() }

    override fun activeCategories(): Flow<List<Category>> =
        firestore.collection(CATEGORIES)
            .whereEqualTo("active", true)
            .listen { snapshot ->
                val collator = Collator.getInstance()
                snapshot.documents.map { it.toCategory() }
                    .sortedWith { a, b -> collator.compare(a.name, b.name) }
            }

    override suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
        location: Coordinates,
        creatorId: String,
    ): ReportOutcome? = withContext(Dispatchers.IO) {
        val text = description.trim()
        val cleanCategoryId = categoryId.trim()
        val cleanCategoryName = categoryName.trim()
        if (text.isBlank() || cleanCategoryId.isBlank() || cleanCategoryName.isBlank() ||
            attachments.isEmpty() || location == Coordinates()
        ) {
            return@withContext null
        }

        val now = System.currentTimeMillis()

        val match = findMatch(location, cleanCategoryId, situation)
        if (match != null) {
            return@withContext joinTicket(
                match.id, cleanCategoryId, situation, location, text, now, attachments, creatorId,
            )
        }

        // Compressed copies and thumbnails (F-013); re-encoding also drops the photos' EXIF,
        // so they no longer carry the place they were taken (decision 0006).
        val prepared = attachments.map { uploader.prepare(it, creatorId) }

        // document() with no argument creates a new auto id locally, so it works offline.
        val ref = firestore.collection(TICKETS).document()
        val ticket = Ticket(
            id = ref.id,
            description = text,
            situation = situation,
            scope = scope,
            location = location,
            currentStatus = TicketStatus.OPEN,
            createdAt = now,
            categoryId = cleanCategoryId,
            categoryName = cleanCategoryName,
            creatorId = creatorId,
            statusHistory = listOf(StatusHistory(TicketStatus.OPEN, now, null, creatorId)),
            attachments = prepared,
        )

        // One batch, like the demo's "clear all": every document changes together or none does.
        val batch = firestore.batch()
        batch.set(ref, ticket.toTicketDocument())
        batch.set(
            firestore.collection(TICKET_LOCATIONS).document(ticket.id),
            ticket.toTicketLocationDocument(),
        )
        if (scope == Scope.PUBLIC) {
            batch.set(
                firestore.collection(PUBLIC_TICKETS).document(ticket.id),
                ticket.toPublicTicketDocument(),
            )
        }
        // merge() adds the counters to the existing document (or creates it) instead of
        // replacing it.
        batch.set(
            firestore.document(STATS_SUMMARY),
            statsIncrements(null, ticket),
            SetOptions.merge(),
        )

        // No await(): offline, commit() completes only when the server answers, which would keep
        // the form open. The local cache applies the batch at once, so this device's listeners
        // show the ticket immediately; failures (e.g. rules) are only logged.
        batch.commit().addOnFailureListener { Log.w(TAG, "report not saved", it) }

        // One upload worker for the ticket's media, after the batch (F-013). The creator's
        // REPORT media of a PUBLIC ticket add their thumbnails to publicTickets.
        if (ticket.attachments.any { it.uploadState == UploadState.PENDING }) {
            uploader.enqueue("$TICKETS/${ticket.id}", ticket.id, scope == Scope.PUBLIC)
        }
        ReportOutcome.CREATED
    }

    override fun messagesFor(ticketId: String): Flow<List<Message>> =
        firestore.collection(TICKETS).document(ticketId).collection(MESSAGES)
            .listen { snapshot ->
                snapshot.documents.map { it.toMessage() }.sortedBy { it.sentAt }
            }

    override fun sendMessage(ticketId: String, text: String, author: User): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val message = messageDocument(trimmed, System.currentTimeMillis(), author)
        val ticketRef = firestore.collection(TICKETS).document(ticketId)
        val batch = firestore.batch()
        batch.set(ticketRef.collection(MESSAGES).document(), message)
        batch.update(ticketRef, lastMessageFields(message))
        batch.commit().addOnFailureListener { Log.w(TAG, "message not saved", it) }
        return true
    }

    override fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean {
        val after = ticket.transition(TicketStatus.OPEN, by, System.currentTimeMillis(), justification)
            ?: return false
        commitStatusChange(ticket, after)
        return true
    }

    override fun closeExpiredTicket(ticket: Ticket) {
        val after = ticket.closeIfExpired(System.currentTimeMillis()) ?: return
        commitStatusChange(ticket, after)
    }

    override fun askForClarification(ticket: Ticket, by: User, question: String): Boolean {
        val now = System.currentTimeMillis()
        val after = ticket.transition(TicketStatus.INFO_REQUESTED, by, now, question) ?: return false
        commitStatusChange(ticket, after, messageDocument(question.trim(), now, by))
        return true
    }

    override fun startWork(ticket: Ticket, by: User, note: String?): Boolean {
        val after = ticket.transition(TicketStatus.IN_PROGRESS, by, System.currentTimeMillis(), note)
            ?: return false
        commitStatusChange(ticket, after)
        return true
    }

    override suspend fun resolveTicket(
        ticket: Ticket,
        by: User,
        proof: List<Attachment>,
        note: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (ticket.transition(TicketStatus.RESOLVED, by, now, note, proof) == null) return@withContext false

        val prepared = proof.map { uploader.prepare(it, by.id) }
        if (prepared.none { it.uploadState == UploadState.PENDING }) return@withContext false

        val after = ticket.transition(TicketStatus.RESOLVED, by, now, note, prepared)
            ?: return@withContext false
        commitStatusChange(ticket, after, newAttachments = after.attachments.drop(ticket.attachments.size))

        if (after.attachments.any { it.phase == AttachmentPhase.RESOLUTION && it.uploadState == UploadState.PENDING }) {
            uploader.enqueue("$TICKETS/${after.id}", after.id, false)
        }
        true
    }

    override fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean {
        if (!ticket.canRecordOperatorNearby(now)) return false
        firestore.collection(TICKETS).document(ticket.id)
            .update("lastOperatorNearbyAt", now)
            .addOnFailureListener { Log.w(TAG, "nearby not saved", it) }
        return true
    }

    private fun commitStatusChange(
        before: Ticket,
        after: Ticket,
        message: Map<String, Any>? = null,
        newAttachments: List<Attachment> = emptyList(),
    ) {
        val ticketRef = firestore.collection(TICKETS).document(after.id)
        val batch = firestore.batch()
        var fields: Map<String, Any> = if (newAttachments.isEmpty()) {
            after.toStatusFields()
        } else {
            after.toStatusFields() +
                ("attachments" to FieldValue.arrayUnion(*newAttachments.map { it.toDocument() }.toTypedArray()))
        }
        // F-016 Q4: a message in the same batch also marks the ticket as having a new message.
        if (message != null) fields = fields + lastMessageFields(message)
        batch.update(ticketRef, fields)
        if (message != null) {
            batch.set(ticketRef.collection(MESSAGES).document(), message)
        }
        if (after.scope == Scope.PUBLIC) {
            batch.update(
                firestore.collection(PUBLIC_TICKETS).document(after.id),
                "currentStatus", after.currentStatus.name,
            )
        }
        batch.update(
            firestore.collection(TICKET_LOCATIONS).document(after.id),
            "active", after.isActive,
        )
        batch.set(firestore.document(STATS_SUMMARY), statsIncrements(before, after), SetOptions.merge())
        batch.commit().addOnFailureListener { Log.w(TAG, "status change not saved", it) }
    }

    private suspend fun findMatch(
        location: Coordinates,
        categoryId: String,
        situation: Situation,
    ): Ticket? {
        val snapshot = firestore.collection(TICKET_LOCATIONS)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("situation", situation.name)
            .whereEqualTo("active", true)
            .get()
            .await()
        val candidates = snapshot.documents.map { it.toTicketLocation() }
        return TicketsList(candidates).findNearby(location, categoryId, situation)
    }

    private suspend fun joinTicket(
        ticketId: String,
        categoryId: String,
        situation: Situation,
        location: Coordinates,
        description: String,
        createdAt: Long,
        attachments: List<Attachment>,
        creatorId: String,
    ): ReportOutcome {
        val mine = firestore.collection(TICKETS)
            .whereArrayContains("participantIds", creatorId)
            .get()
            .await()
        if (mine.documents.any { it.id == ticketId }) return ReportOutcome.ALREADY_FOLLOWING

        val prepared = attachments.map { uploader.prepare(it, creatorId) }
        val report = reportDocument(categoryId, situation, location, description, createdAt, prepared)

        val ticketRef = firestore.collection(TICKETS).document(ticketId)
        val batch = firestore.batch()
        batch.set(ticketRef.collection(REPORTS).document(creatorId), report)
        batch.update(
            ticketRef,
            mapOf(
                "followerIds" to FieldValue.arrayUnion(creatorId),
                "participantIds" to FieldValue.arrayUnion(creatorId),
            ),
        )
        // Not awaited, as in reportTicket: offline the form would wait for the server.
        batch.commit().addOnFailureListener { Log.w(TAG, "join not saved", it) }

        // The joiner's media are uploaded into their report and never go to publicTickets.
        if (prepared.any { it.uploadState == UploadState.PENDING }) {
            uploader.enqueue("$TICKETS/$ticketId/$REPORTS/$creatorId", ticketId, false)
        }
        return ReportOutcome.JOINED
    }

    private fun <T> Query.listen(map: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(map(snapshot))
        }
        awaitClose { registration.remove() }
    }

    private fun <T> DocumentReference.listen(map: (DocumentSnapshot) -> T): Flow<T> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(map(snapshot))
        }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val TAG = "FirestoreTickets"

        private val ACTIVE_STATUS_NAMES: List<String> =
            listOf(TicketStatus.OPEN, TicketStatus.INFO_REQUESTED, TicketStatus.IN_PROGRESS).map { it.name }
    }
}
