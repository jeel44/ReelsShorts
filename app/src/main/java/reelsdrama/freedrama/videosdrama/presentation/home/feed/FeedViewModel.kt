package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import reelsdrama.freedrama.videosdrama.core.constants.NetworkConstants
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Reels Feed screen.
 * Manages the playback state, pagination, and UI logic for the vertical video feed.
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FakeFeedRepository,
    private val rewardsRepository: RewardsRepository,
    val playerManager: VideoPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String? = savedStateHandle["categoryId"]

    private val _uiState = MutableStateFlow(FeedUiState(categoryId = categoryId))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = NetworkConstants.PAGE_SIZE

    init {
        loadInitialVideos()
        observeCoinBalance()
    }

    private fun observeCoinBalance() {
        rewardsRepository.getCoinBalance()
            .onEach { balance ->
                _uiState.update { it.copy(
                    coinBalance = balance,
                    insufficientCoins = if (balance > 0) false else it.insufficientCoins
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInitialVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val videos = if (categoryId != null) {
                repository.getCategoryVideos(categoryId, currentPage, pageSize)
            } else {
                repository.getForYouVideos(currentPage, pageSize)
            }

            _uiState.update {
                it.copy(
                    videos = videos,
                    isLoading = false
                )
            }
        }
    }

    private val watchedVideoIds = mutableSetOf<String>()

    fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.LoadMoreVideos -> {
                loadMore()
            }
            is FeedEvent.VideoViewed -> {
                if (!watchedVideoIds.contains(event.videoId)) {
                    watchedVideoIds.add(event.videoId)
                    viewModelScope.launch {
                        val success = rewardsRepository.consumeCoinForReel(event.videoId)
                        if (!success) {
                            _uiState.update { it.copy(insufficientCoins = true) }
                        }
                    }
                }
            }
        }
    }

    private fun loadMore() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            currentPage++
            
            val newVideos = if (categoryId != null) {
                repository.getCategoryVideos(categoryId, currentPage, pageSize)
            } else {
                repository.getForYouVideos(currentPage, pageSize)
            }

            _uiState.update {
                it.copy(
                    videos = it.videos + newVideos,
                    isLoading = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
