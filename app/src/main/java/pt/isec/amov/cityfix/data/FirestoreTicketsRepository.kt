package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
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

/**
 * Tickets, categories and dashboard figures in Cloud Firestore (brief IV; decisions 0009, 0010).
 * Create one per app with CityFixApplication.firestore (the instance with the offline cache).
 *
 * Reads are live: each one is a snapshot listener (beyond class: the demo uses one-shot get())
 * turned into a Flow, so a change on another device, or a report on this one, reaches the
 * ViewModel without a refresh. Offline, the listeners are served from the local cache.
 *
 * 10 m grouping (brief I.2 Obs., decision 0002): before a new ticket is created, [reportTicket]
 * reads the `ticketLocations` with the same category and situation that are active (three
 * equality filters, served by Firestore's automatic single-field indexes, so no composite index)
 * and runs TicketsList.findNearby on them in the app, because Firestore has no geo query.
 * Offline, the query answers from the local cache.
 *
 * Privacy (CLAUDE.md, decision 0006): [publicTickets] reads only `publicTickets`, never `tickets`
 * or `ticketLocations`, so the anonymous code path never receives author data or exact
 * coordinates.
 *
 * Messages (F-014): each ticket has a `messages` subcollection. [messagesFor] is live like the
 * other reads and [sendMessage] is queued offline like the report batch. Neither is ever called
 * for anonymous or pending users (brief I.1.C).
 */
