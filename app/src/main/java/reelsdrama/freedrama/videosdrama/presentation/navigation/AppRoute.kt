package reelsdrama.freedrama.videosdrama.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Main : AppRoute("main")
    data object Home : AppRoute("home")
    data object Rewards : AppRoute("rewards")
    data object Settings : AppRoute("settings")

    // Obsolete or unused routes from previous phases
    data object Search : AppRoute("search")
    data object Upload : AppRoute("upload")
    data object Notification : AppRoute("notification")
    data object Wallet : AppRoute("wallet")
}

/**
 * Items for the Bottom Navigation Bar
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = AppRoute.Home.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = AppRoute.Rewards.route,
        label = "Rewards",
        selectedIcon = Icons.Filled.Stars,
        unselectedIcon = Icons.Outlined.Stars
    ),
    BottomNavItem(
        route = AppRoute.Settings.route,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
