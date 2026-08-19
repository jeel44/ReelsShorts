package reelsdrama.freedrama.videosdrama.presentation.components

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
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

// Matches the tag every other ad component in this app logs under (AdInitializer,
// RewardedAdManager, AppOpenAdManager, SplashViewModel, App) - was "AdBannerView" before,
// unified here so banner load attempts show up in the same logcat filter as everything else.
private const val TAG = "AdDebug"

/**
 * Thin DI bridge so [AdBannerView] can gate its ad load on [AdInitializer.isInitialized] the
 * same way every other ad placement in this app does (RewardedAdManager/InterstitialAdManager/
 * AppOpenAdManager consumers all do this via a screen ViewModel), for use by screens that embed
 * the banner but don't have a ViewModel of their own to source this from. Unused now that
 * Discover/CategoryLanding (the screens this was originally built for) have been removed, but
 * kept as the general-purpose entry point for any future ViewModel-less screen.
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
 * While loading - the SDK hasn't finished initializing yet, or a load is in flight - shows
 * [AdBannerSkeletonLoader] sized to match the real ad so there's no layout jump once it
 * resolves. If the ad fails to load, this composes to nothing at all - no placeholder box, no
 * reserved space - so it collapses to zero height rather than leaving a visible gap. Matches
 * this package's existing [ScreenPlaceholder] in shape: a single `Composable(modifier)` with
 * no other required parameters.
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
    adSize: AdSize? = null,
    viewModel: AdBannerViewModel = hiltViewModel()
) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    AdBannerView(adUnitId = adUnitId, isInitialized = isInitialized, modifier = modifier, adSize = adSize)
}

/**
 * Overload for screens whose own ViewModel already has [AdInitializer] access and mirrors it
 * into its UI state (same pattern `RewardedAdManager`/`InterstitialAdManager` consumers use
 * elsewhere in this app) - lets them pass that existing signal straight through instead of
 * spinning up the standalone [AdBannerViewModel] bridge above, which exists only for
 * ViewModel-less screens that have nothing else to source it from.
 *
 * Currently has no call site anywhere in the app - the reels feed's Home-screen bottom banner,
 * this overload's original consumer, has been removed. Kept as the general-purpose entry point
 * for any future ViewModel-owning screen that wants a fixed-size (or adaptive) banner strip.
 *
 * @param adSize Fixed size to request instead of the default full-width adaptive size below.
 * Pass e.g. [AdSize.BANNER] (a small 320x50 standard banner) for placements over a full-bleed
 * video, where a full-width adaptive banner would be too visually dominant. Leave null to keep
 * the existing adaptive full-width behavior.
 */
@Composable
fun AdBannerView(
    adUnitId: String,
    isInitialized: Boolean,
    modifier: Modifier = Modifier,
    adSize: AdSize? = null
) {
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

    // Hoisted out of the load effect below (and computed synchronously, not suspended) so the
    // skeleton loader can reserve the exact size the real ad will eventually take, before the
    // load even starts - no layout jump once loadAd() actually resolves.
    val effectiveAdSize = remember(adSize, screenWidthDp) {
        adSize ?: AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    var isAdLoaded by remember(adView) { mutableStateOf(false) }
    // Distinct from "not yet loaded": only true once the SDK has confirmed the load failed, so
    // the skeleton (below) can tell "still loading, keep showing the placeholder" apart from
    // "gave up, collapse to nothing" - both start out looking the same (isAdLoaded == false).
    var hasFailed by remember(adView) { mutableStateOf(false) }

    // Loads once this composable is on an actual Activity and MobileAds has finished
    // initializing - never in Compose Preview, which has no real ad SDK/network to talk to.
    LaunchedEffect(isInitialized, adView) {
        val currentAdView = adView
        Log.d(TAG, "AdBannerView($adUnitId): isInitialized=$isInitialized isPreviewMode=$isPreviewMode adView=${currentAdView != null}")
        if (isPreviewMode || !isInitialized || currentAdView == null) return@LaunchedEffect

        Log.d(TAG, "AdBannerView($adUnitId): calling loadAd() with size=$effectiveAdSize")
        currentAdView.loadAd(
            BannerAdRequest.Builder(adUnitId, effectiveAdSize).build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.d(TAG, "AdBannerView($adUnitId): onAdLoaded")
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    hasFailed = true
                    Log.w(TAG, "AdBannerView($adUnitId): onAdFailedToLoad: $adError")
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
    when {
        isAdLoaded && currentAdView != null -> {
            // Center, not the default TopStart: the default adaptive full-width size already
            // fills this Box so centering is a no-op there, but a fixed size like AdSize.BANNER
            // (Home) is narrower than the Box - centering keeps it a clean thin strip instead of
            // hugging the left edge.
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = Modifier.wrapContentSize(),
                    factory = { currentAdView }
                )
            }
        }
        !hasFailed -> {
            // Still loading (SDK init pending, load in flight, or not yet attempted) - show
            // the skeleton, sized to effectiveAdSize so there's no jump when the real ad
            // above replaces it.
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AdBannerSkeletonLoader(
                    modifier = Modifier
                        .width(effectiveAdSize.width.dp)
                        .height(effectiveAdSize.height.dp)
                )
            }
        }
        else -> {
            // Confirmed failed - nothing composed at all, same zero-height collapse as before
            // this skeleton existed.
        }
    }
}
