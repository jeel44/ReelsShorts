package reelsdrama.freedrama.videosdrama.core.ads

/**
 * One-shot UI feedback about the outcome of the last rewarded-ad show attempt for a given
 * placement (coin-gate unlock, "Watch & Earn", etc). Shared across every screen that shows
 * a rewarded ad via [RewardedAdManager] - each screen's ViewModel maps its own
 * [RewardedAdOutcome] to this the same way, so the UI-feedback shape (and snackbar wiring)
 * doesn't get reinvented per placement.
 */
sealed interface RewardedAdFeedback {
    /** [amount] is whatever the ad SDK actually reported, not a hardcoded guess. */
    data class Earned(val amount: Int) : RewardedAdFeedback
    data object DismissedEarly : RewardedAdFeedback
    data object NotAvailable : RewardedAdFeedback
}
