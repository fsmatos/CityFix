package pt.isec.amov.cityfix.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import pt.isec.amov.cityfix.model.TicketStatus

/**
 * The colours of the small status dots (D-016). The status is always written next to the dot in
 * neutral text, so the colour only supports it. Soft tones, one set per theme; CityFixTheme
 * provides the right one through [LocalStatusColors].
 */
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

/** Ochre, terracotta, green, blue and grey, dark enough for the white card. */
val LightStatusColors = StatusColors(
    open = Color(0xFFB8862A),
    infoRequested = Color(0xFFC0604A),
    inProgress = Color(0xFF387F5C),
    resolved = Color(0xFF3F78B5),
    closed = Color(0xFF8A8A86),
)

/** The same hues, lighter for the dark card. */
val DarkStatusColors = StatusColors(
    open = Color(0xFFD9A945),
    infoRequested = Color(0xFFD9785F),
    inProgress = Color(0xFF5DB58A),
    resolved = Color(0xFF6FA8DC),
    closed = Color(0xFF8E8F93),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
