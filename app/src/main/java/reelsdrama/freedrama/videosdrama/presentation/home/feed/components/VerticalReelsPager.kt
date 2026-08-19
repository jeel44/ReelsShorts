package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import reelsdrama.freedrama.videosdrama.core.constants.PlayerConstants
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.domain.model.AdConfig
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedEvent
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard
import reelsdrama.freedrama.videosdrama.presentation.home.feed.withAdSlots
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * Vertical pager for reels. Handles playback orchestration and pagination.
 *
 * Every 3rd reel, a full-screen ad page - a native ad ([FeedItem.NativeAdItem], rendered by
 * [FullScreenNativeAdPage]) or a banner ad ([FeedItem.BannerAdItem], rendered by
 * [FullScreenBannerAdPage]), decided remotely by [adConfig] - is inserted into the page list via
 * [withAdSlots]. Independently, every 8th reel also gets a centered 300x250 MREC ad page
 * ([FeedItem.MediumRectangleAdItem], rendered by [FullScreenMediumRectangleAdPage]) - not gated
 * by [adConfig] at all, see [withAdSlots]'s doc comment. Either way it's a real page the user
 * swipes past exactly like [ReelCard], not an overlay drawn on top of one (that was the old
 * interstitial's behavior, removed from this feed in favor of this).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalReelsPager(
    videos: List<Video>,
    playerManager: VideoPlayerManager,
    insufficientCoins: Boolean,
    nativeAdUnitId: String,
    bannerAdUnitId: String,
    mediumRectangleAdUnitId: String,
    adConfig: AdConfig,
    onLoadMore: () -> Unit,
    onEvent: (FeedEvent) -> Unit
) {
    val items = remember(videos, adConfig) { videos.map(FeedItem::VideoItem).withAdSlots(adConfig) }
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Track the KEY (FeedItem.key - video.id/slotId, stable across list mutations) of the page
    // we are CURRENTLY on to detect successful swipes AWAY - NOT its index. [items] is rebuilt
    // one entry shorter every time FeedViewModel.applyWatchedFilter removes a just-watched video
    // from the videos this pager was given, which shifts every later item's index down by one.
    // VerticalPager's own `key = { index -> items[index].key }` param then re-resolves
    // pagerState.settledPage to wherever the still-on-screen item's key now sits - a SECOND
    // settledPage change this effect observes for a single physical swipe. Comparing by key
    // instead of index makes that reflow a no-op (the on-screen item's key didn't change, only
    // its index did) instead of misreading it as a swipe away from whatever item now occupies
    // the stale index - which is exactly what was causing a single swipe to charge two coins
    // for two different videos.
    var lastSettledKey by remember { mutableStateOf<String?>(null) }

    // Playback control and swipe detection.
    LaunchedEffect(pagerState.settledPage, insufficientCoins) {
        if (items.isNotEmpty()) {
            val currentPage = pagerState.settledPage
            val currentItem = items.getOrNull(currentPage)
            when (currentItem) {
                is FeedItem.VideoItem -> {
                    if (insufficientCoins) {
                        playerManager.pause(currentItem.video.id)
                    } else {
                        playerManager.play(currentItem.video.id)
                    }
                }
                is FeedItem.NativeAdItem, is FeedItem.BannerAdItem, is FeedItem.MediumRectangleAdItem, null -> {
                    // No video on this page (an ad slot, or an out-of-range index) - make sure
                    // nothing is left playing under it, same as the insufficientCoins pause
                    // above did for the old overlay case.
                    playerManager.pauseAll()
                }
                is FeedItem.StoryItem -> {
                    // Unreachable: this pager only ever builds FeedItem.VideoItem content via
                    // videos.map(FeedItem::VideoItem).withAdSlots(adConfig) above. Grouped with
                    // the ad-slot branch rather than throwing here, since this runs inside a
                    // live LaunchedEffect, not a render path.
                    playerManager.pauseAll()
                }
            }

            val currentKey = currentItem?.key

            // TEMP DIAGNOSTIC (CoinDebug) - logs every run of this effect, not just when it
            // actually fires ReelSwiped, so a run where the key-change guard fails is visible too.
            Log.d(
                "CoinDebug",
                "[Video] pager effect run: settledPage=$currentPage currentKey=$currentKey " +
                    "lastSettledKey=$lastSettledKey"
            )

            // If we settled on a page with a DIFFERENT key, the user successfully swiped AWAY
            // from the last one - see lastSettledKey's doc comment for why this is keyed rather
            // than indexed. Coins are only consumed for swiping away from an actual reel -
            // swiping past a native ad page doesn't fire FeedEvent.ReelSwiped at all, and the
            // very first settle (lastSettledKey still null) never charges anything either.
            if (currentKey != null && currentKey != lastSettledKey) {
                val previousKey = lastSettledKey
                if (previousKey != null) {
                    val previousItem = items.firstOrNull { it.key == previousKey }
                    if (previousItem is FeedItem.VideoItem) {
                        onEvent(FeedEvent.ReelSwiped(previousItem.video.id))
                    }
                }
                lastSettledKey = currentKey
            }
        }
    }

    // Pagination
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= items.size - PlayerConstants.RECENT_PAGES_THRESHOLD && items.isNotEmpty()) {
            onLoadMore()
        }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
    } else {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !insufficientCoins,
            key = { index -> items[index].key }
        ) { page ->
            when (val item = items[page]) {
                is FeedItem.VideoItem -> {
                    ReelCard(
                        video = item.video,
                        isTabSelected = true,
                        playerManager = playerManager,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.NativeAdItem -> {
                    FullScreenNativeAdPage(
                        adUnitId = nativeAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.BannerAdItem -> {
                    FullScreenBannerAdPage(
                        adUnitId = bannerAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.MediumRectangleAdItem -> {
                    FullScreenMediumRectangleAdPage(
                        adUnitId = mediumRectangleAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.StoryItem -> {
                    // Unreachable: this pager only ever builds FeedItem.VideoItem content via
                    // videos.map(FeedItem::VideoItem).withAdSlots(adConfig) above.
                    error("VerticalReelsPager received a StoryItem - should be unreachable")
                }
            }
        }
    }
}
