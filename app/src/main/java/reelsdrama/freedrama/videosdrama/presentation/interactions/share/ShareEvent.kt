package reelsdrama.freedrama.videosdrama.presentation.interactions.share

sealed interface ShareEvent {
    data class OnAppClick(val packageName: String, val videoId: String, val caption: String) : ShareEvent
    data class OnCopyLink(val videoId: String) : ShareEvent
    data class OnMoreClick(val videoId: String, val caption: String) : ShareEvent
    data object SnackbarDismissed : ShareEvent
}
