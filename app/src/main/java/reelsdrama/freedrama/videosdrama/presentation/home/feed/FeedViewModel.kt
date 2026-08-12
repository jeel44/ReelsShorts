package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.InterstitialAdManager
import reelsdrama.freedrama.videosdrama.core.ads.InterstitialAdOutcome
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdManager
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdOutcome
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
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
    private val adInitializer: AdInitializer,
    private val rewardedAdManager: RewardedAdManager,
    private val interstitialAdManager: InterstitialAdManager,
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

    // In-memory only, deliberately: this FeedViewModel instance is scoped to its NavHost
    // back stack entry (Home / a given CategoryReels destination) via Hilt's default
    // hiltViewModel() scoping, and MainScreen's bottom-nav navigation uses
    // popUpTo { saveState = true } + restoreState = true, which preserves that ViewModel
    // (and this counter) across normal tab switches. It only resets on a genuine process
    // restart or if the destination is actually popped off the back stack, which is exactly
    // the desired behavior - no DataStore persistence needed for this.
    private var swipeCount = 0

    init {
        observeRewardsData()
        loadInitialVideos()
        observeRewardedAdAvailability()
        preloadInterstitialAdWhenReady()
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

    /**
     * Preloads the coin-gate rewarded ad as soon as MobileAds finishes initializing, and
     * mirrors [RewardedAdManager]'s readiness for that ad unit into [FeedUiState.isRewardedAdReady]
     * so the confirmation dialog can enable/disable its confirm button accordingly.
     */
    private fun observeRewardedAdAvailability() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            rewardedAdManager.preload(AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID)
        }

        rewardedAdManager.readyAdUnitIds
            .onEach { readyAdUnitIds ->
                _uiState.update {
                    it.copy(isRewardedAdReady = AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID in readyAdUnitIds)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Preloads the feed interstitial as soon as MobileAds finishes initializing. Unlike the
     * rewarded ad, the UI never needs to know "is it ready" ahead of time - the interstitial
     * is triggered opportunistically every 5th swipe and simply skips that cycle via
     * [InterstitialAdOutcome.NotAvailable] if nothing is loaded yet, so there's no readiness
     * state to mirror into [FeedUiState].
     */
    private fun preloadInterstitialAdWhenReady() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            interstitialAdManager.preload(AdConstants.INTERSTITIAL_FEED_UNIT_ID)
        }
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

                maybeTriggerInterstitial()
            }
            is FeedEvent.ToggleAdConfirmation -> {
                _uiState.update { it.copy(showAdConfirmation = event.show) }
            }
            is FeedEvent.WatchRewardedAd -> {
                watchRewardedAd(event.activity)
            }
            is FeedEvent.ConsumeRewardedAdFeedback -> {
                _uiState.update { it.copy(rewardedAdFeedback = null) }
            }
            is FeedEvent.ShowInterstitialAd -> {
                showInterstitialAd(event.activity)
            }
            is FeedEvent.DismissInterstitialTrigger -> {
                _uiState.update { it.copy(showInterstitial = false) }
            }
        }
    }

    /**
     * Counts this swipe and, every [INTERSTITIAL_EVERY_N_SWIPES]th one, flips
     * [FeedUiState.showInterstitial] to true - the UI reacts by pausing playback and asking
     * [showInterstitialAd] to actually display it. Guarded against firing while the coin-gate
     * rewarded-ad flow owns the screen (also structurally impossible today since swiping is
     * disabled whenever [FeedUiState.insufficientCoins] is true - the flow that
     * [FeedUiState.showAdConfirmation] and the rewarded ad both depend on) and against
     * stacking a second trigger on top of one that hasn't resolved yet.
     */
    private fun maybeTriggerInterstitial() {
        swipeCount++
        if (swipeCount % INTERSTITIAL_EVERY_N_SWIPES != 0) return

        val state = _uiState.value
        if (state.insufficientCoins || state.showAdConfirmation || state.showInterstitial) return

        _uiState.update { it.copy(showInterstitial = true) }
    }

    /**
     * Shows the feed interstitial. [InterstitialAdOutcome.Shown] means it's actually on
     * screen - [FeedUiState.showInterstitial] (and therefore playback pausing) stays true
     * until [InterstitialAdOutcome.Dismissed] or [InterstitialAdOutcome.NotAvailable]
     * confirms the attempt is over, so a video is never resumed while the ad is still up.
     */
    private fun showInterstitialAd(activity: Activity) {
        interstitialAdManager.show(
            activity = activity,
            adUnitId = AdConstants.INTERSTITIAL_FEED_UNIT_ID
        ) { outcome ->
            when (outcome) {
                InterstitialAdOutcome.Shown -> Unit
                InterstitialAdOutcome.Dismissed,
                InterstitialAdOutcome.NotAvailable -> {
                    _uiState.update { it.copy(showInterstitial = false) }
                }
            }
        }
    }

    /**
     * Shows the coin-gate rewarded ad. Only [RewardedAdOutcome.Earned] grants coins - a
     * dismissed or failed-to-show ad grants nothing, it just surfaces feedback via
     * [FeedUiState.rewardedAdFeedback].
     */
    private fun watchRewardedAd(activity: Activity) {
        rewardedAdManager.show(
            activity = activity,
            adUnitId = AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID
        ) { outcome ->
            when (outcome) {
                is RewardedAdOutcome.Earned -> {
                    val amount = outcome.reward.amount
                    viewModelScope.launch {
                        rewardsRepository.addActivity(
                            type = "Rewarded Ad",
                            amount = amount,
                            description = "Watched a rewarded ad to unlock more reels"
                        )
                    }
                    _uiState.update { it.copy(rewardedAdFeedback = RewardedAdFeedback.Earned(amount)) }
                }
                RewardedAdOutcome.DismissedWithoutReward -> {
                    _uiState.update { it.copy(rewardedAdFeedback = RewardedAdFeedback.DismissedEarly) }
                }
                RewardedAdOutcome.NotAvailable -> {
                    _uiState.update { it.copy(rewardedAdFeedback = RewardedAdFeedback.NotAvailable) }
                }
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

    private companion object {
        /** Show the feed interstitial every Nth completed swipe, starting from the Nth. */
        const val INTERSTITIAL_EVERY_N_SWIPES = 5
    }
}
