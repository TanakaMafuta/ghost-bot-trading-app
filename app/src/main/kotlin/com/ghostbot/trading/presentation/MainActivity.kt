package com.ghostbot.trading.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.ghostbot.trading.presentation.navigation.GhostBotNavigation
import com.ghostbot.trading.presentation.theme.GhostBotTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main Activity for Ghost Bot Trading App
 * Entry point for the application with Jetpack Compose UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        Timber.d("MainActivity created")
        
        setContent {
            GhostBotApp()
        }
    }
}

@Composable
fun GhostBotApp() {
    val navController = rememberNavController()
    
    GhostBotTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            GhostBotNavigation(navController = navController)
        }
    }
}