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

    // Track the page we are CURRENTLY on to detect successful swipes AWAY.
    var lastSettledPage by remember { mutableIntStateOf(0) }

    // Playback control and swipe detection.
    LaunchedEffect(pagerState.settledPage, insufficientCoins) {
        if (items.isNotEmpty()) {
            val currentPage = pagerState.settledPage
            when (val item = items.getOrNull(currentPage)) {
                is FeedItem.VideoItem -> {
                    if (insufficientCoins) {
                        playerManager.pause(item.video.id)
                    } else {
                        playerManager.play(item.video.id)
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

            // TEMP DIAGNOSTIC (CoinDebug) - logs every run of this effect, not just when it
            // actually fires ReelSwiped, so a run where the page-change guard fails is visible too.
            val previousItemForLog = items.getOrNull(lastSettledPage)
            Log.d(
                "CoinDebug",
                "[Video] pager effect run: settledPage=$currentPage lastSettledPage=$lastSettledPage " +
                    "previousItem=${previousItemForLog?.javaClass?.simpleName} " +
                    "previousVideoId=${(previousItemForLog as? FeedItem.VideoItem)?.video?.id} " +
                    "isVideoItemCheck=${previousItemForLog is FeedItem.VideoItem}"
            )

            // If we settled on a NEW page, it means the user successfully swiped AWAY from the
            // last one. Coins are only consumed for swiping away from an actual reel - swiping
            // past a native ad page doesn't fire FeedEvent.ReelSwiped at all.
            if (currentPage != lastSettledPage) {
                val previousItem = items.getOrNull(lastSettledPage)
                if (previousItem is FeedItem.VideoItem) {
                    onEvent(FeedEvent.ReelSwiped(previousItem.video.id))
                }
                lastSettledPage = currentPage
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
