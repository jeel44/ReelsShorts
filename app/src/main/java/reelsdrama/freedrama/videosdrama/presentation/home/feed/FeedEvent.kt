package reelsdrama.freedrama.videosdrama.presentation.home.feed

sealed interface FeedEvent {
    data object LoadMoreVideos : FeedEvent
    data class ReelSwiped(val fromVideoId: String) : FeedEvent
    data class ToggleAdConfirmation(val show: Boolean) : FeedEvent
}
