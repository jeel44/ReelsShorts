package reelsdrama.freedrama.videosdrama.core.ads

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a single app-open-ad show attempt. Same 3-state shape as [InterstitialAdOutcome]
 * and the same discipline: callers must only resume normal app flow (here: navigating past
 * splash) after [Dismissed] or [NotAvailable] - never on [Shown] alone, since the ad is still
 * on screen at that point.
 */
sealed interface AppOpenAdOutcome {
    /** The ad is now confirmed on screen (`onAdShowedFullScreenContent`). Still showing. */
    data object Shown : AppOpenAdOutcome

    /** The ad that was shown has been closed - safe to resume normal app flow. */
    data object Dismissed : AppOpenAdOutcome

    /** No ad was loaded (or it was stale/expired), or it failed to show - nothing displayed. */
    data object NotAvailable : AppOpenAdOutcome
}

/**
 * Loads and shows a GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk`) app
 * open ad.
 *
 * Same load/preload/show/reload shape as [InterstitialAdManager] and [RewardedAdManager].
 * Unlike those, this deliberately holds a single ad slot rather than a `Map<adUnitId, Ad>` -
 * there is only ever one app-open placement in practice (shown once per cold start), so a
 * per-unit-ID cache would just be unused generality. [preload]/[show] still take an
 * `adUnitId` parameter for API consistency with the other managers.
 *
 * Ad expiry: confirmed against Google's own current official GMA Next-Gen sample
 * (`AppOpenAdManager.kt` in `gma-next-gen-sdk-android-examples`) - the SDK does NOT
 * auto-invalidate a loaded [AppOpenAd] itself after any window. Google's guidance is that
 * ads shown more than ~4 hours after load "are no longer valid and may not earn revenue",
 * and their own reference implementation enforces that manually by tracking load time - so
 * this class does the same, using the identical 4-hour threshold.
 *
 * Callers must not invoke [preload] or [show] until [AdInitializer.isInitialized] is true.
 */
@Singleton
class AppOpenAdManager @Inject constructor() {

    private var loadedAd: AppOpenAd? = null
    private var loadTimeMillis: Long = 0L
    private var isLoading = false

    private val _isAdReady = MutableStateFlow(false)

    /** True if a non-expired app open ad is currently loaded and ready to show. */
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    /**
     * Starts loading an app open ad for [adUnitId] if one isn't already loaded (and
     * unexpired) or currently loading. Safe to call repeatedly.
     */
    fun preload(adUnitId: String) {
        if (isLoading || isAdAvailable()) return
        isLoading = true

        AppOpenAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    loadedAd = ad
                    loadTimeMillis = System.currentTimeMillis()
                    _isAdReady.value = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    loadedAd = null
                    _isAdReady.value = false
                }
            },
        )
    }

    private fun isAdAvailable(): Boolean {
        val hasAd = loadedAd != null
        val isFresh = System.currentTimeMillis() - loadTimeMillis < EXPIRY_WINDOW_MILLIS
        return hasAd && isFresh
    }

    /**
     * Shows the loaded app open ad for [adUnitId] (if any and not expired) on [activity] and
     * reports progress via [onOutcome] - [AppOpenAdOutcome.Shown] when it appears, then
     * exactly one of [AppOpenAdOutcome.Dismissed] or [AppOpenAdOutcome.NotAvailable] to mark
     * the end of the attempt. If nothing usable is loaded, reports
     * [AppOpenAdOutcome.NotAvailable] immediately and does nothing else - it does NOT block
     * the caller or auto-trigger a load; callers are expected to already be preloading and to
     * treat [AppOpenAdOutcome.NotAvailable] as "skip showing an ad this launch".
     *
     * Whichever way the attempt ends, this always starts loading a fresh ad for [adUnitId]
     * afterwards so it's ready for the next cold start.
     */
    fun show(activity: Activity, adUnitId: String, onOutcome: (AppOpenAdOutcome) -> Unit) {
        if (!isAdAvailable()) {
            loadedAd = null
            _isAdReady.value = false
            onOutcome(AppOpenAdOutcome.NotAvailable)
            return
        }

        val ad = loadedAd!!
        loadedAd = null
        _isAdReady.value = false

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                onOutcome(AppOpenAdOutcome.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                onOutcome(AppOpenAdOutcome.Dismissed)
                // The ad we just showed is consumed either way - line up a fresh one.
                preload(adUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                onOutcome(AppOpenAdOutcome.NotAvailable)
                preload(adUnitId)
            }

            override fun onAdImpression() {}

            override fun onAdClicked() {}
        }

        ad.show(activity)
    }

    private companion object {
        const val EXPIRY_WINDOW_MILLIS = 4 * 60 * 60 * 1000L // 4 hours
    }
}
