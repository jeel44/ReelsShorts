package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import reelsdrama.freedrama.videosdrama.core.constants.NetworkConstants
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video
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
    private var allFetchedVideos = mutableListOf<Video>()

    // In-memory set to prevent duplicate processing during the active session
    private val processingVideoIds = mutableSetOf<String>()

    init {
        observeRewardsData()
        loadInitialVideos()
    }

    private fun observeRewardsData() {
        // 1. Observe virtual coin balance
        rewardsRepository.getCoinBalance()
            .onEach { balance ->
                _uiState.update { it.copy(
                    coinBalance = balance,
                    insufficientCoins = balance <= 0
                ) }
            }
            .launchIn(viewModelScope)

        // 2. Observe watched reels for metadata
        rewardsRepository.getWatchedReelIds()
            .onEach { watchedIds ->
                _uiState.update { it.copy(watchedVideoIds = watchedIds) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInitialVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchMoreUntilUnseenFound()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.LoadMoreVideos -> {
                loadMore()
            }
            is FeedEvent.ReelSwiped -> {
                val videoId = event.fromVideoId
                val isProcessing = processingVideoIds.contains(videoId)
                val isWatched = _uiState.value.watchedVideoIds.contains(videoId)
                
                // Every successful swipeaway = -1 coin. No watch time requirement.
                if (!isProcessing && !isWatched) {
                    processingVideoIds.add(videoId)
                    viewModelScope.launch {
                        val success = rewardsRepository.consumeCoinForReel(videoId)
                        if (!success) {
                            _uiState.update { it.copy(insufficientCoins = true) }
                        }
                        processingVideoIds.remove(videoId)
                    }
                }
            }
            is FeedEvent.ToggleAdConfirmation -> {
                _uiState.update { it.copy(showAdConfirmation = event.show) }
            }
        }
    }

    private fun loadMore() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchMoreUntilUnseenFound()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchMoreUntilUnseenFound() {
        var attempts = 0
        val maxAttempts = 5
        var unseenAdded = 0
        
        val watchedIds = rewardsRepository.getWatchedReelIds().first()

        while (unseenAdded < 5 && attempts < maxAttempts) {
            val fetched = if (categoryId != null) {
                repository.getCategoryVideos(categoryId, currentPage, pageSize)
            } else {
                repository.getForYouVideos(currentPage, pageSize)
            }

            if (fetched.isEmpty()) break

            val newUnseen = fetched.filter { it.id !in watchedIds && !allFetchedVideos.any { v -> v.id == it.id } }
            
            allFetchedVideos.addAll(fetched.filter { existing -> !allFetchedVideos.any { it.id == existing.id } })
            unseenAdded += newUnseen.size
            
            currentPage++
            attempts++
            
            if (newUnseen.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(videos = allFetchedVideos.filter { it.id !in watchedIds })
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
