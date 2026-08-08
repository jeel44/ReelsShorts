package reelsdrama.freedrama.videosdrama.presentation.home.feed

sealed interface FeedEvent {
    data class TabSelected(val index: Int) : FeedEvent
    data class LoadMoreVideos(val tabIndex: Int) : FeedEvent
    data class VideoViewed(val videoId: String) : FeedEvent
}
