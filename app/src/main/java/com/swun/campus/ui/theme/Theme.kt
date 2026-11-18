package com.swun.campus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BrandBlue = Color(0xFF0C5B8C)
private val BrandBlueDark = Color(0xFF8FC8EC)
private val BrandRed = Color(0xFFA62A21)

private val LightScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E9F5),
    onPrimaryContainer = Color(0xFF00263C),
    secondary = BrandRed,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8DCD8),
    onSecondaryContainer = Color(0xFF410E09),
    tertiary = Color(0xFF4A6072),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E7F3),
    onTertiaryContainer = Color(0xFF0A2233),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7ECF0),
    onSurfaceVariant = Color(0xFF5B6872),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F7),
    surfaceContainer = Color(0xFFEDF0F4),
    surfaceContainerHigh = Color(0xFFE6EBF0),
    outline = Color(0xFFAEBDC7),
    outlineVariant = Color(0xFFDBE2E8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE2DF),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C73),
    onPrimaryContainer = Color(0xFFCFE6F6),
    secondary = Color(0xFFF2B8AF),
    onSecondary = Color(0xFF5F150E),
    secondaryContainer = Color(0xFF7E231A),
    onSecondaryContainer = Color(0xFFFBDAD5),
    tertiary = Color(0xFFB4CBDD),
    onTertiary = Color(0xFF1F3344),
    tertiaryContainer = Color(0xFF354A5B),
    onTertiaryContainer = Color(0xFFD3E5F5),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF151A1D),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF26313A),
    onSurfaceVariant = Color(0xFFB9C8D3),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171D20),
    surfaceContainer = Color(0xFF1B2124),
    surfaceContainerHigh = Color(0xFF252B2F),
    outline = Color(0xFF55636D),
    outlineVariant = Color(0xFF334047),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF3B0A08),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun SWUNCampusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
