package pt.isec.amov.cityfix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// D-016: neutral schemes with one accent. `background` is the page and `surface` the cards, the
// island and the fields. `primaryContainer` is the indigo report button in both themes (the
// ExtendedFloatingActionButton's default colour). `secondary`/`tertiary` are neutral, so Material
// components that use them stay grey. surfaceTint = surface: no tonal tint on elevated surfaces.
private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Indigo,
    onPrimaryContainer = Color.White,
    inversePrimary = IndigoLight,
    secondary = LightTextMuted,
    onSecondary = Color.White,
    secondaryContainer = LightContainerHigh,
    onSecondaryContainer = LightText,
    tertiary = LightTextMuted,
    onTertiary = Color.White,
    tertiaryContainer = LightContainerHigh,
    onTertiaryContainer = LightText,
    background = LightPage,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightThumb,
    onSurfaceVariant = LightTextMuted,
    surfaceTint = LightCard,
    inverseSurface = Color(0xFF2B2B2A),
    inverseOnSurface = Color(0xFFF2F2EF),
    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightLine,
    surfaceBright = LightCard,
    surfaceDim = LightContainerHighest,
    surfaceContainerLowest = LightCard,
    surfaceContainerLow = LightContainerLow,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = IndigoDeep,
    primaryContainer = Indigo,
    onPrimaryContainer = Color.White,
    inversePrimary = Indigo,
    secondary = DarkTextMuted,
    onSecondary = DarkPage,
    secondaryContainer = DarkContainerHigh,
    onSecondaryContainer = DarkText,
    tertiary = DarkTextMuted,
    onTertiary = DarkPage,
    tertiaryContainer = DarkContainerHigh,
    onTertiaryContainer = DarkText,
    background = DarkPage,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkThumb,
    onSurfaceVariant = DarkTextMuted,
    surfaceTint = DarkCard,
    inverseSurface = DarkText,
    inverseOnSurface = Color(0xFF2B2B2A),
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkLine,
    surfaceBright = DarkContainerHighest,
    surfaceDim = DarkPage,
    surfaceContainerLowest = DarkContainerLowest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
)

@Composable
fun CityFixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

object CityFixTheme {
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
