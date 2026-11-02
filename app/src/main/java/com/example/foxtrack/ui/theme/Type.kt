package com.example.foxtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.foxtrack.R

val GeistFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

val GeistMonoFamily = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

val PixelFamily = FontFamily(
    Font(R.font.geist_pixel_square, FontWeight.Normal),
)

fun pixelFamilyFor(text: String): FontFamily =
    if (text.any { it in 'Ѐ'..'ӿ' }) GeistFamily else PixelFamily

private val defaults = Typography()

val FoxTypography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = GeistFamily),
    displayMedium = defaults.displayMedium.copy(fontFamily = GeistFamily),
    displaySmall = defaults.displaySmall.copy(fontFamily = GeistFamily),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = GeistFamily),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = GeistFamily),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = GeistFamily),
    titleLarge = defaults.titleLarge.copy(fontFamily = GeistFamily),
    titleMedium = defaults.titleMedium.copy(fontFamily = GeistFamily),
    titleSmall = defaults.titleSmall.copy(fontFamily = GeistFamily),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = GeistFamily),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = GeistFamily),
    bodySmall = defaults.bodySmall.copy(fontFamily = GeistFamily),
    labelLarge = defaults.labelLarge.copy(fontFamily = GeistFamily),
    labelMedium = defaults.labelMedium.copy(fontFamily = GeistFamily),
    labelSmall = defaults.labelSmall.copy(fontFamily = GeistFamily),
)