class FirestoreTicketsRepository(
    private val firestore: FirebaseFirestore,
    private val uploader: AttachmentUploader,
) : TicketsRepository {

    /** Active publicTickets (anonymous projection), newest first. Never reads tickets/. */
    override fun publicTickets(): Flow<List<Ticket>> =
        firestore.collection(PUBLIC_TICKETS)
            .whereIn("currentStatus", ACTIVE_STATUS_NAMES)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toPublicTicket() }).getAllSorted()
            }

    /** Full tickets whose participantIds contain [userId] (creator or follower), newest first. */
    override fun participantTickets(userId: String): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .whereArrayContains("participantIds", userId)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    /** Every full ticket, newest first (operators). */
    override fun allTickets(): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    /** The participant query read once with get(), as the class demo reads (F-016 worker). */
    override suspend fun participantTicketsOnce(userId: String): List<Ticket> =
        firestore.collection(TICKETS)
            .whereArrayContains("participantIds", userId)
            .get()
            .await()
            .documents.map { it.toTicket() }

    /** Every full ticket read once with get() (F-016 worker, operators). */
    override suspend fun allTicketsOnce(): List<Ticket> =
        firestore.collection(TICKETS)
            .get()
            .await()
            .documents.map { it.toTicket() }

    /** Dashboard figures from stats/summary (decision 0005 meaning; private tickets counted). */
    override fun stats(): Flow<DashboardStats> =
        firestore.document(STATS_SUMMARY)
            .listen { snapshot -> snapshot.toDashboardStats() }

    /** Active categories (I.3.E), sorted by name with Collator (locale-aware). */
    override fun activeCategories(): Flow<List<Category>> =
        firestore.collection(CATEGORIES)
            .whereEqualTo("active", true)
            .listen { snapshot ->
                val collator = Collator.getInstance()
                snapshot.documents.map { it.toCategory() }
                    .sortedWith { a, b -> collator.compare(a.name, b.name) }
            }

    /**
     * Saves a report from the form at [location] (the reporter's GPS fix). Returns null when
     * description, categoryId or categoryName is blank (all three are trimmed), attachments is
     * empty or location is the unset (0, 0). [creatorId] is the reporter.
     *
     * An active ticket with the same category and situation within 10 m is joined instead
     * ([joinTicket]). Otherwise one batch (decision 0010) creates tickets/{id},
     * ticketLocations/{id}, publicTickets/{id} for PUBLIC tickets, and adds the stats/summary
     * counters. Returns once the batch is queued. The media are prepared first
     * (AttachmentUploader.prepare) and one upload worker for the ticket enqueued after the batch.
     */
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

    /** The thread of ticket [ticketId], oldest first (sorted here, so no index is needed). */
    override fun messagesFor(ticketId: String): Flow<List<Message>> =
        firestore.collection(TICKETS).document(ticketId).collection(MESSAGES)
            .listen { snapshot ->
                snapshot.documents.map { it.toMessage() }.sortedBy { it.sentAt }
            }

    /**
     * Adds a message by [author] to ticket [ticketId]; false when the trimmed [text] is empty.
     * One batch (F-016 Q4): the message and the ticket's lastMessageAt/lastMessageAuthorId, so
     * the tickets listener of the other side sees that there is a new message.
     * Not awaited, as in reportTicket: offline the write is queued and this device's listener
     * shows the message at once; failures are only logged.
     */
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

    /**
     * Reopens a RESOLVED [ticket] (brief II.4). The lifecycle (D-008 transition) checks the
     * participant, the 5-day window and the justification; false when it rejects the move.
     */
    override fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean {
        val after = ticket.transition(TicketStatus.OPEN, by, System.currentTimeMillis(), justification)
            ?: return false
        commitStatusChange(ticket, after)
        return true
    }

    /**
     * Writes a RESOLVED [ticket] whose reopen window has ended as CLOSED (brief II), with the
     * entry by "system" at the end of the window (D-008 closeIfExpired). Nothing otherwise.
     */
    override fun closeExpiredTicket(ticket: Ticket) {
        val after = ticket.closeIfExpired(System.currentTimeMillis()) ?: return
        commitStatusChange(ticket, after)
    }

    /**
     * A citizen's clarification request (brief II.2, F-014 Q1): D-008's transition to
     * INFO_REQUESTED with the question as justification, and the question as a message, in the
     * same batch. False when the lifecycle rejects the move.
     */
    override fun askForClarification(ticket: Ticket, by: User, question: String): Boolean {
        val now = System.currentTimeMillis()
        val after = ticket.transition(TicketStatus.INFO_REQUESTED, by, now, question) ?: return false
        commitStatusChange(ticket, after, messageDocument(question.trim(), now, by))
        return true
    }

    /**
     * The operator starts work (brief II.3, D-013): D-008's transition to IN_PROGRESS with the
     * optional [note] as justification, written with commitStatusChange. False when the
     * lifecycle rejects the move.
     */
    override fun startWork(ticket: Ticket, by: User, note: String?): Boolean {
        val after = ticket.transition(TicketStatus.IN_PROGRESS, by, System.currentTimeMillis(), note)
            ?: return false
        commitStatusChange(ticket, after)
        return true
    }

    /**
     * The operator resolves [ticket] with [proof] (brief I.3.E, D-013), on Dispatchers.IO:
     * 1. a dry run of D-008's transition with the raw files, so a rejected move prepares nothing;
     * 2. each proof prepared (compressed copy + thumbnail); if none is PENDING (all failed) it
     *    stops, and the originals are still there (prepare deletes them only on success);
     * 3. the real transition with the prepared proof (phase RESOLUTION, uploadedById = operator);
     * 4. one batch (commitStatusChange) with the new proof entries appended by arrayUnion;
     * 5. one upload worker for the ticket document. Its thumbnails never go to publicTickets
     *    (decision 0010: only the creator's REPORT media there).
     */
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

    /**
     * Sets only `lastOperatorNearbyAt` on the ticket (D-014); false when canRecordOperatorNearby
     * rejects it. Not awaited, as in sendMessage: offline the update is queued; failures are only
     * logged, without the location.
     */
    override fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean {
        if (!ticket.canRecordOperatorNearby(now)) return false
        firestore.collection(TICKETS).document(ticket.id)
            .update("lastOperatorNearbyAt", now)
            .addOnFailureListener { Log.w(TAG, "nearby not saved", it) }
        return true
    }

    /**
     * One batch for a status change from [before] to [after] (decisions 0010, 0013, 0014):
     * the ticket's status fields, the publicTickets mirror for a PUBLIC ticket, whether
     * ticketLocations still groups (`active`), the stats/summary counters and, when given,
     * [message] as a new message in the thread. Not awaited, as in reportTicket. D-013 reuses
     * it for the operator's moves.
     *
     * [newAttachments] (D-013, the resolution proof) are appended with FieldValue.arrayUnion
     * instead of rewriting `attachments`, so an UploadWorker updating the creator's entries at the
     * same moment is not overwritten by this batch.
     */
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

    /** The nearest active ticket with this category and situation within 10 m, or null. */
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

    /**
     * Makes [creatorId] follow ticket [ticketId] (decision 0010): one batch writes their report
     * (the form fields and their [attachments], prepared by the uploader) to
     * tickets/{id}/reports/{uid} and adds their uid to followerIds and participantIds
     * (FieldValue.arrayUnion, beyond class: adds to the array only if missing). The ticket's own
     * attachments, publicTickets and stats/summary do not change. If the reporter already
     * created or follows the ticket (the F-011 participant query), nothing is written and the
     * media are not prepared.
     */
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

    /**
     * Turns a snapshot listener on this query into a Flow (beyond class).
     *
     * callbackFlow is the flow { } builder for callback APIs: the listener calls trySend for each
     * new snapshot, and awaitClose runs when the collector is cancelled (for example when the
     * ViewModel's viewModelScope ends), removing the listener so it does not leak.
     * A listener error is only logged: Firestore then stops that listener, so the flow sends
     * nothing more (live updates stop) and the ViewModel keeps showing the last list it received,
     * until the flow is collected again (screen reopened or user changed).
     */
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

    /** Same as the Query version, for a single document (stats/summary). */
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

        /**
         * Names of the active statuses for the publicTickets query: the same statuses as
         * Ticket.isActive (decision 0005).
         */
        private val ACTIVE_STATUS_NAMES: List<String> =
            listOf(TicketStatus.OPEN, TicketStatus.INFO_REQUESTED, TicketStatus.IN_PROGRESS).map { it.name }
    }
}
