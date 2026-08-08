package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

@Immutable
data class FeedUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val categoryId: String? = null,
    val insufficientCoins: Boolean = false,
    val coinBalance: Int = 0,
    val showAdConfirmation: Boolean = false
)
