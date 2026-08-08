package reelsdrama.freedrama.videosdrama.presentation.home.feed

sealed interface FeedEvent {
    data object LoadMoreVideos : FeedEvent
    data class VideoViewed(val videoId: String) : FeedEvent
    data class ToggleAdConfirmation(val show: Boolean) : FeedEvent
}
