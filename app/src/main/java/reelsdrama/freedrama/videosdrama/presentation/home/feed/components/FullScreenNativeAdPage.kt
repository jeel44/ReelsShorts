package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.NativeAdManager
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerSkeletonLoader
import javax.inject.Inject

private const val TAG = "AdDebug"

/**
 * Thin DI bridge so [FullScreenNativeAdPage] can reach [NativeAdManager] and
 * [AdInitializer.isInitialized] the same way [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerViewModel]
 * bridges [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView] to
 * [AdInitializer]. This composable lives inside [VerticalReelsPager], several layers below
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel], so it sources both
 * directly via Hilt instead of threading them down through every intermediate composable's
 * parameter list.
 */
@HiltViewModel
class NativeAdSlotViewModel @Inject constructor(
    val nativeAdManager: NativeAdManager,
    adInitializer: AdInitializer
) : ViewModel() {
    val isInitialized: StateFlow<Boolean> = adInitializer.isInitialized
}

/**
 * A full-screen native ad page for [VerticalReelsPager] - takes up an entire pager page exactly
 * like [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard] does for a real reel,
 * so the user swipes past it the same way instead of it appearing as a modal overlay on top of
 * one (that was the old interstitial's behavior; this replaces it - see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem.withAdSlots]).
 *
 * While no ad is ready yet (SDK not initialized, or the load still in flight), shows
 * [AdBannerSkeletonLoader] full-screen - the same shimmer component
 * [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView] uses for its banner
 * placeholder, just filling the whole page instead of a small banner-sized box. This is meant
 * to be a fallback for a slow load, not the default experience: [NativeAdManager] is preloaded
 * well before the user reaches this page (see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel.preloadNativeAdWhenReady]
 * plus [NativeAdManager.take]'s own always-reload-the-next-one behavior), so by the time this
 * page is actually swiped to, an ad is normally already sitting there ready to bind.
 *
 * Uses `AndroidView` interop, not a Compose-native layout - Compose has no `NativeAdView`
 * equivalent, so the ad's headline/body/icon/media/CTA are bound into a plain Android view
 * hierarchy per the GMA Next-Gen SDK's required [NativeAdView] asset-view binding, exactly the
 * same interop technique [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView]
 * already uses for its own `AdView`.
 */
