package reelsdrama.freedrama.videosdrama.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedTabViewModel
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen
import reelsdrama.freedrama.videosdrama.presentation.home.feed.StoriesViewModel
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager

@Composable
fun HomeRoute(
    onCoinClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    storiesViewModel: StoriesViewModel = hiltViewModel(),
    feedTabViewModel: FeedTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storiesUiState by storiesViewModel.uiState.collectAsStateWithLifecycle()
    val feedTabUiState by feedTabViewModel.uiState.collectAsStateWithLifecycle()
    ReelsFeedScreen(
        uiState = uiState,
        storiesUiState = storiesUiState,
        feedTabUiState = feedTabUiState,
        playerManager = viewModel.playerManager,
        onEvent = viewModel::onEvent,
        onStoriesEvent = storiesViewModel::onEvent,
        onTabSelected = feedTabViewModel::onTabSelected,
        onCoinClick = onCoinClick
    )
}
