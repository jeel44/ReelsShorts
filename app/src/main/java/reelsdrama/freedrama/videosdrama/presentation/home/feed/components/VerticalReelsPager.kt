package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

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
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedEvent
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard
import reelsdrama.freedrama.videosdrama.presentation.home.feed.withNativeAdSlots
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * Vertical pager for reels. Handles playback orchestration and pagination.
 *
 * Every 5th reel, a full-screen native ad ([FeedItem.NativeAdItem], rendered by
 * [FullScreenNativeAdPage]) is inserted into the page list via [withNativeAdSlots] - it's a
 * real page the user swipes past exactly like [ReelCard], not an overlay drawn on top of one
 * (that was the old interstitial's behavior, removed from this feed in favor of this).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalReelsPager(
    videos: List<Video>,
    playerManager: VideoPlayerManager,
    insufficientCoins: Boolean,
    nativeAdUnitId: String,
    onLoadMore: () -> Unit,
    onEvent: (FeedEvent) -> Unit
) {
    val items = remember(videos) { videos.withNativeAdSlots() }
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
                is FeedItem.NativeAdItem, null -> {
                    // No video on this page (a native ad slot, or an out-of-range index) -
                    // make sure nothing is left playing under it, same as the insufficientCoins
                    // pause above did for the old overlay case.
                    playerManager.pauseAll()
                }
            }

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
            }
        }
    }
}
