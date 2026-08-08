package reelsdrama.freedrama.videosdrama.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import reelsdrama.freedrama.videosdrama.presentation.discover.DiscoverScreen
import reelsdrama.freedrama.videosdrama.presentation.discover.category.CategoryLandingScreen
import reelsdrama.freedrama.videosdrama.presentation.home.HomeRoute
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen
import reelsdrama.freedrama.videosdrama.presentation.navigation.AppRoute
import reelsdrama.freedrama.videosdrama.presentation.navigation.bottomNavItems
import reelsdrama.freedrama.videosdrama.presentation.rewards.RewardsScreen
import reelsdrama.freedrama.videosdrama.presentation.settings.SettingsRoute

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route?.contains("category_reels") != true

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon, 
                                    contentDescription = item.label 
                                ) 
                            },
                            label = { Text(item.label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = AppRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Home.route) { 
                HomeRoute(onSettingsClick = {
                    navController.navigate(AppRoute.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }) 
            }
            composable(AppRoute.Discover.route) { 
                DiscoverScreen(
                    onDramaClick = { category ->
                        navController.navigate(AppRoute.CategoryLanding.createRoute(category))
                    }
                ) 
            }
            composable(AppRoute.Rewards.route) { 
                RewardsScreen() 
            }
            composable(AppRoute.Settings.route) { 
                SettingsRoute(onBackClick = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }) 
            }
            composable(
                route = AppRoute.CategoryLanding.route
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                CategoryLandingScreen(
                    categoryId = categoryId,
                    onBackClick = { navController.popBackStack() },
                    onPosterClick = { category ->
                        navController.navigate(AppRoute.CategoryReels.createRoute(category))
                    }
                )
            }
            composable(
                route = AppRoute.CategoryReels.route
            ) {
                val viewModel: FeedViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                ReelsFeedScreen(
                    uiState = uiState,
                    playerManager = viewModel.playerManager,
                    onEvent = viewModel::onEvent,
                    onSettingsClick = {},
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
