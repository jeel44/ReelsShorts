package reelsdrama.freedrama.videosdrama.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import reelsdrama.freedrama.videosdrama.presentation.main.MainScreen
import reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.Splash.route) {
        composable(AppRoute.Splash.route) {
            SplashRoute(
                onNavigateToHome = {
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.Main.route) {
            MainScreen()
        }
    }
}
