package reelsdrama.freedrama.videosdrama.core.ads

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk`) native-advanced ads,
 * one at a time per ad unit ID.
 *
 * Same load/preload/reload shape as [InterstitialAdManager]/[RewardedAdManager], adapted for
 * native ads: there is no `show(activity)` step - a native ad isn't a full-screen overlay the
 * SDK draws for you, it's data + assets the caller binds into its own `NativeAdView` wherever
 * it's placed (here: [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.FullScreenNativeAdPage],
 * which renders it as a real page in the reels pager). [take] replaces `show()` - it hands the
 * caller the loaded [NativeAd] (or null if nothing is ready yet) and, either way, immediately
 * starts loading a replacement so a later ad slot isn't left waiting on a cold load.
 *
 * A single-slot cache per ad unit ID (like the other managers) is correct here, not a queue:
 * the feed only ever has at most one native-ad page inside the pager's live composition window
 * (`beyondViewportPageCount = 1`) at a time, since ad slots are spaced every 5 reels apart - see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem.withNativeAdSlots].
 *
 * Callers must not invoke [preload] or [take] until [AdInitializer.isInitialized] is true.
 */
@Singleton
class NativeAdManager @Inject constructor() {

    private val loadedAds = mutableMapOf<String, NativeAd>()
    private val loadingUnitIds = mutableSetOf<String>()

    private val _readyAdUnitIds = MutableStateFlow<Set<String>>(emptySet())

    /** Ad unit IDs that currently have a loaded, untaken native ad ready to render. */
    val readyAdUnitIds: StateFlow<Set<String>> = _readyAdUnitIds.asStateFlow()

    /**
     * Starts loading a native ad for [adUnitId] if one isn't already loaded or currently
     * loading. Safe to call repeatedly - it's a no-op if a load is already in flight or an ad
     * is already cached and waiting to be taken.
     */
    fun preload(adUnitId: String) {
        if (loadedAds.containsKey(adUnitId) || loadingUnitIds.contains(adUnitId)) return
        loadingUnitIds += adUnitId

        NativeAdLoader.load(
            NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build(),
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    loadingUnitIds -= adUnitId
                    loadedAds[adUnitId] = nativeAd
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
     * Hands over the loaded native ad for [adUnitId] (if any), removing it from the cache, and
     * always starts loading a fresh replacement afterwards - mirrors [InterstitialAdManager.show]'s
     * "always line up the next one" behavior. Returns null if nothing was ready yet; callers
     * should keep showing their loading placeholder in that case rather than block - a later
     * recomposition can call [take] again once [readyAdUnitIds] confirms one is ready.
     */
    fun take(adUnitId: String): NativeAd? {
        val ad = loadedAds.remove(adUnitId)
        _readyAdUnitIds.update { it - adUnitId }
        preload(adUnitId)
        return ad
    }
}
