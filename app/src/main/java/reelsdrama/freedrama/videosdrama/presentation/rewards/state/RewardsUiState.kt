package reelsdrama.freedrama.videosdrama.presentation.rewards.state

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.domain.model.*

@Immutable
data class RewardsUiState(
    val coinBalance: Int = 0,
    val dailyRewards: List<DailyReward> = emptyList(),
    val activities: List<RewardActivity> = emptyList(),
    val isLoading: Boolean = false,
    val showHistory: Boolean = false,
    val error: String? = null
) {
    val isClaimedToday: Boolean = dailyRewards.any { it.isToday && it.isClaimed } || dailyRewards.none { it.isToday }
    val currentStreak: Int = dailyRewards.count { it.isClaimed }
}
