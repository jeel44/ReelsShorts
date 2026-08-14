package reelsdrama.freedrama.videosdrama.presentation.main

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import reelsdrama.freedrama.videosdrama.presentation.home.HomeRoute
import reelsdrama.freedrama.videosdrama.presentation.main.components.BrandedBottomNav
import reelsdrama.freedrama.videosdrama.presentation.navigation.AppRoute
import reelsdrama.freedrama.videosdrama.presentation.navigation.bottomNavItems
import reelsdrama.freedrama.videosdrama.presentation.rewards.RewardsScreen
import reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel.RewardsViewModel
import reelsdrama.freedrama.videosdrama.presentation.settings.SettingsRoute

/** The one tab-switch pattern every bottom-nav/back-ish navigation in this app uses - pop down
 *  to the start destination (saving state) then navigate to [route], restoring its state if it
 *  had any saved. Factored out so every call site (bottom nav, Home's coin click, Rewards'/
 *  Settings' back actions) stays byte-for-byte identical rather than five separately hand-typed
 *  copies of the same block. */
private fun NavHostController.navigateAsTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val activity = LocalActivity.current

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isOnRewards = currentDestination?.hierarchy?.any { it.route == AppRoute.Rewards.route } == true

            // Resolved only while actually on Rewards, scoped to that exact back stack entry -
            // this is the SAME RewardsViewModel instance RewardsScreen() itself holds below (its
            // own hiltViewModel() call resolves against that identical entry), not a second one.
            // That's what lets tapping "Home" here go through the exact same
            // interstitial-then-navigate logic (RewardsViewModel.onExitRewards) as Rewards'
            // in-screen back handling, instead of a second, duplicated copy of it.
            val rewardsViewModel = navBackStackEntry?.takeIf { isOnRewards }?.let {
                hiltViewModel<RewardsViewModel>(it)
            }

            BrandedBottomNav(
                items = bottomNavItems,
                isSelected = { item ->
                    currentDestination?.hierarchy?.any { it.route == item.route } == true
                },
                onItemClick = { item ->
                    if (isOnRewards && item.route == AppRoute.Home.route && rewardsViewModel != null) {
                        // Leaving Rewards via the bottom nav's "Home" tap is a second, genuinely
                        // separate exit path from RewardsScreen's own BackHandler - this calls
                        // navigate() directly rather than going through it - so it needs the
                        // same ad gate explicitly.
                        rewardsViewModel.onExitRewards(activity) {
                            navController.navigateAsTab(AppRoute.Home.route)
                        }
                    } else {
                        navController.navigateAsTab(item.route)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Home.route) {
                HomeRoute(onCoinClick = {
                    navController.navigateAsTab(AppRoute.Rewards.route)
                })
            }
            composable(AppRoute.Rewards.route) {
                RewardsScreen(onBackClick = {
                    navController.navigateAsTab(AppRoute.Home.route)
                })
            }
            composable(AppRoute.Settings.route) {
                SettingsRoute(onBackClick = {
                    navController.navigateAsTab(AppRoute.Home.route)
                })
            }
        }
    }
}
