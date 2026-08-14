package reelsdrama.freedrama.videosdrama.core.constants

/**
 * TEST AD UNIT IDS — replace with real AdMob IDs before release build.
 *
 * Centralizes every AdMob identifier used across the app (the App ID and every ad unit
 * ID, one per placement/format) so swapping test IDs for production IDs is a single-file
 * edit. No AdMob ID should be hardcoded anywhere else in the codebase — reference these
 * constants instead.
 *
 * All values below are Google's official sample/test IDs:
 * https://developers.google.com/admob/android/test-ads
 */
object AdConstants {

    /**
     * AdMob App ID. Declared in AndroidManifest.xml's `com.google.android.gms.ads.APPLICATION_ID`
     * meta-data and passed to `MobileAds.initialize()`. Keep both in sync.
     */
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** Rewarded ad shown to unlock/consume a coin-gated reel in the feed. */
    const val REWARDED_COIN_UNLOCK_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /** Rewarded ad shown from the Rewards screen's "Watch & Earn" card. */
    const val REWARDED_WATCH_AND_EARN_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Interstitial ad unit. No longer wired into the reels feed's every-5-reels slot - that
     * placement now shows [NATIVE_FEED_UNIT_ID] instead (a full-screen native ad the user
     * swipes past as a real pager page, rather than a modal overlay on top of one). Now shown
     * instead when the user navigates back out of the Rewards screen - see
     * [reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel.RewardsViewModel.onBackFromRewards].
     */
    const val INTERSTITIAL_FEED_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Full-screen native-advanced ad shown as its own page in the reels feed every 5 reels -
     * Google's official native-advanced test unit. See
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem.withNativeAdSlots] and
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.FullScreenNativeAdPage].
     */
    const val NATIVE_FEED_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    /** App Open ad shown on cold/warm app start. */
    const val APP_OPEN_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    /** Banner ad, used as a fallback wherever native isn't used. */
    const val BANNER_FALLBACK_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
}
