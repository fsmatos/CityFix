package pt.isec.amov.cityfix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pt.isec.amov.cityfix.R

val AppFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val base = Typography()

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
