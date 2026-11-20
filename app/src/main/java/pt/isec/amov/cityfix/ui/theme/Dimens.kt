package pt.isec.amov.cityfix.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing and sizes shared by the screens (D-016), so layouts use names instead of numbers. */
object Dimens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Side and top padding of a screen's content. */
    val screenPadding = 16.dp

    /** Bottom padding of a list under the report button: 16 + 52 + 16, rounded up. */
    val fabClearance = 88.dp

    /** Minimum touch target (Material, WCAG). */
    val minTouchTarget = 48.dp

    /** Leading thumbnail or icon of a card. */
    val thumbnail = 56.dp

    /** Maximum width of lists and detail content, so they do not stretch in landscape. */
    val contentMaxWidth = 600.dp

    /** Maximum width of forms. */
    val formMaxWidth = 480.dp

    /** Height of the report button. */
    val reportButtonHeight = 52.dp
}
