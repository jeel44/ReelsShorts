package reelsdrama.freedrama.videosdrama.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel
import reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReelsFeedScreen(
        uiState = uiState,
        playerManager = viewModel.playerManager,
        onEvent = viewModel::onEvent,
        onSettingsClick = onSettingsClick
    )
}
