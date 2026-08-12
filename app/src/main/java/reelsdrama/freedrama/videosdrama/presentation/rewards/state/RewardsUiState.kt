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
    // getDailyRewards() guarantees exactly one entry has isToday == true whenever a claim
    // is pending (and wraps back to day 1 once a 7-day cycle completes), so the absence of
    // any pending "today" entry reliably means today's reward has actually been claimed -
    // this is no longer a fallback for an unresolved day, it's the correct signal.
    val isClaimedToday: Boolean = dailyRewards.none { it.isToday }
    val currentStreak: Int = dailyRewards.count { it.isClaimed }
}
