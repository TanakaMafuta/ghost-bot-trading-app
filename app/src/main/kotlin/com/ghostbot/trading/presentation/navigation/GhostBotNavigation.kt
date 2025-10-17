package com.ghostbot.trading.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ghostbot.trading.presentation.screens.dashboard.DashboardScreen
import com.ghostbot.trading.presentation.screens.security.SecurityPinScreen
import com.ghostbot.trading.presentation.screens.login.LoginScreen
import com.ghostbot.trading.presentation.screens.market.MarketAnalysisScreen
import com.ghostbot.trading.presentation.screens.config.TradingConfigScreen
import com.ghostbot.trading.presentation.screens.history.TradingHistoryScreen
import com.ghostbot.trading.presentation.screens.notifications.NotificationsScreen
import com.ghostbot.trading.presentation.screens.settings.SettingsScreen
import com.ghostbot.trading.presentation.screens.analysis.DetailedAnalysisScreen

/**
 * Main navigation component for Ghost Bot
 * Handles all screen navigation and routing
 */
@Composable
fun GhostBotNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SecurityPin.route
    ) {
        // Security Flow
        composable(Screen.SecurityPin.route) {
            SecurityPinScreen(
                onPinVerified = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SecurityPin.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Login Flow
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackPressed = {
                    navController.navigate(Screen.SecurityPin.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Main App Flow with Bottom Navigation
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        
        composable(Screen.Market.route) {
            MarketAnalysisScreen(navController = navController)
        }
        
        composable(Screen.Config.route) {
            TradingConfigScreen(navController = navController)
        }
        
        composable(Screen.History.route) {
            TradingHistoryScreen(navController = navController)
        }
        
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController = navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        
        composable(Screen.Analysis.route) {
            DetailedAnalysisScreen(navController = navController)
        }
    }
}