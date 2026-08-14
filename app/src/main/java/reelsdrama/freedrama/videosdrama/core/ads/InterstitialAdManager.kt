package reelsdrama.freedrama.videosdrama.core.ads

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a single interstitial-ad show attempt. Interstitials aren't reward-gated, so
 * there's no earned/dismissed-without-reward distinction like [RewardedAdOutcome] - callers
 * just need to know what stage the attempt reached, so they can log/skip gracefully and know
 * exactly when it's safe to resume anything they paused for the ad (only once [Dismissed] or
 * [NotAvailable] fires - never on [Shown] alone, since the ad is still on screen at that point).
 */
sealed interface InterstitialAdOutcome {
    /** The ad is now confirmed on screen (`onAdShowedFullScreenContent`). Still showing. */
    data object Shown : InterstitialAdOutcome

    /** The ad that was shown has been closed - safe to resume whatever was paused for it. */
    data object Dismissed : InterstitialAdOutcome

    /** No ad was loaded for this placement, or it failed to show - nothing was displayed. */
    data object NotAvailable : InterstitialAdOutcome
}

/**
 * Loads and shows GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk`)
 * interstitial ads, one at a time per ad unit ID.
 *
 * Same load/preload/show/reload shape as [RewardedAdManager] - deliberately kept as a
 * separate class rather than a shared base, since `InterstitialAd` and `RewardedAd` are
 * distinct SDK types with different `show()` signatures (no reward listener here) and there's
 * only a single interstitial placement today.
 *
 * Callers must not invoke [preload] or [show] until [AdInitializer.isInitialized] is true.
 */
@Singleton
class InterstitialAdManager @Inject constructor() {

    private val loadedAds = mutableMapOf<String, InterstitialAd>()
    private val loadingUnitIds = mutableSetOf<String>()

    // Same self-made-scope idiom AdInitializer already uses in this package
    // (CoroutineScope(SupervisorJob() + Dispatchers.X) as a private singleton field) - here to
    // hop onto the main thread before invoking the caller's onOutcome from
    // onAdDismissedFullScreenContent/onAdFailedToShowFullScreenContent below. Those two fire
    // from the GMA SDK's own background dispatch (confirmed via the "GMA(BG)" crashing thread
    // name), not the main thread, but callers of show() are entitled to assume onOutcome runs
    // on the main thread - same as every other Android callback API - since they may do
    // main-thread-only work in response (e.g. RewardsViewModel.onBackFromRewards calling
    // navController.navigate(), which crashed with "Method setCurrentState must be called on
    // the main thread" before this fix).
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _readyAdUnitIds = MutableStateFlow<Set<String>>(emptySet())

    /** Ad unit IDs that currently have a loaded, unshown ad ready to display. */
    val readyAdUnitIds: StateFlow<Set<String>> = _readyAdUnitIds.asStateFlow()

    /** True if [adUnitId] currently has a loaded ad ready to show right now. */
    fun isAdReady(adUnitId: String): Boolean = loadedAds.containsKey(adUnitId)

    /**
     * Starts loading an interstitial ad for [adUnitId] if one isn't already loaded or
     * currently loading. Safe to call repeatedly - it's a no-op if a load is already in
     * flight or an ad is already cached and waiting to be shown.
     */
    fun preload(adUnitId: String) {
        if (loadedAds.containsKey(adUnitId) || loadingUnitIds.contains(adUnitId)) return
        loadingUnitIds += adUnitId

        InterstitialAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingUnitIds -= adUnitId
                    loadedAds[adUnitId] = ad
                    _readyAdUnitIds.update { it + adUnitId }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loadingUnitIds -= adUnitId
                    loadedAds.remove(adUnitId)
                    _readyAdUnitIds.update { it - adUnitId }
                }
            },
        )
    }

    /**
     * Shows the loaded interstitial ad for [adUnitId] (if any) on [activity] and reports
     * progress via [onOutcome] - [InterstitialAdOutcome.Shown] when it appears, then exactly
     * one of [InterstitialAdOutcome.Dismissed] (closed normally) or re-reports
     * [InterstitialAdOutcome.NotAvailable] (failed to show after all) to mark the end of the
     * attempt. If no ad is loaded at all, reports [InterstitialAdOutcome.NotAvailable]
     * immediately and does nothing else - it does NOT block the caller or auto-trigger a
     * load; callers are expected to already be preloading and to treat
     * [InterstitialAdOutcome.NotAvailable] as "skip this cycle, try again next time".
     *
     * Whichever way the attempt ends, this always starts loading a fresh ad for [adUnitId]
     * afterwards so the next trigger isn't stuck on a stale/consumed ad.
     */
    fun show(activity: Activity, adUnitId: String, onOutcome: (InterstitialAdOutcome) -> Unit) {
        val ad = loadedAds.remove(adUnitId)
        _readyAdUnitIds.update { it - adUnitId }

        if (ad == null) {
            onOutcome(InterstitialAdOutcome.NotAvailable)
            return
        }

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                onOutcome(InterstitialAdOutcome.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                // Dispatched, not called directly - this fires on the SDK's own background
                // thread (see mainScope's doc comment above), but onOutcome may do main-thread
                // work (navigation, Compose state driving a view, etc.).
                mainScope.launch { onOutcome(InterstitialAdOutcome.Dismissed) }
                // The ad we just showed is consumed either way - line up a fresh one. Left on
                // the calling (background) thread, not dispatched - preload() only touches
                // plain Kotlin collections and a StateFlow, neither of which requires the main
                // thread, and there's no reason to delay the next load waiting for a main-thread
                // hop.
                preload(adUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                // Same risk and same fix as onAdDismissedFullScreenContent above.
                mainScope.launch { onOutcome(InterstitialAdOutcome.NotAvailable) }
                preload(adUnitId)
            }

            override fun onAdImpression() {}

            override fun onAdClicked() {}
        }

        ad.show(activity)
    }
}
