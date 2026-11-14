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
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusHistory
import pt.isec.amov.cityfix.model.Ticket
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.TicketsList
import pt.isec.amov.cityfix.model.UploadState
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

/** Collection and document paths (decision 0010). */
internal const val TICKETS = "tickets"
internal const val PUBLIC_TICKETS = "publicTickets"
internal const val TICKET_LOCATIONS = "ticketLocations"

/** Subcollection of a ticket: one report per citizen who joined it by grouping. */
internal const val REPORTS = "reports"
internal const val CATEGORIES = "categories"
internal const val STATS_SUMMARY = "stats/summary"

/* ---------- model -> document ---------- */

/**
 * The full `tickets/{id}` document (decision 0010). The id is the document id, not a field.
 * `participantIds` = creator + followers, so one array-contains query lists "my tickets".
 * Not written here: `resolvedAt` (server time, D-013) and `messages` (subcollection, F-014).
 */
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

/**
 * The `publicTickets/{id}` document, the only one anonymous users read (decisions 0006, 0010).
 * This is the only builder of a public document: it calls toAnonymousView() first and copies
 * just the 7 allowed keys from the projected view, so no author id, description or exact
 * location can reach it. `thumbnails` = REPORT-phase thumbnails (empty until F-013 uploads them).
 */
internal fun Ticket.toPublicTicketDocument(): Map<String, Any?> {
    val view = toAnonymousView()
    return hashMapOf(
        "categoryId" to view.categoryId,
        "categoryName" to view.categoryName,
        "situation" to view.situation.name,
        "currentStatus" to view.currentStatus.name,
        "coarseLocation" to view.location.toGeoPoint(),
        "thumbnails" to view.attachments
            .filter { it.phase == AttachmentPhase.REPORT }
            .mapNotNull { it.thumbnail },
        "createdAt" to view.createdAt,
    )
}

/** The `ticketLocations/{id}` document for the 10 m grouping (decision 0010, F-012). */
internal fun Ticket.toTicketLocationDocument(): Map<String, Any?> = hashMapOf(
    "categoryId" to categoryId,
    "situation" to situation.name,
    "location" to location.toGeoPoint(),
    // Same rule as findNearby (decision 0005): RESOLVED and CLOSED tickets do not group (amends
    // decision 0010). D-013/F-014 must update it when a ticket is resolved or reopened.
    "active" to isActive,
)

/**
 * The `tickets/{id}/reports/{uid}` document of a citizen who joined the ticket (decision 0010).
 * Their attachments stay here, never on the shared ticket, so a PRIVATE reporter's photos never
 * reach a PUBLIC ticket (F-012 Q5).
 */
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

/** The `categories/{id}` document (debug seed only; operators manage categories in D-012). */
internal fun Category.toCategoryDocument(): Map<String, Any?> = hashMapOf(
    "name" to name,
    "active" to active,
)

/** One `statusHistory` entry; `justification` only when there is one. */
private fun StatusHistory.toDocument(): Map<String, Any> {
    val map = hashMapOf<String, Any>(
        "status" to status.name,
        "dateTime" to dateTime,
        "responsibleId" to responsibleId,
    )
    if (justification != null) map["justification"] = justification
    return map
}

/** One `attachments` entry; `storagePath` is Attachment.file, `thumbnailPath` only when set. */
private fun Attachment.toDocument(): Map<String, Any> {
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

/** A full ticket from `tickets/{id}`. Firestore-only fields (participantIds, resolvedAt) are dropped. */
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
    attachments = getMapList("attachments").map { it.toAttachment() },
)

/**
 * A ticket from `publicTickets/{id}`, in the same shape toAnonymousView() produces: scope PUBLIC,
 * coarse location, no author, description or history. Each thumbnail becomes an attachment so
 * the card can show it.
 */
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

/**
 * A `ticketLocations/{id}` document as a minimal ticket (id, category, situation, location, OPEN
 * when active), so TicketsList.findNearby can be reused. Not a real ticket: never store or show it.
 */
internal fun DocumentSnapshot.toTicketLocation(): Ticket = Ticket(
    id = id,
    categoryId = getString("categoryId") ?: "",
    situation = enumOrDefault(getString("situation"), Situation.DAMAGE),
    location = getGeoPoint("location").toCoordinates(),
    currentStatus = if (getBoolean("active") == true) TicketStatus.OPEN else TicketStatus.CLOSED,
)

internal fun DocumentSnapshot.toCategory(): Category = Category(
    id = id,
    name = getString("name") ?: "",
    active = getBoolean("active") ?: true,
)

/** Dashboard figures from `stats/summary`; a missing document means no tickets yet. */
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

/** One `statusHistory` entry; `dateTime` is a Long (decision 0003). */
private fun Map<*, *>.toStatusHistory() = StatusHistory(
    status = enumOrDefault(this["status"] as? String, TicketStatus.OPEN),
    dateTime = (this["dateTime"] as? Number)?.toLong() ?: 0L,
    justification = this["justification"] as? String,
    responsibleId = this["responsibleId"] as? String ?: "",
)

/** One `attachments` entry. A missing `phase` reads as REPORT (decision 0011). */
private fun Map<*, *>.toAttachment() = Attachment(
    id = this["id"] as? String ?: "",
    type = enumOrDefault(this["type"] as? String, AttachmentType.IMAGE),
    file = this["storagePath"] as? String ?: "",
    thumbnail = this["thumbnailPath"] as? String,
    uploadState = enumOrDefault(this["uploadState"] as? String, UploadState.PENDING),
    uploadedById = this["uploadedById"] as? String ?: "",
    phase = enumOrDefault(this["phase"] as? String, AttachmentPhase.REPORT),
)

/** A Firestore array of strings (no typed getter exists for lists). */
private fun DocumentSnapshot.getStringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

/** A Firestore array of maps (statusHistory, attachments); anything else is skipped. */
private fun DocumentSnapshot.getMapList(field: String): List<Map<*, *>> =
    (get(field) as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

private fun GeoPoint?.toCoordinates(): Coordinates =
    if (this == null) Coordinates() else Coordinates(latitude, longitude)

/** The enum constant called [name], or [default] when the name is missing or unknown. */
private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/* ---------- dashboard counters ---------- */

/**
 * The change one ticket write makes to `stats/summary` (F-011 Q3): stats(after) - stats(before)
 * for each counter, as FieldValue.increment. [before] is null for a new ticket.
 *
 * It reuses TicketsList.stats() on a one-ticket list, so the counters keep decision 0005's
 * meaning (active, resolved and repair time) without a second copy of the rules. For one ticket
 * the average repair time is that ticket's repair time, so it adds to the sum directly.
 *
 * FieldValue.increment (beyond class) asks Firestore to add the number to the stored value, so
 * two devices writing at once do not overwrite each other, and it works offline (the write is
 * queued). Every batch that changes a ticket must add this map with SetOptions.merge()
 * (handover to D-013 and F-014). Both tickets need a non-blank id (TicketsList ignores blank ids).
 */
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
