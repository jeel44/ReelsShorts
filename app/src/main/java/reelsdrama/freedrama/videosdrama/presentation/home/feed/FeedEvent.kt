package reelsdrama.freedrama.videosdrama.presentation.home.feed

sealed interface FeedEvent {
    data object LoadMoreVideos : FeedEvent
    data class VideoViewed(val videoId: String) : FeedEvent
}
