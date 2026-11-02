package com.example.foxtrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
class FoxColors(
    val isDark: Boolean,
    val surfaceSunken: Color,
    val field: Color,
    val border: Color,
    val borderStrong: Color,
    val accentInk: Color,
    val focus: Color,
    val wish: Color,
    val wishBg: Color,
    val play: Color,
    val playBg: Color,
    val done: Color,
    val doneBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val skeleton: Color,
    val success: Color,
)

private val LightFoxColors = FoxColors(
    isDark = false,
    surfaceSunken = Color(0xFFF2E3C9),
    field = Color(0xFFFFFDF8),
    border = Color(0xFFEAD9B8),
    borderStrong = Color(0xFF857250),
    accentInk = Color(0xFFB33B09),
    focus = Color(0xFF1D1F4B),
    wish = Color(0xFF2A2C6B),
    wishBg = Color(0xFFE5E5F5),
    play = Color(0xFF8A3006),
    playBg = Color(0xFFFFE0CC),
    done = Color(0xFF155C39),
    doneBg = Color(0xFFD8F0E2),
    danger = Color(0xFFB42318),
    dangerBg = Color(0xFFFDE8E4),
    skeleton = Color(0xFFECDCBB),
    success = Color(0xFF155C39),
)

private val DarkFoxColors = FoxColors(
    isDark = true,
    surfaceSunken = Color(0xFF101132),
    field = Color(0xFF171843),
    border = Color(0xFF34366E),
    borderStrong = Color(0xFF7477BA),
    accentInk = Color(0xFFFF8E58),
    focus = Color(0xFFF9E7CD),
    wish = Color(0xFFDCDCFF),
    wishBg = Color(0xFF2B2E72),
    play = Color(0xFFFFD9C2),
    playBg = Color(0xFF6A2F14),
    done = Color(0xFFC4F0D8),
    doneBg = Color(0xFF1C4A38),
    danger = Color(0xFFFF8F80),
    dangerBg = Color(0xFF4A1D2B),
    skeleton = Color(0xFF262A5E),
    success = Color(0xFFC4F0D8),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFF05C20),
    onPrimary = Color(0xFF1A0F08),
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF8A3006),
    secondary = Color(0xFF1D1F4B),
    onSecondary = Color(0xFFFBF1E1),
    secondaryContainer = Color(0xFFE5E5F5),
    onSecondaryContainer = Color(0xFF2A2C6B),
    tertiary = Color(0xFF155C39),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF1E1),
    onBackground = Color(0xFF1D1F4B),
    surface = Color(0xFFFFFAF1),
    onSurface = Color(0xFF1D1F4B),
    surfaceVariant = Color(0xFFF2E3C9),
    onSurfaceVariant = Color(0xFF525483),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1D1F4B),
    inverseOnSurface = Color(0xFFFBF1E1),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE8E4),
    onErrorContainer = Color(0xFFB42318),
    outline = Color(0xFF857250),
    outlineVariant = Color(0xFFEAD9B8),
    scrim = Color(0xFF1C190F),
    surfaceContainerLowest = Color(0xFFFFFDF8),
    surfaceContainerLow = Color(0xFFFFFAF1),
    surfaceContainer = Color(0xFFFFFAF1),
    surfaceContainerHigh = Color(0xFFFFFAF1),
    surfaceContainerHighest = Color(0xFFF2E3C9),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF05C20),
    onPrimary = Color(0xFF1A0F08),
    primaryContainer = Color(0xFF6A2F14),
    onPrimaryContainer = Color(0xFFFFD9C2),
    secondary = Color(0xFFF9E7CD),
    onSecondary = Color(0xFF14153A),
    secondaryContainer = Color(0xFF2B2E72),
    onSecondaryContainer = Color(0xFFDCDCFF),
    tertiary = Color(0xFFC4F0D8),
    onTertiary = Color(0xFF1C4A38),
    background = Color(0xFF14153A),
    onBackground = Color(0xFFF9E7CD),
    surface = Color(0xFF1D1F4B),
    onSurface = Color(0xFFF9E7CD),
    surfaceVariant = Color(0xFF101132),
    onSurfaceVariant = Color(0xFFBCB9D6),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF9E7CD),
    inverseOnSurface = Color(0xFF14153A),
    error = Color(0xFFFF8F80),
    onError = Color(0xFF1A0F08),
    errorContainer = Color(0xFF4A1D2B),
    onErrorContainer = Color(0xFFFF8F80),
    outline = Color(0xFF7477BA),
    outlineVariant = Color(0xFF34366E),
    scrim = Color(0xFF060614),
    surfaceContainerLowest = Color(0xFF171843),
    surfaceContainerLow = Color(0xFF1D1F4B),
    surfaceContainer = Color(0xFF1D1F4B),
    surfaceContainerHigh = Color(0xFF1D1F4B),
    surfaceContainerHighest = Color(0xFF101132),
)

private val FoxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val LocalFoxColors = staticCompositionLocalOf { LightFoxColors }

@Composable
fun FoxTrackTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFoxColors provides if (darkTheme) DarkFoxColors else LightFoxColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = FoxTypography,
            shapes = FoxShapes,
            content = content,
        )
    }
}

object FoxTheme {
    val colors: FoxColors
        @Composable get() = LocalFoxColors.current
}
