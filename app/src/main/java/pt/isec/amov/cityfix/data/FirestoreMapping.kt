package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import pt.isec.amov.cityfix.model.Attachment
import pt.isec.amov.cityfix.model.AttachmentPhase
import pt.isec.amov.cityfix.model.AttachmentType
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Coordinates
import pt.isec.amov.cityfix.model.DashboardStats
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Operator
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.UploadState
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.isActive
import pt.isec.amov.cityfix.model.toAnonymousView

/*
 * Every conversion between the model and Firestore documents (decision 0010). Written by hand,
 * like the class demo (T8 AMOVStorageDemo: hashMapOf to write, doc.getString to read), because
 * the Firestore field names differ from the model (storagePath = Attachment.file, GeoPoint =
 * Coordinates) and some fields exist only in Firestore (participantIds, resolvedAt).
 *
 * Reading uses the document's typed getters, as in the class demo (`doc.getString("name") ?: ""`),
 * with the model default when a field is missing. Lists and the maps inside the
 * statusHistory/attachments arrays have no typed getters, so those few values are cast.
 */

internal const val TICKETS = "tickets"
internal const val PUBLIC_TICKETS = "publicTickets"
internal const val TICKET_LOCATIONS = "ticketLocations"

internal const val REPORTS = "reports"

internal const val MESSAGES = "messages"
internal const val CATEGORIES = "categories"
internal const val STATS_SUMMARY = "stats/summary"

/* ---------- model -> document ---------- */

internal fun Ticket.toTicketDocument(): Map<String, Any?> = hashMapOf(
    "description" to description,
    "situation" to situation.name,
    "scope" to scope.name,
    "location" to location.toGeoPoint(),
    "currentStatus" to currentStatus.name,
    "createdAt" to createdAt,
    "categoryId" to categoryId,
    "categoryName" to categoryName,
    "creatorId" to creatorId,
    "followerIds" to followerIds,
    "participantIds" to (listOf(creatorId) + followerIds).distinct(),
    "statusHistory" to statusHistory.map { it.toDocument() },
    "attachments" to attachments.map { it.toDocument() },
)

internal fun Ticket.toPublicTicketDocument(): Map<String, Any?> {
    val view = toAnonymousView()
    return hashMapOf(
        "categoryId" to view.categoryId,
        "categoryName" to view.categoryName,
        "situation" to view.situation.name,
        "currentStatus" to view.currentStatus.name,
        "coarseLocation" to view.location.toGeoPoint(),
        "thumbnails" to view.attachments
            .filter { it.phase == AttachmentPhase.REPORT && it.uploadState == UploadState.UPLOADED }
            .mapNotNull { it.thumbnail },
        "createdAt" to view.createdAt,
    )
}

internal fun Ticket.toTicketLocationDocument(): Map<String, Any?> = hashMapOf(
    "categoryId" to categoryId,
    "situation" to situation.name,
    "location" to location.toGeoPoint(),
    // Same rule as findNearby (decision 0005): RESOLVED and CLOSED tickets do not group (amends
    // decision 0010). D-013/F-014 must update it when a ticket is resolved or reopened.
    "active" to isActive,
)

internal fun reportDocument(
    categoryId: String,
    situation: Situation,
    location: Coordinates,
    description: String,
    createdAt: Long,
    attachments: List<Attachment>,
): Map<String, Any?> = hashMapOf(
    "categoryId" to categoryId,
    "situation" to situation.name,
    "location" to location.toGeoPoint(),
    "description" to description,
    "createdAt" to createdAt,
    "attachments" to attachments.map { it.toDocument() },
)

internal fun Ticket.toStatusFields(): Map<String, Any> = hashMapOf(
    "currentStatus" to currentStatus.name,
    "statusHistory" to statusHistory.map { it.toDocument() },
)

internal fun messageDocument(text: String, sentAt: Long, author: User): Map<String, Any> = hashMapOf(
    "text" to text,
    "sentAt" to sentAt,
    "authorId" to author.id,
    "byOperator" to (author is Operator),
)

internal fun lastMessageFields(message: Map<String, Any>): Map<String, Any> = hashMapOf(
    "lastMessageAt" to message.getValue("sentAt"),
    "lastMessageAuthorId" to message.getValue("authorId"),
)

internal fun Category.toCategoryDocument(): Map<String, Any?> = hashMapOf(
    "name" to name,
    "active" to active,
)

private fun StatusHistory.toDocument(): Map<String, Any> {
    val map = hashMapOf<String, Any>(
        "status" to status.name,
        "dateTime" to dateTime,
        "responsibleId" to responsibleId,
    )
    if (justification != null) map["justification"] = justification
    return map
}

internal fun Attachment.toDocument(): Map<String, Any> {
    val map = hashMapOf<String, Any>(
        "id" to id,
        "type" to type.name,
        "phase" to phase.name,
        "storagePath" to file,
        "uploadState" to uploadState.name,
        "uploadedById" to uploadedById,
    )
    if (thumbnail != null) map["thumbnailPath"] = thumbnail
    return map
}

private fun Coordinates.toGeoPoint() = GeoPoint(latitude, longitude)

/* ---------- document -> model ---------- */

