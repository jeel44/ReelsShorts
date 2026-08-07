package reelsdrama.freedrama.videosdrama.presentation.home.state

import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

data class HomeUiState(
    val selectedTabIndex: Int = 1,
    val followingVideos: List<Video> = emptyList(),
    val forYouVideos: List<Video> = emptyList(),
)
