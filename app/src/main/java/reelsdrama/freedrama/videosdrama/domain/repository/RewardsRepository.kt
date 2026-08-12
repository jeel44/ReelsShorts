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
    fun getWatchedReelIds(): Flow<Set<String>>
    
    suspend fun initRewards()

    /**
     * True if [initRewards] has never completed a fresh-install grant on this device -
     * i.e. this is the user's very first launch ever. Must be read BEFORE calling
     * [initRewards] in the same launch, since [initRewards] is what flips this to false
     * (via the same `welcome_bonus_granted` preference it already tracks).
     */
    suspend fun isFirstLaunch(): Boolean

    suspend fun claimDailyReward(): Result<Unit>
    suspend fun addActivity(type: String, amount: Int, description: String)
    suspend fun consumeCoinForReel(videoId: String): Boolean
    suspend fun markReelAsWatched(videoId: String)
}
