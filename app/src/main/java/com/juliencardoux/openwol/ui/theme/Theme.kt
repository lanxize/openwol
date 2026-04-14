package com.juliencardoux.openwol.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { System, Light, Dark }

private val DarkColorScheme = darkColorScheme(
    primary                 = Gold,
    onPrimary               = OnGold,
    primaryContainer        = Color(0xFF4A3800),
    onPrimaryContainer      = Gold,
    secondary               = Color(0xFFCEC8B6),
    onSecondary             = Color(0xFF342E1C),
    secondaryContainer      = Color(0xFF4B4536),
    onSecondaryContainer    = Color(0xFFECE5D0),
    tertiary                = Gold,
    onTertiary              = OnGold,
    tertiaryContainer       = Color(0xFF4A3800),
    onTertiaryContainer     = Gold,
    error                   = ErrorDark,
    onError                 = OnErrorDark,
    errorContainer          = ErrorContainerDark,
    onErrorContainer        = OnErrorContainerDark,
    background              = DarkBg,
    onBackground            = OnDark,
    surface                 = DarkBg,
    onSurface               = OnDark,
    surfaceVariant          = DarkSurfaceVariant,
    onSurfaceVariant        = OnDarkVariant,
    surfaceContainer        = DarkContainer,
    surfaceContainerLow     = DarkContainerLow,
    surfaceContainerHigh    = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    outline                 = DarkOutline,
    outlineVariant          = DarkOutlineVariant,
    inverseSurface          = Color(0xFFE7E2DA),
    inverseOnSurface        = Color(0xFF312F2C),
    inversePrimary          = Color(0xFF7A5800),
)

private val LightColorScheme = lightColorScheme(
    primary                 = Blue,
    onPrimary               = OnBlue,
    primaryContainer        = BlueContainer,
    onPrimaryContainer      = OnBlueContainer,
    secondary               = BlueDim,
    onSecondary             = OnBlue,
    secondaryContainer      = Color(0xFFD6E2F9),
    onSecondaryContainer    = Color(0xFF0F1C2E),
    tertiary                = Blue,
    onTertiary              = OnBlue,
    tertiaryContainer       = BlueContainer,
    onTertiaryContainer     = OnBlueContainer,
    error                   = ErrorLight,
    onError                 = OnErrorLight,
    errorContainer          = ErrorContainerLight,
    onErrorContainer        = OnErrorContainerLight,
    background              = LightBg,
    onBackground            = OnLight,
    surface                 = LightBg,
    onSurface               = OnLight,
    surfaceVariant          = LightSurfaceVariant,
    onSurfaceVariant        = OnLightVariant,
    surfaceContainer        = LightContainer,
    surfaceContainerLow     = LightContainerLow,
    surfaceContainerHigh    = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
    outline                 = LightOutline,
    outlineVariant          = LightOutlineVariant,
    inverseSurface          = Color(0xFF312F2C),
    inverseOnSurface        = Color(0xFFF5F2EE),
    inversePrimary          = BlueInverse,
)

@Composable
fun OpenWOLTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light  -> false
        ThemeMode.Dark   -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
