package reelsdrama.freedrama.videosdrama.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import reelsdrama.freedrama.videosdrama.presentation.home.HomeRoute
import reelsdrama.freedrama.videosdrama.presentation.notification.NotificationRoute
import reelsdrama.freedrama.videosdrama.presentation.search.SearchRoute
import reelsdrama.freedrama.videosdrama.presentation.settings.SettingsRoute
import reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute
import reelsdrama.freedrama.videosdrama.presentation.upload.UploadRoute
import reelsdrama.freedrama.videosdrama.presentation.wallet.WalletRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.Splash.route) {
        composable(AppRoute.Splash.route) { SplashRoute() }
        composable(AppRoute.Home.route) { HomeRoute() }
        composable(AppRoute.Search.route) { SearchRoute() }
        composable(AppRoute.Upload.route) { UploadRoute() }
        composable(AppRoute.Notification.route) { NotificationRoute() }
        composable(AppRoute.Wallet.route) { WalletRoute() }
        composable(AppRoute.Settings.route) { SettingsRoute() }
    }
}
