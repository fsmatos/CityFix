package pt.isec.amov.cityfix.model

import kotlin.math.pow
import kotlin.math.round

/** Decimal places kept in a location shown to anonymous users: 2 is about 1 km (brief I.1.C, coarse zoom). */
const val ANONYMOUS_COORDINATE_DECIMALS = 2

/**
 * Copy of this ticket that is safe for anonymous users (CLAUDE.md, decision 0003).
 * Blanks creatorId, followerIds, messages, every StatusHistory.responsibleId (the first OPEN
 * entry holds the creator's id), every StatusHistory.justification (free text that may name
 * the author) and every Attachment.uploadedById, and rounds the location to
 * [ANONYMOUS_COORDINATE_DECIMALS]. Update this when Ticket gains a field with personal data.
 * Media files are passed through: EXIF location must be stripped at compression (utils/) and
 * Storage paths must not contain user ids (media-upload task).
 */
fun Ticket.toAnonymousView(): Ticket = copy(
    location = location.coarse(),
    creatorId = "",
    followerIds = emptyList(),
    messages = emptyList(),
    statusHistory = statusHistory.map { it.copy(responsibleId = "", justification = null) },
    attachments = attachments.map { it.copy(uploadedById = "") },
)

/** Rounds both coordinates to [ANONYMOUS_COORDINATE_DECIMALS]. (0, 0) stays (0, 0). */
private fun Coordinates.coarse(): Coordinates {
    val scale = 10.0.pow(ANONYMOUS_COORDINATE_DECIMALS)
    return Coordinates(round(latitude * scale) / scale, round(longitude * scale) / scale)
}
