package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.NativeAdManager
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
    private val nativeAdManager: NativeAdManager,
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
        observeRewardedAdAvailability()
        preloadNativeAdWhenReady()
        observeAdInitialization()
    }

    /**
     * Mirrors [AdInitializer.isInitialized] into [FeedUiState.isAdInitialized] so
     * [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView] (the feed's
     * bottom banner) can gate its load using this ViewModel's existing [AdInitializer]
     * access, instead of the standalone `AdBannerViewModel` bridge built for screens that
     * don't have a ViewModel of their own.
     */
    private fun observeAdInitialization() {
        adInitializer.isInitialized
            .onEach { initialized -> _uiState.update { it.copy(isAdInitialized = initialized) } }
            .launchIn(viewModelScope)
    }

    private fun observeRewardsData() {
        // 1. Observe virtual coin balance
        rewardsRepository.getCoinBalance()
            .onEach { balance ->
                val insufficientCoins = balance <= 0
                _uiState.update {
                    it.copy(
                        coinBalance = balance,
                        insufficientCoins = insufficientCoins,
                        // Mirrors insufficientCoins directly - deliberately no edge-gating
                        // here. This used to only flip to true on a fresh false->true
                        // transition (and otherwise preserve whatever showAdConfirmation
                        // already was, so a user's Cancel stuck once genuinely broke), which
                        // meant re-emissions of this Flow while still at 0 - whether from
                        // another failed swipe attempt via consumeCoinForReel's edit{}-always-
                        // commits behavior, or any other unrelated write - never reopened it
                        // after that first Cancel. The dialog is meant to reopen every distinct
                        // time the balance is observed at 0, not just the first, so it now just
                        // tracks insufficientCoins directly; once coins are gained it still
                        // always closes.
                        showAdConfirmation = insufficientCoins
                    )
                }
            }
            .launchIn(viewModelScope)

        // 2. Observe watched reels - re-derives FeedUiState.videos from allFetchedVideos every
        // time this set changes, not just watchedVideoIds alone. See applyWatchedFilter's doc
        // comment for why this has to be reactive rather than a one-time fetch-time filter.
        rewardsRepository.getWatchedReelIds()
            .onEach { watchedIds -> applyWatchedFilter(watchedIds) }
            .launchIn(viewModelScope)
    }

    /**
     * Re-derives [FeedUiState.videos] (and [FeedUiState.watchedVideoIds]) from [allFetchedVideos]
     * filtered against the CURRENT [watchedIds] - called every time
     * [RewardsRepository.getWatchedReelIds] emits (see [observeRewardsData]), and again after
     * every fetch in [fetchMoreUntilUnseenFound]. [FeedUiState.videos] must never be built from a
     * one-time snapshot alone: [FeedEvent.ReelSwiped] can mark a video watched at any moment
     * outside of a fetch (no new fetch happens just because the user swiped past something), and
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager] is
     * fully disposed and recomposed from scratch - restarting at page 0 - every time
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen]'s tab switcher
     * leaves and re-enters the Videos tab (this [FeedViewModel] itself survives that switch, only
     * the pager composable doesn't). Without re-deriving [FeedUiState.videos] on every watched-set
     * change, a user re-entering this tab after already swiping through it once would land page 0
     * back on an already-watched video: its swipe-away event still fires correctly, but
     * [onEvent]'s own `isWatched` guard then silently skips the coin deduction.
     */
    private fun applyWatchedFilter(watchedIds: Set<String>) {
        _uiState.update {
            it.copy(
                videos = allFetchedVideos.filter { video -> video.id !in watchedIds },
                watchedVideoIds = watchedIds
            )
        }
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
                val isReady = AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID in readyAdUnitIds
                _uiState.update {
                    it.copy(
                        isRewardedAdReady = isReady,
                        // Only overwritten once an ad is actually loaded and can report its
                        // real RewardItem.amount - see FeedUiState.coinUnlockRewardAmount's doc
                        // comment for why the fallback stays put otherwise, rather than briefly
                        // showing something like "+0" while nothing is loaded yet.
                        coinUnlockRewardAmount = if (isReady) {
                            rewardedAdManager.getRewardAmount(AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID)
                                ?: it.coinUnlockRewardAmount
                        } else {
                            it.coinUnlockRewardAmount
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Preloads the feed's full-screen native ad as soon as MobileAds finishes initializing, so
     * the first ad slot ([FeedItem.withNativeAdSlots] inserts one every 5 reels) already has an
     * ad sitting in [NativeAdManager]'s cache by the time the user swipes to it - see
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.FullScreenNativeAdPage]
     * for where it's actually taken and rendered. Unlike the rewarded ad, this ViewModel never
     * needs to know "is it ready" ahead of time - [NativeAdManager.take] always lines up the
     * next one regardless of outcome, and the ad page itself falls back to a shimmer while
     * nothing is ready yet.
     */
    private fun preloadNativeAdWhenReady() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            nativeAdManager.preload(AdConstants.NATIVE_FEED_UNIT_ID)
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

                // TEMP DIAGNOSTIC (CoinDebug)
                Log.d(
                    "CoinDebug",
                    "[Video] ReelSwiped received: videoId=$videoId isProcessing=$isProcessing " +
                        "isWatched=$isWatched guardPassed=${!isProcessing && !isWatched}"
                )

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
            is FeedEvent.WatchRewardedAd -> {
                watchRewardedAd(event.activity)
            }
            is FeedEvent.ConsumeRewardedAdFeedback -> {
                _uiState.update { it.copy(rewardedAdFeedback = null) }
            }
            is FeedEvent.ScreenResumed -> {
                recheckCoinBalance()
            }
        }
    }

    /**
     * Re-derives [FeedUiState.insufficientCoins]/[FeedUiState.showAdConfirmation] straight from
     * the actual current coin balance, independent of [observeRewardsData]'s ongoing collector.
     *
     * That collector only fires on a genuine new emission from [RewardsRepository.getCoinBalance]
     * - i.e. an actual DataStore write - not just because this screen became visible again. If
     * the user leaves this screen after a zero-balance attempt that granted nothing (a CTA tap
     * whose ad was dismissed/unavailable, or simply navigating away with the dialog already
     * closed) and no balance-changing write happens while they're away (e.g. they bounce off
     * Rewards without earning anything), returning here fires no new emission at all - the
     * balance is still 0 -> 0, nothing wrote to DataStore - so [showAdConfirmation] would stay
     * stuck at whatever it was left at (false) even though the user is still exactly as broke as
     * when they left. FeedViewModel/its uiState survive this round trip untouched (Home is kept
     * alive via popUpTo(saveState = true)/restoreState = true in MainScreen's tab-switch
     * pattern), so nothing else naturally re-evaluates this on the way back either.
     *
     * Called on every [FeedEvent.ScreenResumed] (see
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen]'s
     * LifecycleResumeEffect) so the dialog reliably reopens on each fresh visit while still
     * broke, not just the first - mirroring the coin-balance flow's own derivation above.
     */
    private fun recheckCoinBalance() {
        viewModelScope.launch {
            val balance = rewardsRepository.getCoinBalance().first()
            val insufficientCoins = balance <= 0
            _uiState.update {
                it.copy(insufficientCoins = insufficientCoins, showAdConfirmation = insufficientCoins)
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
                // Routes through the same re-derivation applyWatchedFilter's own reactive
                // collector uses, rather than a one-off state.copy here - see its doc comment.
                // watchedIds itself is untouched: still the one-time snapshot this loop's
                // newUnseen/unseenAdded pagination decision above is built on.
                applyWatchedFilter(watchedIds)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
