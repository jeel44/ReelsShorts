package reelsdrama.freedrama.videosdrama.presentation.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import reelsdrama.freedrama.videosdrama.presentation.home.model.FakeVideoRepository
import reelsdrama.freedrama.videosdrama.presentation.home.state.HomeEvent
import reelsdrama.freedrama.videosdrama.presentation.home.state.HomeUiState
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: FakeVideoRepository,
) : ViewModel() {
    private val selectedTabIndex = MutableStateFlow(1)
    private val currentReelByTab = MutableStateFlow(mapOf(0 to 0, 1 to 0))

    val uiState = combine(
        selectedTabIndex,
        repository.observeFollowingVideos(),
        repository.observeForYouVideos(),
    ) { selectedTab, followingVideos, forYouVideos ->
        HomeUiState(
            selectedTabIndex = selectedTab,
            followingVideos = followingVideos,
            forYouVideos = forYouVideos,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(),
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.TabSelected -> selectedTabIndex.value = event.index
            is HomeEvent.ReelChanged -> currentReelByTab.update { it + (event.tabIndex to event.reelIndex) }
            is HomeEvent.LikeClicked,
            is HomeEvent.CommentClicked,
            is HomeEvent.ShareClicked,
            is HomeEvent.GiftClicked,
            is HomeEvent.CoinsClicked -> Unit
        }
    }
}
