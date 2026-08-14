package reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.InterstitialAdManager
import reelsdrama.freedrama.videosdrama.core.ads.InterstitialAdOutcome
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdManager
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdOutcome
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import reelsdrama.freedrama.videosdrama.presentation.rewards.state.RewardsUiState
import javax.inject.Inject

/**
 * ViewModel for the Rewards screen.
 * Orchestrates the loading and presentation of virtual engagement rewards.
 */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardsRepository,
    private val adInitializer: AdInitializer,
    private val rewardedAdManager: RewardedAdManager,
    private val interstitialAdManager: InterstitialAdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        observeRewardsData()
        initializeRewards()
        observeRewardedAdAvailability()
        preloadBackInterstitialWhenReady()
    }

    private fun initializeRewards() {
        viewModelScope.launch {
            repository.initRewards()
        }
    }

    private fun observeRewardsData() {
        combine(
            repository.getCoinBalance(),
            repository.getDailyRewards(),
            repository.getRewardActivities()
        ) { balance, daily, activities ->
            _uiState.update {
                it.copy(
                    coinBalance = balance,
                    dailyRewards = daily,
                    activities = activities
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Preloads the "Watch & Earn" rewarded ad as soon as MobileAds finishes initializing,
     * and mirrors [RewardedAdManager]'s readiness for that ad unit into
     * [RewardsUiState.isRewardedAdReady] so the card can enable/disable its button.
     *
     * This is a separate ad unit from the feed's coin-gate placement
     * (see [AdConstants.REWARDED_WATCH_AND_EARN_UNIT_ID] vs [AdConstants.REWARDED_COIN_UNLOCK_UNIT_ID]),
     * but goes through the exact same [RewardedAdManager] instance and pattern as
     * `FeedViewModel`.
     */
    private fun observeRewardedAdAvailability() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            rewardedAdManager.preload(AdConstants.REWARDED_WATCH_AND_EARN_UNIT_ID)
        }

        rewardedAdManager.readyAdUnitIds
            .onEach { readyAdUnitIds ->
                _uiState.update {
                    it.copy(isRewardedAdReady = AdConstants.REWARDED_WATCH_AND_EARN_UNIT_ID in readyAdUnitIds)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onClaimDailyReward() {
        viewModelScope.launch {
            repository.claimDailyReward()
        }
    }

    /**
     * Preloads the interstitial shown whenever the user leaves Rewards (see [onExitRewards]),
     * as soon as MobileAds finishes initializing - same wait-for-init-then-preload timing
     * [observeRewardedAdAvailability] already uses for the "Watch & Earn" rewarded ad, just for
     * [InterstitialAdManager]/[AdConstants.INTERSTITIAL_FEED_UNIT_ID] instead. Started here in
     * [init], not at the moment of exit itself - a cold load takes real time, and however long
     * the user actually spends on this screen is exactly the window to spend it in.
     */
    private fun preloadBackInterstitialWhenReady() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            interstitialAdManager.preload(AdConstants.INTERSTITIAL_FEED_UNIT_ID)
        }
    }

    /** Guards [onExitRewards] against firing twice concurrently - see its doc comment. */
    private var isExitingRewards = false

    /**
     * Called for every way the user can leave Rewards - see
     * [reelsdrama.freedrama.videosdrama.presentation.rewards.RewardsScreen]'s `BackHandler`
     * (system back gesture/button; this screen has no top-bar back icon of its own) AND
     * `MainScreen`'s bottom-nav "Home" tap handler (a direct `navigate()` call that's a
     * genuinely separate code path from the back gesture, not routed through it) - both funnel
     * through this single function rather than duplicating the ad-then-navigate sequence, so
     * there's exactly one place that decides what happens on the way out of this screen.
     *
     * Shows the preloaded interstitial if [activity] is available and one is ready; either way,
     * [onNavigateAway] fires exactly once - only after the ad is confirmed dismissed, or the
     * attempt is confirmed to have nothing to show ([InterstitialAdOutcome.NotAvailable], e.g.
     * still mid-load) - never while it's still on screen, and never blocked waiting on a load
     * that hasn't finished yet. Whichever way this resolves, [InterstitialAdManager.show]
     * always lines up a fresh interstitial for next time, same as every other placement using
     * this manager.
     *
     * [isExitingRewards] makes this a no-op while a previous call is still in flight - both
     * call sites above are independent Compose UI callbacks (one from a `BackHandler`, one from
     * a bottom-nav click), so nothing else stops a user from triggering both in the same
     * instant (e.g. a back gesture landing the same frame as a "Home" tap); without this guard
     * that would call [InterstitialAdManager.show] twice back to back, and the SDK only has one
     * ad loaded to hand out.
     */
    fun onExitRewards(activity: Activity?, onNavigateAway: () -> Unit) {
        if (isExitingRewards) return
        isExitingRewards = true

        fun finish() {
            isExitingRewards = false
            onNavigateAway()
        }

        if (activity == null) {
            // No Activity in this composition (shouldn't normally happen) - don't block the
            // user's exit waiting for something that can never be shown.
            finish()
            return
        }

        interstitialAdManager.show(
            activity = activity,
            adUnitId = AdConstants.INTERSTITIAL_FEED_UNIT_ID
        ) { outcome ->
            when (outcome) {
                InterstitialAdOutcome.Shown -> Unit
                InterstitialAdOutcome.Dismissed,
                InterstitialAdOutcome.NotAvailable -> finish()
            }
        }
    }

    fun onToggleHistory(show: Boolean) {
        _uiState.update { it.copy(showHistory = show) }
    }

    /**
     * Shows the "Watch & Earn" rewarded ad. Only [RewardedAdOutcome.Earned] grants coins -
     * a dismissed or failed-to-show ad grants nothing, it just surfaces feedback via
     * [RewardsUiState.rewardedAdFeedback].
     */
    fun onWatchRewardedAd(activity: Activity) {
        rewardedAdManager.show(
            activity = activity,
            adUnitId = AdConstants.REWARDED_WATCH_AND_EARN_UNIT_ID
        ) { outcome ->
            Log.d(TAG, "onWatchRewardedAd outcome=$outcome")
            when (outcome) {
                is RewardedAdOutcome.Earned -> {
                    val amount = outcome.reward.amount
                    Log.d(TAG, "onWatchRewardedAd: Earned amount=$amount, calling addActivity")
                    viewModelScope.launch {
                        repository.addActivity(
                            type = "Rewarded Ad",
                            amount = amount,
                            description = "Watched a rewarded ad to earn coins"
                        )
                        Log.d(TAG, "onWatchRewardedAd: addActivity() completed, new balance=${repository.getCoinBalance().first()}")
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

    /** UI has shown [RewardsUiState.rewardedAdFeedback] - clear it so it doesn't repeat. */
    fun onConsumeRewardedAdFeedback() {
        _uiState.update { it.copy(rewardedAdFeedback = null) }
    }

    private companion object {
        const val TAG = "AdDebug"
    }
}
