package pt.isec.amov.cityfix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The app's one font family (D-016). The plan is Inter 400/500/600 from res/font; until those
 * files are added this is the system font. Changing it here changes every text in the app,
 * because the screens only use MaterialTheme.typography.
 */
val AppFontFamily: FontFamily = FontFamily.Default

private val base = Typography()

/**
 * Material 3 type scale on [AppFontFamily], three weights only: 600 for display and headlines,
 * 500 for titles and labels, 400 for body text.
 */
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium),
)
