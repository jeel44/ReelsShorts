package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

@Immutable
data class FeedUiState(
    val followingVideos: List<Video> = emptyList(),
    val forYouVideos: List<Video> = emptyList(),
    val selectedTabIndex: Int = 1, // Default to "For You"
    val isLoading: Boolean = false,
    val categoryId: String? = null,
    val insufficientCoins: Boolean = false,
    val coinBalance: Int = 0
)
