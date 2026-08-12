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
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * Vertical pager for reels. Handles playback orchestration and pagination.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalReelsPager(
    videos: List<Video>,
    playerManager: VideoPlayerManager,
    insufficientCoins: Boolean,
    showInterstitial: Boolean,
    onLoadMore: () -> Unit,
    onEvent: (FeedEvent) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })

    // Track the page we are CURRENTLY on to detect successful swipes AWAY.
    var lastSettledPage by remember { mutableIntStateOf(0) }

    // Playback control and swipe detection. showInterstitial is included as a key (and in
    // the pause condition below) so a video is paused for the ENTIRE time an interstitial is
    // pending/on screen and automatically resumes the instant it's cleared - the exact same
    // mechanism already used for the insufficientCoins ("out of coins") overlay, just
    // extended to a second reason to pause.
    LaunchedEffect(pagerState.settledPage, insufficientCoins, showInterstitial) {
        if (videos.isNotEmpty()) {
            val currentPage = pagerState.settledPage
            val video = if (currentPage < videos.size) videos[currentPage] else null

            if (video != null) {
                if (insufficientCoins || showInterstitial) {
                    playerManager.pause(video.id)
                } else {
                    playerManager.play(video.id)
                }

                // If we settled on a NEW page, it means the user successfully swiped AWAY from the last one
                if (currentPage != lastSettledPage) {
                    if (lastSettledPage >= 0 && lastSettledPage < videos.size) {
                        val swipedFromVideo = videos[lastSettledPage]
                        onEvent(FeedEvent.ReelSwiped(swipedFromVideo.id))
                    }
                    lastSettledPage = currentPage
                }
            }
        }
    }

    // Pagination
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= videos.size - PlayerConstants.RECENT_PAGES_THRESHOLD && videos.isNotEmpty()) {
            onLoadMore()
        }
    }

    if (videos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
    } else {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !insufficientCoins,
            key = { index -> if (index < videos.size) videos[index].id else index }
        ) { page ->
            val video = videos[page]
            ReelCard(
                video = video,
                isTabSelected = true,
                playerManager = playerManager,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
