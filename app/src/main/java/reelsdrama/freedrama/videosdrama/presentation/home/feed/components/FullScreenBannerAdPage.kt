package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerSkeletonLoader
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerViewModel

private const val TAG = "AdDebug"

/**
 * A full-screen banner ad page for [VerticalReelsPager] - takes up an entire pager page exactly
 * like [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard] does for a real reel,
 * mirroring [FullScreenNativeAdPage]'s structure (shimmer-while-loading via
 * [AdBannerSkeletonLoader], swipeable pager page) but for the standard GMA Next-Gen SDK banner
 * format instead of native-advanced. Rendered when [reelsdrama.freedrama.videosdrama.domain.model.AdConfig]
 * (Firebase Realtime Database's `/adConfig`) selects a banner for this slot - see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.withAdSlots].
 *
 * Reuses [AdBannerViewModel] - the same thin Hilt bridge to [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer]
 * that [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView]'s standalone
 * overload already uses - rather than a second near-identical bridge ViewModel, since this page
 * needs nothing beyond `isInitialized`.
 *
 * Unlike [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView]'s strip
 * placement, this never collapses to nothing on a failed/absent load - it's a full pager page,
 * not a strip sitting above other content, so it just keeps showing the shimmer fallback exactly
 * like [FullScreenNativeAdPage] does while nothing is ready.
 */
@Composable
fun FullScreenBannerAdPage(
    adUnitId: String,
    modifier: Modifier = Modifier,
    viewModel: AdBannerViewModel = hiltViewModel()
) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val isPreviewMode = LocalInspectionMode.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // The ad container view itself - constructed eagerly (once an Activity exists and this
    // isn't Compose Preview) so loadAd() has somewhere to load into, same technique as
    // AdBannerView.
    val adView = remember(activity, isPreviewMode) {
        if (isPreviewMode) null else activity?.let { AdView(it) }
    }

    // Full-width adaptive size (not the small AdSize.BANNER used by the feed's bottom strip) -
    // this is a full-screen slot, so it should read like a real page, not a thin bar.
    val effectiveAdSize = remember(screenWidthDp) {
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    var isAdLoaded by remember(adView) { mutableStateOf(false) }

    LaunchedEffect(isInitialized, adView) {
        val currentAdView = adView
        Log.d(TAG, "FullScreenBannerAdPage($adUnitId): isInitialized=$isInitialized isPreviewMode=$isPreviewMode adView=${currentAdView != null}")
        if (isPreviewMode || !isInitialized || currentAdView == null) return@LaunchedEffect

        currentAdView.loadAd(
            BannerAdRequest.Builder(adUnitId, effectiveAdSize).build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.d(TAG, "FullScreenBannerAdPage($adUnitId): onAdLoaded")
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    Log.w(TAG, "FullScreenBannerAdPage($adUnitId): onAdFailedToLoad: $adError")
                }
            },
        )
    }

    // Destroys the AdView whenever it's swapped out or this page leaves the pager's live
    // composition window, same cleanup contract as AdBannerView.
    DisposableEffect(adView) {
        onDispose { adView?.destroy() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val currentAdView = adView
        if (isAdLoaded && currentAdView != null) {
            AndroidView(
                modifier = Modifier.width(effectiveAdSize.width.dp).height(effectiveAdSize.height.dp),
                factory = { currentAdView }
            )
        } else {
            AdBannerSkeletonLoader(
                modifier = Modifier
                    .width(effectiveAdSize.width.dp)
                    .height(effectiveAdSize.height.dp)
            )
        }
    }
}