@Composable
fun FullScreenNativeAdPage(
    adUnitId: String,
    modifier: Modifier = Modifier,
    viewModel: NativeAdSlotViewModel = hiltViewModel()
) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val isPreviewMode = LocalInspectionMode.current
    val density = LocalDensity.current

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Takes a preloaded ad the instant the SDK is ready - normally already sitting in
    // NativeAdManager's cache by the time the user swipes this far, since FeedViewModel starts
    // preloading it well before this page is ever composed. If nothing is ready yet (cold
    // start, or a previous load failed), NativeAdManager.take() returns null and this page just
    // keeps showing the shimmer below - there's no manual retry loop here because take() always
    // re-triggers preload() internally, and readyAdUnitIds changing would recompose a consumer
    // that observed it; this page instead just re-checks once per (isInitialized, adUnitId)
    // change, which is enough since a slot is only ever composed once per swipe-through.
    LaunchedEffect(isInitialized, adUnitId) {
        if (isPreviewMode || !isInitialized || nativeAd != null) return@LaunchedEffect
        val ad = viewModel.nativeAdManager.take(adUnitId)
        Log.d(TAG, "FullScreenNativeAdPage($adUnitId): take() -> ${if (ad != null) "hit" else "miss, showing shimmer"}")
        nativeAd = ad
    }

    // Destroys the ad the moment this specific page instance is gone (swiped away and recycled
    // out of the pager, or the feed itself torn down) - AdMob's documented cleanup step for a
    // NativeAd once it's done being displayed.
    DisposableEffect(nativeAd) {
        val adToDestroy = nativeAd
        onDispose { adToDestroy?.destroy() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val ad = nativeAd
        if (ad != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> buildNativeAdView(context, density) },
                update = { view -> bindNativeAd(view, ad) }
            )
        } else {
            AdBannerSkeletonLoader(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Builds the (initially empty) view hierarchy a [NativeAd] gets bound into by [bindNativeAd]:
 * a full-bleed [MediaView] behind a bottom scrim panel holding icon/headline/body/CTA, with
 * [AdChoicesView] pinned to the top-end corner per AdMob's placement requirement. Built in code
 * rather than an XML layout - this is the only screen in the app that needs a native ad view
 * tree, so a dedicated layout resource would be pure ceremony for a single call site.
 */
private fun buildNativeAdView(context: Context, density: androidx.compose.ui.unit.Density): NativeAdView {
    fun Dp.toPx(): Int = with(density) { roundToPx() }

    val mediaView = MediaView(context)

    val iconView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        layoutParams = LinearLayout.LayoutParams(ICON_SIZE.toPx(), ICON_SIZE.toPx())
    }

    val headlineView = TextView(context).apply {
        setTextColor(AndroidColor.WHITE)
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }

    val bodyView = TextView(context).apply {
        setTextColor(AndroidColor.argb(210, 255, 255, 255))
        textSize = 14f
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, PANEL_TEXT_GAP.toPx(), 0, 0)
    }

    val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            marginStart = PANEL_PADDING.toPx()
        }
        addView(headlineView)
        addView(bodyView)
    }

    val infoRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(iconView)
        addView(textColumn)
    }

    val ctaView = Button(context).apply {
        setTextColor(AndroidColor.BLACK)
        setBackgroundColor(AndroidColor.parseColor("#F5C542")) // same coin-yellow accent used elsewhere in the feed
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = PANEL_PADDING.toPx()
        }
    }

    val bottomPanel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(AndroidColor.argb(179, 0, 0, 0)) // translucent scrim, matches VideoInfoOverlay's bottom gradient intent
        setPadding(PANEL_PADDING.toPx(), PANEL_PADDING.toPx(), PANEL_PADDING.toPx(), PANEL_PADDING.toPx())
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM)
        addView(infoRow)
        addView(ctaView)
    }

    val adChoicesView = AdChoicesView(context).apply {
        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            topMargin = PANEL_PADDING.toPx()
            marginEnd = PANEL_PADDING.toPx()
        }
    }

    val root = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(mediaView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(bottomPanel)
        addView(adChoicesView)
    }

    return NativeAdView(context).apply {
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(root)
        // NativeAdView.mediaView is only populated *after* registerNativeAd() tags it
        // internally (confirmed by decompiling the SDK's registerNativeAd - it's the one place
        // that associates a MediaView with the container), so there's no getter to rely on
        // beforehand. Stash the instance we just built via the plain View.tag slot instead, so
        // bindNativeAd() below can hand the same MediaView back to registerNativeAd().
        tag = mediaView
        // Property-assignment syntax, not `.setHeadlineView(...)` etc. - these are genuine
        // Kotlin `var` properties on the SDK's side (confirmed by decompiling: calling the
        // underlying setHeadlineView/setBodyView/setIconView/setCallToActionView/setAdChoicesView
        // methods directly is an unresolved reference from Kotlin, only the property form
        // resolves), same as any other Kotlin-to-Kotlin property access.
        this.headlineView = headlineView
        this.bodyView = bodyView
        this.iconView = iconView
        this.callToActionView = ctaView
        this.adChoicesView = adChoicesView
    }
}

/** Populates a [NativeAdView] built by [buildNativeAdView] with [nativeAd]'s actual assets. */
private fun bindNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    val body = nativeAd.body
    (nativeAdView.bodyView as? TextView)?.apply {
        text = body
        visibility = if (body.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction

    val iconDrawable = nativeAd.icon?.drawable
    (nativeAdView.iconView as? ImageView)?.apply {
        if (iconDrawable != null) {
            setImageDrawable(iconDrawable)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    val mediaView = nativeAdView.tag as? MediaView ?: return
    mediaView.mediaContent = nativeAd.mediaContent
    nativeAdView.registerNativeAd(nativeAd, mediaView)
}

private val ICON_SIZE: Dp = 44.dp
private val PANEL_PADDING: Dp = 16.dp
private val PANEL_TEXT_GAP: Dp = 4.dp
