package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onLoadMore: () -> Unit,
    onEvent: (FeedEvent) -> Unit
) {
    val pagerState = rememberPagerState { videos.size }

    LaunchedEffect(pagerState.currentPage, insufficientCoins) {
        if (videos.isNotEmpty()) {
            val video = videos[pagerState.currentPage]
            if (insufficientCoins) {
                playerManager.pause(video.id)
            } else {
                playerManager.play(video.id)
            }
        }
    }

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
            userScrollEnabled = !insufficientCoins
        ) { page ->
            val video = videos[page]
            ReelCard(
                video = video,
                isTabSelected = true, // Always active now
                playerManager = playerManager,
                onViewComplete = { onEvent(FeedEvent.VideoViewed(video.id)) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
