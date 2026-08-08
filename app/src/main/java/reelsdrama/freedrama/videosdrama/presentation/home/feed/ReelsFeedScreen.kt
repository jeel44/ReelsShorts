package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.HomeTopTabs
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager
import kotlinx.coroutines.launch

/**
 * The main container for the Reels Feed.
 * Manages the HorizontalPager for "Following" and "For You" tabs.
 * Also supports a single-category view when categoryId is provided.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsFeedScreen(
    uiState: FeedUiState,
    playerManager: VideoPlayerManager,
    onEvent: (FeedEvent) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val isCategoryView = uiState.categoryId != null
    val tabs = if (isCategoryView) emptyList() else listOf("Following", "For You")
    val horizontalPagerState = rememberPagerState(
        initialPage = if (isCategoryView) 0 else uiState.selectedTabIndex
    ) { if (isCategoryView) 1 else tabs.size }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val adsComingSoon = stringResource(R.string.rewards_ads_coming_soon)

    LaunchedEffect(uiState.selectedTabIndex) {
        if (!isCategoryView && horizontalPagerState.currentPage != uiState.selectedTabIndex) {
            horizontalPagerState.animateScrollToPage(uiState.selectedTabIndex)
        }
    }

    LaunchedEffect(horizontalPagerState.currentPage) {
        if (!isCategoryView) {
            onEvent(FeedEvent.TabSelected(horizontalPagerState.currentPage))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCategoryView
        ) { tabIndex ->
            val videos = when {
                isCategoryView -> uiState.forYouVideos
                tabIndex == 0 -> uiState.followingVideos
                else -> uiState.forYouVideos
            }
            
            VerticalReelsPager(
                videos = videos,
                tabIndex = tabIndex,
                selectedTabIndex = horizontalPagerState.currentPage,
                playerManager = playerManager,
                insufficientCoins = uiState.insufficientCoins,
                onLoadMore = { onEvent(FeedEvent.LoadMoreVideos(tabIndex)) },
                onEvent = onEvent
            )
        }

        if (!isCategoryView) {
            HomeTopTabs(
                tabs = tabs,
                selectedTabIndex = horizontalPagerState.currentPage,
                onTabClick = { onEvent(FeedEvent.TabSelected(it)) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            )

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        } else {
            // Category View Header (Back Button)
            IconButton(
                onClick = { onBackClick?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Out of Coins Overlay
        if (uiState.insufficientCoins) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFFF5C542),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.rewards_out_of_coins),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.rewards_out_of_coins_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(adsComingSoon)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.rewards_watch_ad))
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
