package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.app.Activity
import android.util.Log
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
import reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story
import javax.inject.Inject

/**
 * ViewModel for the Stories feed - the text-based sibling of [FeedViewModel], selected via
 * [ReelsFeedScreen]'s tab switcher. Mirrors [FeedViewModel]'s ad-config/coin-gate/rewarded-ad
 * plumbing method-for-method (same [AdConfigRepository]/[RewardsRepository] flows, same
 * [reelsdrama.freedrama.videosdrama.domain.model.AdConfig]-driven ad slots via
 * [FeedItem.withAdSlots]) - see [FeedViewModel]'s per-method docs for anything not re-explained
 * here. Only the content source ([FakeStoryRepository] instead of [FakeFeedRepository]) and the
 * lack of a `categoryId` (Stories has no single-category view) actually differ.
 *
 * [NativeAdManager]/[RewardedAdManager]'s `preload` calls below are safe to also fire from here
 * even though [FeedViewModel] already preloads the same ad unit IDs - both managers' `preload`
 * is a documented no-op once a load is already in flight or cached, so this can't double-load or
 * clobber whichever page is actually showing that ad slot at swipe time.
 */
@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: FakeStoryRepository,
    private val rewardsRepository: RewardsRepository,
    private val adConfigRepository: AdConfigRepository,
    private val adInitializer: AdInitializer,
    private val rewardedAdManager: RewardedAdManager,
    private val nativeAdManager: NativeAdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = NetworkConstants.PAGE_SIZE
    private var allFetchedStories = mutableListOf<Story>()

    // In-memory set to prevent duplicate processing during the active session
    private val processingStoryIds = mutableSetOf<String>()

    init {
        observeRewardsData()
        loadInitialStories()
        observeRewardedAdAvailability()
        preloadNativeAdWhenReady()
        observeAdConfig()
    }

    /** See [FeedViewModel.observeAdConfig]'s doc comment - same rationale, mirrored into [StoriesUiState.adConfig]. */
    private fun observeAdConfig() {
        adConfigRepository.getAdConfig()
            .onEach { adConfig -> _uiState.update { it.copy(adConfig = adConfig) } }
            .launchIn(viewModelScope)
    }

    private fun observeRewardsData() {
        // 1. Observe virtual coin balance - the same global economy FeedViewModel observes.
        rewardsRepository.getCoinBalance()
            .onEach { balance ->
                val insufficientCoins = balance <= 0
                _uiState.update {
                    it.copy(
                        coinBalance = balance,
                        insufficientCoins = insufficientCoins,
                        showAdConfirmation = insufficientCoins
                    )
                }
            }
            .launchIn(viewModelScope)

        // 2. Observe watched ids for metadata. Reuses the same RewardsRepository set the video
        // feed uses (there's no story-specific watched-id tracking) - safe since generated story
        // ids ("story_N") never collide with generated video ids. Re-derives StoriesUiState.stories
        // from allFetchedStories every time this set changes, not just watchedStoryIds alone - see
        // applyWatchedFilter's doc comment for why this has to be reactive rather than a one-time
        // fetch-time filter.
        rewardsRepository.getWatchedReelIds()
            .onEach { watchedIds -> applyWatchedFilter(watchedIds) }
            .launchIn(viewModelScope)
    }

    /**
     * See [FeedViewModel.applyWatchedFilter]'s doc comment - same rationale, mirrored for
     * [StoriesUiState.stories]/[StoriesUiState.watchedStoryIds]. [VerticalStoriesPager] is
     * likewise fully disposed and recomposed - restarting at page 0 - every time the Stories tab
     * is left and re-entered, while this [StoriesViewModel] itself survives that switch.
     */
    private fun applyWatchedFilter(watchedIds: Set<String>) {
        _uiState.update {
            it.copy(
                stories = allFetchedStories.filter { story -> story.id !in watchedIds },
                watchedStoryIds = watchedIds
            )
        }
    }

    /** See [FeedViewModel.observeRewardedAdAvailability]'s doc comment - same rationale. */
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

    /** See [FeedViewModel.preloadNativeAdWhenReady]'s doc comment - same rationale. */
    private fun preloadNativeAdWhenReady() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            nativeAdManager.preload(AdConstants.NATIVE_FEED_UNIT_ID)
        }
    }

    private fun loadInitialStories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchMoreUntilUnseenFound()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: StoriesEvent) {
        when (event) {
            is StoriesEvent.LoadMoreStories -> {
                loadMore()
            }
            is StoriesEvent.StorySwiped -> {
                val storyId = event.fromStoryId
                val isProcessing = processingStoryIds.contains(storyId)
                val isWatched = _uiState.value.watchedStoryIds.contains(storyId)

                // TEMP DIAGNOSTIC (CoinDebug)
                Log.d(
                    "CoinDebug",
                    "[Stories] StorySwiped received: storyId=$storyId isProcessing=$isProcessing " +
                        "isWatched=$isWatched guardPassed=${!isProcessing && !isWatched}"
                )

                // Every successful swipeaway = -1 coin, same rule as the video feed.
                if (!isProcessing && !isWatched) {
                    processingStoryIds.add(storyId)
                    viewModelScope.launch {
                        val success = rewardsRepository.consumeCoinForReel(storyId)
                        if (!success) {
                            _uiState.update { it.copy(insufficientCoins = true) }
                        }
                        processingStoryIds.remove(storyId)
                    }
                }
            }
            is StoriesEvent.ToggleAdConfirmation -> {
                _uiState.update { it.copy(showAdConfirmation = event.show) }
            }
            is StoriesEvent.WatchRewardedAd -> {
                watchRewardedAd(event.activity)
            }
            is StoriesEvent.ConsumeRewardedAdFeedback -> {
                _uiState.update { it.copy(rewardedAdFeedback = null) }
            }
            is StoriesEvent.ScreenResumed -> {
                recheckCoinBalance()
            }
        }
    }

    /** See [FeedViewModel.recheckCoinBalance]'s doc comment - same rationale. */
    private fun recheckCoinBalance() {
        viewModelScope.launch {
            val balance = rewardsRepository.getCoinBalance().first()
            val insufficientCoins = balance <= 0
            _uiState.update {
                it.copy(insufficientCoins = insufficientCoins, showAdConfirmation = insufficientCoins)
            }
        }
    }

    /** See [FeedViewModel.watchRewardedAd]'s doc comment - same rationale. */
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
                            description = "Watched a rewarded ad to unlock more stories"
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
            val fetched = repository.getForYouStories(currentPage, pageSize)

            if (fetched.isEmpty()) break

            val newUnseen = fetched.filter { it.id !in watchedIds && !allFetchedStories.any { s -> s.id == it.id } }

            allFetchedStories.addAll(fetched.filter { existing -> !allFetchedStories.any { it.id == existing.id } })
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
}
