package reelsdrama.freedrama.videosdrama.presentation.components

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import javax.inject.Inject

private const val TAG = "AdBannerView"

/**
 * Thin DI bridge so [AdBannerView] can gate its ad load on [AdInitializer.isInitialized] the
 * same way every other ad placement in this app does (RewardedAdManager/InterstitialAdManager/
 * AppOpenAdManager consumers all do this via a screen ViewModel), even though the screens that
 * embed the banner - DiscoverScreen, CategoryLandingScreen - don't have one of their own.
 *
 * Scoped only to this composable via `hiltViewModel()`; it does not change how those screens
 * are built or require them to know it exists. See the Part 6 report for why this exists
 * instead of a raw Hilt EntryPoint or skipping the gate.
 *
 * Not `internal` despite being a private implementation detail of [AdBannerView] - Kotlin
 * doesn't allow a public function to expose a less-visible type as a parameter's default
 * value type, and [AdBannerView] needs to stay public for other screens to call it.
 */
@HiltViewModel
class AdBannerViewModel @Inject constructor(
    adInitializer: AdInitializer
) : ViewModel() {
    val isInitialized: StateFlow<Boolean> = adInitializer.isInitialized
}

/**
 * A standard GMA Next-Gen SDK banner ad (`com.google.android.libraries.ads.mobile.sdk.banner`),
 * sized to fill the width it's given - meant to sit as a fixed, non-scrolling element at the
 * bottom of a screen.
 *
 * If the ad fails to load, hasn't finished loading yet, or the SDK hasn't finished
 * initializing, this composes to nothing at all - no placeholder box, no reserved space -
 * so it collapses to zero height rather than leaving a visible gap. Matches this package's
 * existing [ScreenPlaceholder] in shape: a single `Composable(modifier)` with no other
 * required parameters.
 *
 * Implementation note: uses [AdView] + `adView.loadAd(...)`, NOT `BannerAd.load(...)` +
 * `BannerAd.getView(activity)` - the latter is deprecated in ads-mobile-sdk 1.3.1 (confirmed
 * by the compiler, not the deprecation message alone; Google's own Compose sample
 * (`ComposeBannerFragment.kt`) hasn't been updated off it yet, so this adapts the *non*-Compose
 * `BannerFragment.kt`/`BannerSnippets.kt` samples - which do use the current API - into
 * Compose via the same `AndroidView` bridging technique). With this API the ad container view
 * exists first and is loaded into afterwards (inverted from the old load-then-get-view flow),
 * so the zero-height-on-failure guarantee is now enforced explicitly via [isAdLoaded] rather
 * than relying on any assumption that a bare unloaded/failed [AdView] collapses on its own.
 */
@Composable
fun AdBannerView(
    adUnitId: String,
    modifier: Modifier = Modifier,
    viewModel: AdBannerViewModel = hiltViewModel()
) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isPreviewMode = LocalInspectionMode.current

    // The ad container view itself - constructed eagerly (once an Activity exists and this
    // isn't Compose Preview) so loadAd() has somewhere to load into. Never touches the real
    // SDK in Preview mode: this stays null there, same guarantee as before.
    val adView = remember(activity, isPreviewMode) {
        if (isPreviewMode) null else activity?.let { AdView(it) }
    }

    var isAdLoaded by remember(adView) { mutableStateOf(false) }

    // Loads once this composable is on an actual Activity and MobileAds has finished
    // initializing - never in Compose Preview, which has no real ad SDK/network to talk to.
    LaunchedEffect(isInitialized, adView) {
        val currentAdView = adView
        if (isPreviewMode || !isInitialized || currentAdView == null) return@LaunchedEffect

        val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
        currentAdView.loadAd(
            BannerAdRequest.Builder(adUnitId, adSize).build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    Log.w(TAG, "Banner ad failed to load: $adError")
                }
            },
        )
    }

    // Destroys the AdView (not a transient BannerAd result - the persistent container view
    // itself now owns cleanup) whenever it's swapped out or this composable leaves.
    DisposableEffect(adView) {
        onDispose { adView?.destroy() }
    }

    val currentAdView = adView
    if (isAdLoaded && currentAdView != null) {
        Box(modifier = modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { currentAdView }
            )
        }
    }
}
