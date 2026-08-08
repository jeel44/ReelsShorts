package reelsdrama.freedrama.videosdrama.domain.repository

import kotlinx.coroutines.flow.Flow
import reelsdrama.freedrama.videosdrama.domain.model.*

/**
 * Repository interface for managing user engagement rewards and coin balance.
 * Coins are virtual in-app points with no monetary value.
 */
interface RewardsRepository {
    fun getCoinBalance(): Flow<Int>
    fun getDailyRewards(): Flow<List<DailyReward>>
    fun getRewardActivities(): Flow<List<RewardActivity>>
    
    suspend fun initRewards()
    suspend fun claimDailyReward(): Result<Unit>
    suspend fun addActivity(type: String, amount: Int, description: String)
    suspend fun consumeCoinForReel(videoId: String): Boolean
}
