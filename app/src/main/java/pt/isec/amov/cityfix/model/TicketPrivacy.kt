package pt.isec.amov.cityfix.model

import kotlin.math.pow
import kotlin.math.round

const val ANONYMOUS_COORDINATE_DECIMALS = 2

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

private fun Coordinates.coarse(): Coordinates {
    val scale = 10.0.pow(ANONYMOUS_COORDINATE_DECIMALS)
    return Coordinates(round(latitude * scale) / scale, round(longitude * scale) / scale)
}
