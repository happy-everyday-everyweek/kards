package com.kards.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * MD3 配色。Android 12 及以上跟随系统取色（动态色），
 * 低版本回退到一套偏军事题材的低饱和配色。
 */
private val FallbackLight = lightColorScheme(
    primary = Color(0xFF4A5D3F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBDFB9),
    onPrimaryContainer = Color(0xFF0B2005),
    secondary = Color(0xFF5A5F52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE3D4),
    onSecondaryContainer = Color(0xFF171D13),
    tertiary = Color(0xFF8A4A2F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFDF5),
    onBackground = Color(0xFF191D16),
    surface = Color(0xFFFBFDF5),
    onSurface = Color(0xFF191D16),
    surfaceVariant = Color(0xFFE1E4D9),
    onSurfaceVariant = Color(0xFF45483F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFAFC49E),
    onPrimary = Color(0xFF1B3613),
    primaryContainer = Color(0xFF324D29),
    onPrimaryContainer = Color(0xFFCBDFB9),
    secondary = Color(0xFFC2C9B7),
    onSecondary = Color(0xFF2C3227),
    secondaryContainer = Color(0xFF42483D),
    onSecondaryContainer = Color(0xFFDEE3D4),
    tertiary = Color(0xFFFFB59A),
    onTertiary = Color(0xFF52210E),
    background = Color(0xFF11140E),
    onBackground = Color(0xFFE1E4D9),
    surface = Color(0xFF11140E),
    onSurface = Color(0xFFE1E4D9),
    surfaceVariant = Color(0xFF45483F),
    onSurfaceVariant = Color(0xFFC5C8BC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun KardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}