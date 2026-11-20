package pt.isec.amov.cityfix.model

import kotlin.math.pow
import kotlin.math.round

/** Decimal places kept in a location shown to anonymous users: 2 is about 1 km (brief I.1.C, coarse zoom). */
const val ANONYMOUS_COORDINATE_DECIMALS = 2

/**
 * Copy of this ticket that is safe for anonymous users (brief I.1.B, I.1.C; CLAUDE.md;
 * decisions 0006, 0007). It is also the content of the Firestore public document, built by
 * toPublicTicketDocument (F-011).
 *
 * Dropped: description (free text that may hold personal data), statusHistory (I.1.C shows
 * only media and map, and its first entry holds the creator's id), creatorId, followerIds,
 * messages, lastOperatorNearbyAt (D-014), lastMessageAt and lastMessageAuthorId (F-016) and
 * every Attachment.uploadedById. The location is rounded to [ANONYMOUS_COORDINATE_DECIMALS].
 *
 * Kept: id, situation, scope, currentStatus (status badge), createdAt (ordering only, never
 * shown to anonymous users), categoryId, categoryName, and the attachments' id, type, file,
 * thumbnail, uploadState and phase.
 *
 * The result is a view, not a valid ticket (it has no history entry): never store it as a full
 * ticket and never compute stats, lifecycle rules or 10 m grouping from it.
 *
 * Media files are passed through: photos are re-encoded in AttachmentUploader.prepare (F-013),
 * which drops all EXIF; videos are uploaded as recorded and may keep their recording metadata
 * (known limit; the brief does not require removing it). Storage paths hold only the ticket id
 * and a UUID (decision 0010).
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
    lastOperatorNearbyAt = null,
    lastMessageAt = null,
    lastMessageAuthorId = null,
    attachments = attachments.map { it.copy(uploadedById = "") },
)

/** Rounds both coordinates to [ANONYMOUS_COORDINATE_DECIMALS]. (0, 0) stays (0, 0). */
private fun Coordinates.coarse(): Coordinates {
    val scale = 10.0.pow(ANONYMOUS_COORDINATE_DECIMALS)
    return Coordinates(round(latitude * scale) / scale, round(longitude * scale) / scale)
}
