package reelsdrama.freedrama.videosdrama.presentation.navigation

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Home : AppRoute("home")
    data object Search : AppRoute("search")
    data object Upload : AppRoute("upload")
    data object Notification : AppRoute("notification")
    data object Wallet : AppRoute("wallet")
    data object Settings : AppRoute("settings")
}
