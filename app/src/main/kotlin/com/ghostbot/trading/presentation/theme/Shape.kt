package com.ghostbot.trading.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Ghost Bot Shape System
 * Defines consistent corner radius values throughout the app
 */
val GhostBotShapes = Shapes(
    // Extra Small - for small buttons, chips
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for text fields, small cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - for full screen modals
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for specific components
object CustomShapes {
    val ButtonPrimary = RoundedCornerShape(12.dp)
    val ButtonSecondary = RoundedCornerShape(8.dp)
    val ButtonFab = RoundedCornerShape(16.dp)
    
    val CardElevated = RoundedCornerShape(16.dp)
    val CardFlat = RoundedCornerShape(12.dp)
    val CardGlow = RoundedCornerShape(20.dp)
    
    val TextFieldOutlined = RoundedCornerShape(12.dp)
    val TextFieldFilled = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    val BottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    val Dialog = RoundedCornerShape(24.dp)
    val Snackbar = RoundedCornerShape(8.dp)
    
    val ProgressBar = RoundedCornerShape(4.dp)
    val Slider = RoundedCornerShape(12.dp)
    
    // Special shapes for Ghost Bot aesthetic
    val CyberHexagon = RoundedCornerShape(8.dp) // Will be customized later for hexagonal look
    val GlowContainer = RoundedCornerShape(16.dp)
    val SecurityPin = RoundedCornerShape(50) // Circular
}