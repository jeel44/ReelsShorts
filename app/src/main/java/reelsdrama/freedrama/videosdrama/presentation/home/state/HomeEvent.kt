package reelsdrama.freedrama.videosdrama.presentation.home.state

sealed interface HomeEvent {
    data class TabSelected(val index: Int) : HomeEvent
    data class ReelChanged(val tabIndex: Int, val reelIndex: Int) : HomeEvent
    data class LikeClicked(val videoId: String) : HomeEvent
    data class CommentClicked(val videoId: String) : HomeEvent
    data class ShareClicked(val videoId: String) : HomeEvent
    data class GiftClicked(val videoId: String) : HomeEvent
    data class CoinsClicked(val videoId: String) : HomeEvent
}
