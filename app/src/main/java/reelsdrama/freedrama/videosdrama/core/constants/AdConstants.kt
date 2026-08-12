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

    /** Interstitial ad shown in the reels feed, roughly every 5 reels. */
    const val INTERSTITIAL_FEED_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /** App Open ad shown on cold/warm app start. */
    const val APP_OPEN_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    /** Native ad shown within Discover/Category listings. */
    const val NATIVE_DISCOVER_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    /** Banner ad, used as a fallback wherever native isn't used. */
    const val BANNER_FALLBACK_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
}
