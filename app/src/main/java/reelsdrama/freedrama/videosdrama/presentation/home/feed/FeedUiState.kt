package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

@Immutable
data class FeedUiState(
    val videos: List<Video> = emptyList(),
    val watchedVideoIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val categoryId: String? = null,
    val insufficientCoins: Boolean = false,
    val coinBalance: Int = 0,
    val showAdConfirmation: Boolean = false,
    val isRewardedAdReady: Boolean = false,
    val rewardedAdFeedback: RewardedAdFeedback? = null,
    /** True while an interstitial is pending or actually on screen - drives pausing playback. */
    val showInterstitial: Boolean = false
)
