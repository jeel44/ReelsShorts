package reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        observeRewardsData()
        initializeRewards()
        observeRewardedAdAvailability()
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
            when (outcome) {
                is RewardedAdOutcome.Earned -> {
                    val amount = outcome.reward.amount
                    viewModelScope.launch {
                        repository.addActivity(
                            type = "Rewarded Ad",
                            amount = amount,
                            description = "Watched a rewarded ad to earn coins"
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

    /** UI has shown [RewardsUiState.rewardedAdFeedback] - clear it so it doesn't repeat. */
    fun onConsumeRewardedAdFeedback() {
        _uiState.update { it.copy(rewardedAdFeedback = null) }
    }
}
