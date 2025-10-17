package com.ghostbot.trading.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ghostbot.trading.R

/**
 * Sealed class defining all screens in the Ghost Bot app
 * Provides type-safe navigation with route definitions
 */
sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int? = null,
    val icon: ImageVector? = null,
    val showInBottomNav: Boolean = false
) {
    // Security Flow
    object SecurityPin : Screen(
        route = "security_pin",
        titleRes = R.string.security_title
    )
    
    // Authentication Flow
    object Login : Screen(
        route = "login",
        titleRes = R.string.login_title
    )
    
    // Main App Screens (Bottom Navigation)
    object Dashboard : Screen(
        route = "dashboard",
        titleRes = R.string.nav_dashboard,
        icon = Icons.Filled.Dashboard,
        showInBottomNav = true
    )
    
    object Market : Screen(
        route = "market",
        titleRes = R.string.nav_market,
        icon = Icons.Filled.TrendingUp,
        showInBottomNav = true
    )
    
    object Config : Screen(
        route = "config",
        titleRes = R.string.nav_config,
        icon = Icons.Filled.Settings,
        showInBottomNav = true
    )
    
    object History : Screen(
        route = "history",
        titleRes = R.string.nav_history,
        icon = Icons.Filled.History,
        showInBottomNav = true
    )
    
    object More : Screen(
        route = "more",
        titleRes = R.string.nav_more,
        icon = Icons.Filled.MoreHoriz,
        showInBottomNav = true
    )
    
    // Extended Screens (accessed from More)
    object Notifications : Screen(
        route = "notifications",
        titleRes = R.string.nav_notifications,
        icon = Icons.Filled.Notifications
    )
    
    object Settings : Screen(
        route = "settings",
        titleRes = R.string.nav_settings,
        icon = Icons.Filled.Settings
    )
    
    object Analysis : Screen(
        route = "analysis",
        titleRes = R.string.nav_analysis,
        icon = Icons.Filled.Analytics
    )
    
    companion object {
        /**
         * Get all screens that should appear in bottom navigation
         */
        fun getBottomNavScreens(): List<Screen> {
            return listOf(
                Dashboard,
                Market,
                Config,
                History,
                More
            )
        }
        
        /**
         * Get screens accessible from the "More" section
         */
        fun getMoreScreens(): List<Screen> {
            return listOf(
                Notifications,
                Settings,
                Analysis
            )
        }
    }
}