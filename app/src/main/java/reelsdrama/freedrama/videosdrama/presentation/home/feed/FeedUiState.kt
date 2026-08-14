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
    /**
     * The coins [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.AdConfirmationDialog]'s
     * CTA actually grants, mirrored from [reelsdrama.freedrama.videosdrama.core.ads.RewardedAdManager.getRewardAmount]
     * the instant that ad unit's ad finishes loading (see [FeedViewModel.observeRewardedAdAvailability]).
     * Defaults to [DEFAULT_COIN_UNLOCK_REWARD_AMOUNT] - Google's test unit's actual configured
     * reward - only for the brief window before the first load completes; it's provisional
     * display copy for that gap, not the source of truth for what's granted (that's always the
     * SDK's own `RewardItem` at the moment the ad is shown).
     */
    val coinUnlockRewardAmount: Int = DEFAULT_COIN_UNLOCK_REWARD_AMOUNT,
    val rewardedAdFeedback: RewardedAdFeedback? = null,
    /** Mirrors [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer.isInitialized] so
     * the banner ad at the bottom of the feed can gate its load without needing its own
     * ViewModel (see the [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView]
     * overload that takes this directly). */
    val isAdInitialized: Boolean = false
)

/** See [FeedUiState.coinUnlockRewardAmount]'s doc comment. */
const val DEFAULT_COIN_UNLOCK_REWARD_AMOUNT = 10
