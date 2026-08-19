package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.domain.model.AdConfig
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story

/**
 * Mirrors [FeedUiState] for the Stories feed - see that class's field docs for anything not
 * re-explained here. A separate state class (not a shared one) because [StoriesViewModel] and
 * [FeedViewModel] are independent ViewModel instances - [ReelsFeedScreen]'s tab switcher shows
 * at most one of their feeds at a time, but both stay alive and keep collecting the same
 * underlying [reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository]/
 * [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository] flows independently.
 */
@Immutable
data class StoriesUiState(
    val stories: List<Story> = emptyList(),
    val watchedStoryIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val insufficientCoins: Boolean = false,
    val coinBalance: Int = 0,
    val showAdConfirmation: Boolean = false,
    val isRewardedAdReady: Boolean = false,
    /** See [FeedUiState.coinUnlockRewardAmount]'s doc comment - same rationale. */
    val coinUnlockRewardAmount: Int = DEFAULT_COIN_UNLOCK_REWARD_AMOUNT,
    val rewardedAdFeedback: RewardedAdFeedback? = null,
    /**
     * Mirrors [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository.getAdConfig]
     * so [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalStoriesPager]
     * can decide each every-3-stories slot's ad type, same as [FeedUiState.adConfig] does for
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager].
     */
    val adConfig: AdConfig = AdConfig()
)
