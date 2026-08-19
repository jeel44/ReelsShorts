package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView

/**
 * A full-screen pager page for [VerticalReelsPager] whose ad content is a centered, fixed
 * 300x250dp AdMob MEDIUM_RECTANGLE ("MREC") card - a "square-ish" ad card, distinct from a
 * full-width strip ([AdBannerView]'s original use) or a full-bleed native ad
 * ([FullScreenNativeAdPage]) - on a black background, the same full-page footprint every other
 * ad slot in this pager gets.
 *
 * Unlike [FullScreenBannerAdPage], this does NOT re-implement [AdBannerView]'s AdView load/
 * skeleton/cleanup logic: [AdBannerView] already fully supports an arbitrary fixed [AdSize] -
 * including sizing its own [AdBannerSkeletonLoader] fallback and horizontally centering the
 * result - so this composable is just [AdBannerView] configured with [AdSize.MEDIUM_RECTANGLE],
 * wrapped in a full-size, center-aligned [Box]. [AdBannerView] on its own only centers
 * horizontally ([androidx.compose.foundation.layout.fillMaxWidth]); this [Box] is what supplies
 * true both-axes centering for a full pager page.
 */
@Composable
fun FullScreenMediumRectangleAdPage(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AdBannerView(adUnitId = adUnitId, adSize = AdSize.MEDIUM_RECTANGLE)
    }
}
