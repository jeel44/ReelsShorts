package reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        observeRewardsData()
        initializeRewards()
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

    fun onClaimDailyReward() {
        viewModelScope.launch {
            repository.claimDailyReward()
        }
    }

    fun onToggleHistory(show: Boolean) {
        _uiState.update { it.copy(showHistory = show) }
    }
}