internal fun DocumentSnapshot.toTicket(): Ticket = Ticket(
    id = id,
    description = getString("description") ?: "",
    situation = enumOrDefault(getString("situation"), Situation.DAMAGE),
    scope = enumOrDefault(getString("scope"), Scope.PRIVATE),
    location = getGeoPoint("location").toCoordinates(),
    currentStatus = enumOrDefault(getString("currentStatus"), TicketStatus.OPEN),
    createdAt = getLong("createdAt") ?: 0L,
    categoryId = getString("categoryId") ?: "",
    categoryName = getString("categoryName") ?: "",
    creatorId = getString("creatorId") ?: "",
    followerIds = getStringList("followerIds"),
    statusHistory = getMapList("statusHistory").map { it.toStatusHistory() },
    attachments = attachments(),
    lastOperatorNearbyAt = getLong("lastOperatorNearbyAt"),
    lastMessageAt = getLong("lastMessageAt"),
    lastMessageAuthorId = getString("lastMessageAuthorId"),
)

internal fun DocumentSnapshot.toPublicTicket(): Ticket {
    val thumbnails = getStringList("thumbnails")
    return Ticket(
        id = id,
        situation = enumOrDefault(getString("situation"), Situation.DAMAGE),
        scope = Scope.PUBLIC,
        location = getGeoPoint("coarseLocation").toCoordinates(),
        currentStatus = enumOrDefault(getString("currentStatus"), TicketStatus.OPEN),
        createdAt = getLong("createdAt") ?: 0L,
        categoryId = getString("categoryId") ?: "",
        categoryName = getString("categoryName") ?: "",
        attachments = thumbnails.mapIndexed { index, path ->
            Attachment(
                id = "thumb-$index",
                file = path,
                thumbnail = path,
                uploadState = UploadState.UPLOADED,
            )
        },
    )
}

internal fun DocumentSnapshot.toTicketLocation(): Ticket = Ticket(
    id = id,
    categoryId = getString("categoryId") ?: "",
    situation = enumOrDefault(getString("situation"), Situation.DAMAGE),
    location = getGeoPoint("location").toCoordinates(),
    currentStatus = if (getBoolean("active") == true) TicketStatus.OPEN else TicketStatus.CLOSED,
)

internal fun DocumentSnapshot.toMessage(): Message = Message(
    id = id,
    text = getString("text") ?: "",
    sentAt = getLong("sentAt") ?: 0L,
    authorId = getString("authorId") ?: "",
    byOperator = getBoolean("byOperator") ?: false,
)

internal fun DocumentSnapshot.toCategory(): Category = Category(
    id = id,
    name = getString("name") ?: "",
    active = getBoolean("active") ?: true,
)

internal fun DocumentSnapshot.toDashboardStats(): DashboardStats {
    if (!exists()) return DashboardStats()
    val resolved = getLong("resolvedTickets")?.toInt() ?: 0
    val repairSum = getLong("repairTimeSumMillis") ?: 0L
    return DashboardStats(
        totalTickets = getLong("totalTickets")?.toInt() ?: 0,
        activeTickets = getLong("activeTickets")?.toInt() ?: 0,
        resolvedTickets = resolved,
        averageRepairTimeMillis = if (resolved > 0) repairSum / resolved else null,
    )
}

internal fun DocumentSnapshot.attachments(): List<Attachment> =
    getMapList("attachments").map { it.toAttachment() }

internal fun DocumentSnapshot.attachmentsWith(change: (Attachment) -> Attachment): List<Map<String, Any>> =
    attachments().map { change(it).toDocument() }

private fun Map<*, *>.toStatusHistory() = StatusHistory(
    status = enumOrDefault(this["status"] as? String, TicketStatus.OPEN),
    dateTime = (this["dateTime"] as? Number)?.toLong() ?: 0L,
    justification = this["justification"] as? String,
    responsibleId = this["responsibleId"] as? String ?: "",
)

private fun Map<*, *>.toAttachment() = Attachment(
    id = this["id"] as? String ?: "",
    type = enumOrDefault(this["type"] as? String, AttachmentType.IMAGE),
    file = this["storagePath"] as? String ?: "",
    thumbnail = this["thumbnailPath"] as? String,
    uploadState = enumOrDefault(this["uploadState"] as? String, UploadState.PENDING),
    uploadedById = this["uploadedById"] as? String ?: "",
    phase = enumOrDefault(this["phase"] as? String, AttachmentPhase.REPORT),
)

private fun DocumentSnapshot.getStringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

private fun DocumentSnapshot.getMapList(field: String): List<Map<*, *>> =
    (get(field) as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

private fun GeoPoint?.toCoordinates(): Coordinates =
    if (this == null) Coordinates() else Coordinates(latitude, longitude)

private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/* ---------- dashboard counters ---------- */

internal fun statsIncrements(before: Ticket?, after: Ticket): Map<String, Any> {
    val afterStats = TicketsList(listOf(after)).stats()
    val beforeStats = if (before == null) DashboardStats() else TicketsList(listOf(before)).stats()
    val repairDelta =
        (afterStats.averageRepairTimeMillis ?: 0L) - (beforeStats.averageRepairTimeMillis ?: 0L)
    return hashMapOf(
        "totalTickets" to FieldValue.increment((afterStats.totalTickets - beforeStats.totalTickets).toLong()),
        "activeTickets" to FieldValue.increment((afterStats.activeTickets - beforeStats.activeTickets).toLong()),
        "resolvedTickets" to FieldValue.increment((afterStats.resolvedTickets - beforeStats.resolvedTickets).toLong()),
        "repairTimeSumMillis" to FieldValue.increment(repairDelta),
    )
}
