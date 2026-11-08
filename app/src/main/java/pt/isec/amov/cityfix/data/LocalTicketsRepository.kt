package pt.isec.amov.cityfix.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.model.toAnonymousView
import java.util.UUID

/**
 * Phase-1 repository (no Firebase), like ContactsRepository in Contacts_#3 but without a JSON
 * file: the cache is a TicketsList seeded with [SampleTickets], so a new process starts again
 * from the samples. Create one per app (CityFixApplication).
 * The suspend functions run on Dispatchers.IO behind a Mutex: the IO pool can run a load and a
 * report at the same time, and TicketsList is not thread-safe.
 */
class LocalTicketsRepository(
    initialTickets: List<Ticket> = SampleTickets.all,
    private val categories: List<Category> = SampleCategories.all,
) : TicketsRepository {

    private val ticketsList = TicketsList(initialTickets)
    private val mutex = Mutex()

    override suspend fun loadPublicTickets(): List<Ticket> = withContext(Dispatchers.IO) {
        mutex.withLock { visibleTickets() }
    }

    override suspend fun loadStats(): DashboardStats = withContext(Dispatchers.IO) {
        mutex.withLock { ticketsList.stats() }
    }

    /**
     * Suspend and behind the Mutex like the loads (unlike getContactById in Contacts_#3):
     * publicActive() iterates the cache, which a report may be adding to on another thread.
     */
    override suspend fun getTicketById(id: String): Ticket? = withContext(Dispatchers.IO) {
        mutex.withLock { ticketsList.publicActive().firstOrNull { it.id == id }?.toAnonymousView() }
    }

    /** No location yet, so no 10 m grouping. Attachments are stored as PENDING. */
    override suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
    ): String? = withContext(Dispatchers.IO) {
        val text = description.trim()
        val cleanCategoryId = categoryId.trim()
        val cleanCategoryName = categoryName.trim()
        if (text.isBlank() || cleanCategoryId.isBlank() || cleanCategoryName.isBlank() ||
            attachments.isEmpty()
        ) {
            return@withContext null
        }

        val now = System.currentTimeMillis()
        val ticket = Ticket(
            id = UUID.randomUUID().toString(),
            description = text,
            situation = situation,
            scope = scope,
            currentStatus = TicketStatus.OPEN,
            createdAt = now,
            categoryId = cleanCategoryId,
            categoryName = cleanCategoryName,
            creatorId = LOCAL_USER_ID,
            statusHistory = listOf(StatusHistory(TicketStatus.OPEN, now, null, LOCAL_USER_ID)),
            attachments = attachments.map {
                it.copy(uploadState = UploadState.PENDING, uploadedById = LOCAL_USER_ID)
            },
        )

        val added = mutex.withLock { ticketsList.add(ticket) }
        if (added) ticket.id else null
    }

    override fun activeCategories(): List<Category> = categories.filter { it.active }

    private fun visibleTickets(): List<Ticket> =
        ticketsList.publicActive().map { it.toAnonymousView() }

    companion object {
        /** Phase 1 has no login; stands in for the reporter's user id. */
        const val LOCAL_USER_ID = "local-citizen"
    }
}
