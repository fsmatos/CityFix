package pt.isec.amov.cityfix.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import pt.isec.amov.cityfix.model.TicketStatus

@Immutable
data class StatusColors(
    val open: Color,
    val infoRequested: Color,
    val inProgress: Color,
    val resolved: Color,
    val closed: Color,
) {
    fun of(status: TicketStatus): Color = when (status) {
        TicketStatus.OPEN -> open
        TicketStatus.INFO_REQUESTED -> infoRequested
        TicketStatus.IN_PROGRESS -> inProgress
        TicketStatus.RESOLVED -> resolved
        TicketStatus.CLOSED -> closed
    }
}

val LightStatusColors = StatusColors(
    open = Color(0xFFB8862A),
    infoRequested = Color(0xFFC0604A),
    inProgress = Color(0xFF387F5C),
    resolved = Color(0xFF3F78B5),
    closed = Color(0xFF8A8A86),
)

val DarkStatusColors = StatusColors(
    open = Color(0xFFD9A945),
    infoRequested = Color(0xFFD9785F),
    inProgress = Color(0xFF5DB58A),
    resolved = Color(0xFF6FA8DC),
    closed = Color(0xFF8E8F93),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
