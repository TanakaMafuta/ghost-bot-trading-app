package com.ghostbot.trading.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Ghost Bot Color Scheme - Dark Theme (Primary)
val GhostBotDarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color.Black,
    primaryContainer = RoyalGoldDark,
    onPrimaryContainer = Color.White,
    
    secondary = RubyRed,
    onSecondary = Color.White,
    secondaryContainer = RubyRedDark,
    onSecondaryContainer = Color.White,
    
    tertiary = CyberBlue,
    onTertiary = Color.Black,
    tertiaryContainer = CyberBlueDark,
    onTertiaryContainer = Color.White,
    
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    
    error = RubyRed,
    onError = Color.White,
    errorContainer = RubyRedDark,
    onErrorContainer = Color.White,
    
    outline = RoyalGoldAlpha,
    outlineVariant = CyberBlueAlpha,
    
    scrim = Color.Black.copy(alpha = 0.8f)
)

// Light color scheme (for compatibility, though app is primarily dark)
val GhostBotLightColorScheme = lightColorScheme(
    primary = RoyalGoldDark,
    onPrimary = Color.White,
    primaryContainer = RoyalGold,
    onPrimaryContainer = Color.Black,
    
    secondary = RubyRedDark,
    onSecondary = Color.White,
    secondaryContainer = RubyRed,
    onSecondaryContainer = Color.White,
    
    tertiary = CyberBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = CyberBlue,
    onTertiaryContainer = Color.Black,
    
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF5F5F5),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF666666),
    
    error = RubyRed,
    onError = Color.White,
    errorContainer = RubyRedAlpha,
    onErrorContainer = RubyRedDark
)

@Composable
fun GhostBotTheme(
    darkTheme: Boolean = true, // Force dark theme for Ghost Bot aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GhostBotDarkColorScheme
        else -> GhostBotLightColorScheme
    }
    
    // Always use dark theme for Ghost Bot
    val finalColorScheme = GhostBotDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    
    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = GhostBotTypography,
        shapes = GhostBotShapes,
        content = content
    )
}