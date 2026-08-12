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

    /** Every-5th-swipe trigger fired and the UI has an [activity] to show the interstitial on. */
    data class ShowInterstitialAd(val activity: Activity) : FeedEvent

    /** No Activity was available to show the pending interstitial - clear the trigger so
     *  playback isn't left paused forever waiting for an ad that will never be shown. */
    data object DismissInterstitialTrigger : FeedEvent
}
