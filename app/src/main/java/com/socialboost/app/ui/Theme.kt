package com.socialboost.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

val Ink = Color(0xFF0C0C0A)
val Canvas = Color(0xFFEFEEEA)
val Paper = Color.White
val Signal = Color(0xFFF0501F)
val Muted = Color(0xFF6E6E66)
val Hairline = Color(0xFFE4E3DD)
val Cobalt = Color(0xFF2B3AD4)
val Sage = Color(0xFF6E7C58)
val Danger = Color(0xFFC0321D)

private val light = lightColorScheme(
    primary = Signal, onPrimary = Paper, secondary = Cobalt, onSecondary = Paper,
    background = Canvas, onBackground = Ink, surface = Paper, onSurface = Ink,
    outline = Hairline, error = Danger
)
private val dark = darkColorScheme(
    primary = Color(0xFFFF7549), onPrimary = Ink, secondary = Color(0xFF9CA8FF),
    background = Color(0xFF0C0C0A), onBackground = Color(0xFFF4F3EE),
    surface = Color(0xFF17170F), onSurface = Color(0xFFF4F3EE),
    outline = Color(0xFF41413A)
)
private val type = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun SocialBoostTheme(darkMode: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkMode) dark else light, typography = type, content = content)
}
