package pt.isec.amov.cityfix.model

import kotlin.math.pow
import kotlin.math.round

/** Decimal places kept in a location shown to anonymous users: 2 is about 1 km (brief I.1.C, coarse zoom). */
const val ANONYMOUS_COORDINATE_DECIMALS = 2

/**
 * Copy of this ticket that is safe for anonymous users (brief I.1.B, I.1.C; CLAUDE.md;
 * decisions 0006, 0007). It is also the content of the Firestore public document (F-011).
 *
 * Dropped: description (free text that may hold personal data), statusHistory (I.1.C shows
 * only media and map, and its first entry holds the creator's id), creatorId, followerIds,
 * messages and every Attachment.uploadedById. The location is rounded to
 * [ANONYMOUS_COORDINATE_DECIMALS].
 *
 * Kept: id, situation, scope, currentStatus (status badge), createdAt (ordering only, never
 * shown to anonymous users), categoryId, categoryName, and the attachments' id, type, file,
 * thumbnail and uploadState.
 *
 * The result is a view, not a valid ticket (it has no history entry): never store it as a full
 * ticket and never compute stats, lifecycle rules or 10 m grouping from it.
 *
 * Media files are passed through: photo GPS EXIF tags are removed in
 * LocalTicketsRepository.reportTicket (FileUtils.removeImageLocation, F-009) before the ticket
 * is stored; video metadata and Storage paths without user ids are for the media-upload task.
 *
 * Update this when Ticket, Attachment or StatusHistory gains a field.
 */
fun Ticket.toAnonymousView(): Ticket = copy(
    description = "",
    location = location.coarse(),
    creatorId = "",
    followerIds = emptyList(),
    messages = emptyList(),
    statusHistory = emptyList(),
    attachments = attachments.map { it.copy(uploadedById = "") },
)

/** Rounds both coordinates to [ANONYMOUS_COORDINATE_DECIMALS]. (0, 0) stays (0, 0). */
private fun Coordinates.coarse(): Coordinates {
    val scale = 10.0.pow(ANONYMOUS_COORDINATE_DECIMALS)
    return Coordinates(round(latitude * scale) / scale, round(longitude * scale) / scale)
}
