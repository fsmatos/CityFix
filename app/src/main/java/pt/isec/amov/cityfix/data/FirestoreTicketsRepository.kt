package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.model.isActive
import pt.isec.amov.cityfix.utils.FileUtils
import java.text.Collator

/**
 * Tickets, categories and dashboard figures in Cloud Firestore (brief IV; decisions 0009, 0010).
 * Create one per app with CityFixApplication.firestore (the instance with the offline cache).
 *
 * Reads are live: each one is a snapshot listener (beyond class: the demo uses one-shot get())
 * turned into a Flow, so a change on another device, or a report on this one, reaches the
 * ViewModel without a refresh. Offline, the listeners are served from the local cache.
 *
 * Privacy (CLAUDE.md, decision 0006): [publicTickets] reads only `publicTickets`, never `tickets`
 * or `ticketLocations`, so the anonymous code path never receives author data or exact
 * coordinates.
 */
class FirestoreTicketsRepository(private val firestore: FirebaseFirestore) {

    /** Active publicTickets (anonymous projection), newest first. Never reads tickets/. */
    fun publicTickets(): Flow<List<Ticket>> =
        firestore.collection(PUBLIC_TICKETS)
            .whereIn("currentStatus", ACTIVE_STATUS_NAMES)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toPublicTicket() }).getAllSorted()
            }

    /** Full tickets whose participantIds contain [userId] (creator or follower), newest first. */
    fun participantTickets(userId: String): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .whereArrayContains("participantIds", userId)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    /** Every full ticket, newest first (operators). */
    fun allTickets(): Flow<List<Ticket>> =
        firestore.collection(TICKETS)
            .listen { snapshot ->
                TicketsList(snapshot.documents.map { it.toTicket() }).getAllSorted()
            }

    /** Dashboard figures from stats/summary (decision 0005 meaning; private tickets counted). */
    fun stats(): Flow<DashboardStats> =
        firestore.document(STATS_SUMMARY)
            .listen { snapshot -> snapshot.toDashboardStats() }

    /** Active categories (I.3.E), sorted by name with Collator (locale-aware). */
    fun activeCategories(): Flow<List<Category>> =
        firestore.collection(CATEGORIES)
            .whereEqualTo("active", true)
            .listen { snapshot ->
                val collator = Collator.getInstance()
                snapshot.documents.map { it.toCategory() }
                    .sortedWith { a, b -> collator.compare(a.name, b.name) }
            }

    /**
     * Opens a new ticket from the report form. Returns its id, or null when description,
     * categoryId or categoryName is blank (all three are trimmed) or attachments is empty.
     * [creatorId] is the reporter. No location yet (F-012), so no 10 m grouping.
     *
     * Writes one batch (decision 0010): tickets/{id}, ticketLocations/{id}, publicTickets/{id}
     * for PUBLIC tickets, and the stats/summary counters. Returns once the batch is queued.
     */
    suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
        creatorId: String,
    ): String? = withContext(Dispatchers.IO) {
        val text = description.trim()
        val cleanCategoryId = categoryId.trim()
        val cleanCategoryName = categoryName.trim()
        if (text.isBlank() || cleanCategoryId.isBlank() || cleanCategoryName.isBlank() ||
            attachments.isEmpty()
        ) {
            return@withContext null
        }

        // Photos lose their GPS EXIF tags before the ticket is stored (decision 0006, F-009).
        // Result ignored on purpose (F-009 Q2): if a format cannot be written, e.g. HEIC, the
        // attachment and the report are kept and the file keeps its tags.
        attachments.filter { it.type == AttachmentType.IMAGE }
            .forEach { FileUtils.removeImageLocation(it.file) }

        // document() with no argument creates a new auto id locally, so it works offline.
        val ref = firestore.collection(TICKETS).document()
        val now = System.currentTimeMillis()
        val ticket = Ticket(
            id = ref.id,
            description = text,
            situation = situation,
            scope = scope,
            currentStatus = TicketStatus.OPEN,
            createdAt = now,
            categoryId = cleanCategoryId,
            categoryName = cleanCategoryName,
            creatorId = creatorId,
            statusHistory = listOf(StatusHistory(TicketStatus.OPEN, now, null, creatorId)),
            attachments = attachments.map {
                it.copy(uploadState = UploadState.PENDING, uploadedById = creatorId)
            },
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
        ticket.id
    }

    /**
     * Turns a snapshot listener on this query into a Flow (beyond class).
     *
     * callbackFlow is the flow { } builder for callback APIs: the listener calls trySend for each
     * new snapshot, and awaitClose runs when the collector is cancelled (for example when the
     * ViewModel's viewModelScope ends), removing the listener so it does not leak.
     * A listener error is logged and the flow keeps its last value instead of failing, so the
     * collector never throws and the list stays as last seen.
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
         * Names of the active statuses for the publicTickets query, taken from Ticket.isActive
         * so "active" keeps one definition (decision 0005).
         */
        private val ACTIVE_STATUS_NAMES: List<String> =
            TicketStatus.entries.filter { Ticket(currentStatus = it).isActive }.map { it.name }
    }
}
