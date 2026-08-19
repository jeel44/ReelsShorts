package reelsdrama.freedrama.videosdrama.core.ads

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
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
 * there is only ever one app-open placement in practice, so a per-unit-ID cache would just be
 * unused generality. [preload]/[show] still take an `adUnitId` parameter for API consistency
 * with the other managers.
 *
 * Two callers today: [reelsdrama.freedrama.videosdrama.presentation.splash.SplashViewModel]'s
 * cold-start path, and [AppOpenAdForegroundTrigger]'s foreground-return (ON_START) path. [show]
 * itself enforces [AdConstants.APP_OPEN_MIN_INTERVAL_MS] and the [isShowing] re-entrancy guard
 * so both callers get the same protection without either needing to know about the other.
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

    /**
     * True from the moment [show] accepts an ad (right before `ad.show(activity)`) until either
     * `onAdDismissedFullScreenContent` or `onAdFailedToShowFullScreenContent` fires. Guards
     * against a second [show] call landing mid-display - now-explicit rather than the previous
     * "safe by accident because [loadedAd] is nulled immediately" behavior, which would silently
     * stop being safe if [show]'s implementation ever changed.
     */
    private var isShowing = false

    /**
     * Wall-clock time of the last confirmed `onAdShowedFullScreenContent`, 0L until the first
     * show. [show] enforces [AdConstants.APP_OPEN_MIN_INTERVAL_MS] against this - primarily for
     * [AppOpenAdForegroundTrigger], where without a cap a user toggling apps would otherwise see
     * an App Open ad on every single return.
     */
    private var lastShownAtMillis = 0L

    // Same self-made-scope idiom InterstitialAdManager already uses (and the identical bug this
    // mirrors the fix for - see that class's own doc comment on this field) - hops onto the main
    // thread before invoking the caller's onOutcome from onAdDismissedFullScreenContent/
    // onAdFailedToShowFullScreenContent below, which fire on the GMA SDK's own background
    // dispatch ("GMA(BG)"), not the main thread. Left out of scope by commit 7693624 when this
    // exact fix was applied to InterstitialAdManager, since nothing called AppOpenAdManager.show()
    // outside the cold-start path back then; now that AppOpenAdForegroundTrigger's resume-path
    // callback also touches state, it's in scope here too.
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        val requestedAt = System.currentTimeMillis()
        Log.d(TAG, "AppOpenAd.load() starting at $requestedAt for $adUnitId")

        AppOpenAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    val elapsed = System.currentTimeMillis() - requestedAt
                    Log.d(TAG, "AppOpenAd onAdLoaded after ${elapsed}ms")
                    isLoading = false
                    loadedAd = ad
                    loadTimeMillis = System.currentTimeMillis()
                    _isAdReady.value = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val elapsed = System.currentTimeMillis() - requestedAt
                    Log.d(TAG, "AppOpenAd onAdFailedToLoad after ${elapsed}ms: ${adError.message}")
                    isLoading = false
                    loadedAd = null
                    _isAdReady.value = false
                }
            },
        )
    }

    private fun isAdAvailable(): Boolean {
        val hasAd = loadedAd != null
        val ageMillis = System.currentTimeMillis() - loadTimeMillis
        val isFresh = ageMillis < EXPIRY_WINDOW_MILLIS
        Log.d(TAG, "isAdAvailable: hasAd=$hasAd loadTimeMillis=$loadTimeMillis ageMillis=$ageMillis isFresh=$isFresh")
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
     *
     * Also reports [AppOpenAdOutcome.NotAvailable] immediately, without touching [loadedAd] at
     * all, if an ad is already mid-display ([isShowing]) or [AdConstants.APP_OPEN_MIN_INTERVAL_MS]
     * hasn't elapsed since the last confirmed show - deliberately checked before the
     * [isAdAvailable] check so a loaded-but-not-yet-due ad is left alone for the next allowed
     * window instead of being consumed here.
     */
    fun show(activity: Activity, adUnitId: String, onOutcome: (AppOpenAdOutcome) -> Unit) {
        val sinceLastShownMillis = System.currentTimeMillis() - lastShownAtMillis
        Log.d(
            TAG,
            "show() called; isShowing=$isShowing sinceLastShownMillis=$sinceLastShownMillis " +
                "isAdAvailable=${isAdAvailable()} hasAd=${loadedAd != null}"
        )

        if (isShowing) {
            onOutcome(AppOpenAdOutcome.NotAvailable)
            return
        }
        if (sinceLastShownMillis < AdConstants.APP_OPEN_MIN_INTERVAL_MS) {
            onOutcome(AppOpenAdOutcome.NotAvailable)
            return
        }
        if (!isAdAvailable()) {
            loadedAd = null
            _isAdReady.value = false
            onOutcome(AppOpenAdOutcome.NotAvailable)
            return
        }

        val ad = loadedAd!!
        loadedAd = null
        _isAdReady.value = false
        isShowing = true

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                lastShownAtMillis = System.currentTimeMillis()
                onOutcome(AppOpenAdOutcome.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                // Dispatched, not called directly - this fires on the SDK's own background
                // thread (see mainScope's doc comment above), but onOutcome may do main-thread
                // work. Mirrors InterstitialAdManager.show()'s identical fix.
                mainScope.launch { onOutcome(AppOpenAdOutcome.Dismissed) }
                // The ad we just showed is consumed either way - line up a fresh one. Left on
                // the calling (background) thread, same rationale as InterstitialAdManager:
                // preload() only touches plain Kotlin fields and a StateFlow.
                preload(adUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                isShowing = false
                // Same risk and same fix as onAdDismissedFullScreenContent above.
                mainScope.launch { onOutcome(AppOpenAdOutcome.NotAvailable) }
                preload(adUnitId)
            }

            override fun onAdImpression() {}

            override fun onAdClicked() {}
        }

        ad.show(activity)
    }

    private companion object {
        const val TAG = "AdDebug"
        const val EXPIRY_WINDOW_MILLIS = 4 * 60 * 60 * 1000L // 4 hours
    }
}
