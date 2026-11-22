package pt.isec.amov.cityfix.data

import kotlinx.coroutines.flow.Flow
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.User

enum class ReportOutcome { CREATED, JOINED, ALREADY_FOLLOWING }

interface TicketsRepository {

    fun publicTickets(): Flow<List<Ticket>>

    fun participantTickets(userId: String): Flow<List<Ticket>>

    fun allTickets(): Flow<List<Ticket>>

    suspend fun participantTicketsOnce(userId: String): List<Ticket>

    suspend fun allTicketsOnce(): List<Ticket>

    fun ticketsFromServer(userId: String, isOperator: Boolean): Flow<List<Ticket>>

    fun stats(): Flow<DashboardStats>

    fun activeCategories(): Flow<List<Category>>

    suspend fun reportTicket(
        description: String,
        categoryId: String,
        categoryName: String,
        situation: Situation,
        scope: Scope,
        attachments: List<Attachment>,
        location: Coordinates,
        creatorId: String,
    ): ReportOutcome?

    fun messagesFor(ticketId: String): Flow<List<Message>>

    fun sendMessage(ticketId: String, text: String, author: User): Boolean

    fun reopenTicket(ticket: Ticket, by: User, justification: String): Boolean

    fun closeExpiredTicket(ticket: Ticket)

    fun askForClarification(ticket: Ticket, by: User, question: String): Boolean

    fun startWork(ticket: Ticket, by: User, note: String? = null): Boolean

    suspend fun resolveTicket(ticket: Ticket, by: User, proof: List<Attachment>, note: String? = null): Boolean

    fun recordOperatorNearby(ticket: Ticket, now: Long): Boolean
}
