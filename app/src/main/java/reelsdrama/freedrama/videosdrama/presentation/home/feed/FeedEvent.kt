package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.app.Activity

sealed interface FeedEvent {
    data object LoadMoreVideos : FeedEvent
    data class ReelSwiped(val fromVideoId: String) : FeedEvent
    data class ToggleAdConfirmation(val show: Boolean) : FeedEvent

    /** User confirmed the rewarded-ad dialog and the ad should be shown now. */
    data class WatchRewardedAd(val activity: Activity) : FeedEvent

    /** UI has shown [FeedUiState.rewardedAdFeedback] - clear it so it doesn't repeat. */
    data object ConsumeRewardedAdFeedback : FeedEvent

    /**
     * This screen (its NavBackStackEntry) just became resumed again - fired on first display
     * and on every return trip from another tab (e.g. Rewards). See
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel.recheckCoinBalance]
     * for why this needs its own explicit recheck rather than relying on the coin-balance flow
     * alone.
     */
    data object ScreenResumed : FeedEvent
}
