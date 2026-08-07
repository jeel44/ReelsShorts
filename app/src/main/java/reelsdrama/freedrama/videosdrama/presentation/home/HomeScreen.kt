package reelsdrama.freedrama.videosdrama.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.presentation.home.components.HomeReelCard
import reelsdrama.freedrama.videosdrama.presentation.home.state.HomeEvent
import reelsdrama.freedrama.videosdrama.presentation.home.state.HomeUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("Following", "For You")
    val horizontalPagerState = rememberPagerState(initialPage = uiState.selectedTabIndex) { tabs.size }

    LaunchedEffect(uiState.selectedTabIndex) {
        if (horizontalPagerState.currentPage != uiState.selectedTabIndex) {
            horizontalPagerState.animateScrollToPage(uiState.selectedTabIndex)
        }
    }

    LaunchedEffect(horizontalPagerState.currentPage) {
        onEvent(HomeEvent.TabSelected(horizontalPagerState.currentPage))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize(),
        ) { tabIndex ->
            val videos = if (tabIndex == 0) uiState.followingVideos else uiState.forYouVideos
            ReelsFeed(
                tabIndex = tabIndex,
                videos = videos,
                onEvent = onEvent,
            )
        }

        HomeTopTabs(
            tabs = tabs,
            selectedTabIndex = horizontalPagerState.currentPage,
            onTabClick = { onEvent(HomeEvent.TabSelected(it)) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReelsFeed(
    tabIndex: Int,
    videos: List<reelsdrama.freedrama.videosdrama.presentation.home.model.Video>,
    onEvent: (HomeEvent) -> Unit,
) {
    val verticalPagerState = rememberPagerState { videos.size }

    LaunchedEffect(verticalPagerState.currentPage, videos.size) {
        if (videos.isNotEmpty()) {
            onEvent(HomeEvent.ReelChanged(tabIndex, verticalPagerState.currentPage))
        }
    }

    if (videos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos yet", color = MaterialTheme.colorScheme.onBackground)
        }
    } else {
        VerticalPager(
            state = verticalPagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val video = videos[page]
            HomeReelCard(
                video = video,
                onLikeClick = { onEvent(HomeEvent.LikeClicked(video.id)) },
                onCommentClick = { onEvent(HomeEvent.CommentClicked(video.id)) },
                onShareClick = { onEvent(HomeEvent.ShareClicked(video.id)) },
                onGiftClick = { onEvent(HomeEvent.GiftClicked(video.id)) },
                onCoinsClick = { onEvent(HomeEvent.CoinsClicked(video.id)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun HomeTopTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.20f), shape = MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, title ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabClick(index) },
            ) {
                Text(
                    text = title,
                    color = if (index == selectedTabIndex) Color.White else Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (index == selectedTabIndex) FontWeight.Bold else FontWeight.Medium,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = if (index == selectedTabIndex) Color.White else Color.Transparent,
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 16.dp, vertical = 1.5.dp),
                )
            }
        }
    }
}
