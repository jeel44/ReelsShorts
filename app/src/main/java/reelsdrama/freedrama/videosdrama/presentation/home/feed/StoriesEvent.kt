package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.app.Activity

/** Mirrors [FeedEvent] for the Stories feed - see that file's per-case docs for rationale. */
sealed interface StoriesEvent {
    data object LoadMoreStories : StoriesEvent
    data class StorySwiped(val fromStoryId: String) : StoriesEvent
    data class ToggleAdConfirmation(val show: Boolean) : StoriesEvent

    /** User confirmed the rewarded-ad dialog and the ad should be shown now. */
    data class WatchRewardedAd(val activity: Activity) : StoriesEvent

    /** UI has shown [StoriesUiState.rewardedAdFeedback] - clear it so it doesn't repeat. */
    data object ConsumeRewardedAdFeedback : StoriesEvent

    /** See [FeedEvent.ScreenResumed]'s doc comment - same rationale. */
    data object ScreenResumed : StoriesEvent
}
