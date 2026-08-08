package reelsdrama.freedrama.videosdrama.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import reelsdrama.freedrama.videosdrama.domain.model.*
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : RewardsRepository {

    private object PreferencesKeys {
        val COIN_BALANCE = intPreferencesKey("coin_balance")
        val LAST_CLAIM_DATE = longPreferencesKey("last_claim_date")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val ACTIVITIES_JSON = stringPreferencesKey("activities_json")
        val WELCOME_BONUS_GRANTED = booleanPreferencesKey("welcome_bonus_granted")
        val CONSUMED_REELS = stringSetPreferencesKey("consumed_reels")
    }

    private val dailyRewardAmounts = listOf(10, 10, 20, 20, 30, 30, 100)

    override fun getCoinBalance(): Flow<Int> = dataStore.data.map { it[PreferencesKeys.COIN_BALANCE] ?: 0 }

    override fun getDailyRewards(): Flow<List<DailyReward>> = dataStore.data.map { prefs ->
        val lastClaimDate = prefs[PreferencesKeys.LAST_CLAIM_DATE] ?: 0L
        val streakCount = prefs[PreferencesKeys.STREAK_COUNT] ?: 0
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val today = calendar.timeInMillis
        
        val isClaimedToday = lastClaimDate == today
        val yesterday = today - 86400000L
        
        // If streak is broken (more than 1 day since last claim and not claimed today)
        val effectiveStreak = if (!isClaimedToday && lastClaimDate < yesterday) 0 else streakCount

        List(7) { i ->
            val dayNum = i + 1
            DailyReward(
                day = dayNum,
                amount = dailyRewardAmounts[i],
                isClaimed = dayNum <= streakCount && (dayNum < streakCount || isClaimedToday),
                isToday = (dayNum == effectiveStreak + 1) && !isClaimedToday,
                isSpecial = dayNum == 7
            )
        }
    }

    override fun getRewardActivities(): Flow<List<RewardActivity>> = dataStore.data.map { prefs ->
        val json = prefs[PreferencesKeys.ACTIVITIES_JSON] ?: "[]"
        try {
            Json.decodeFromString<List<RewardActivity>>(json).reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun initRewards() {
        dataStore.edit { prefs ->
            // 1. Check if welcome bonus has already been granted
            if (prefs[PreferencesKeys.WELCOME_BONUS_GRANTED] == true) return@edit

            // 2. Audit existing state to protect existing users
            val currentBalance = prefs[PreferencesKeys.COIN_BALANCE] ?: 0
            val activitiesJson = prefs[PreferencesKeys.ACTIVITIES_JSON] ?: "[]"
            val hasExistingActivity = activitiesJson != "[]"

            // If user has existing balance or activity, mark as granted but don't add +5
            if (currentBalance > 0 || hasExistingActivity) {
                prefs[PreferencesKeys.WELCOME_BONUS_GRANTED] = true
                return@edit
            }

            // 3. fresh install flow: Grant Welcome Bonus (+5 Coins)
            prefs[PreferencesKeys.COIN_BALANCE] = 5
            
            val activities = mutableListOf(
                RewardActivity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    type = "Welcome Bonus",
                    amount = 5,
                    description = "Welcome Bonus"
                )
            )
            prefs[PreferencesKeys.ACTIVITIES_JSON] = Json.encodeToString(activities)
            prefs[PreferencesKeys.WELCOME_BONUS_GRANTED] = true
        }
    }

    override suspend fun consumeCoinForReel(videoId: String): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val consumedReels = prefs[PreferencesKeys.CONSUMED_REELS] ?: emptySet()
            if (consumedReels.contains(videoId)) {
                success = true
                return@edit
            }

            val currentBalance = prefs[PreferencesKeys.COIN_BALANCE] ?: 0
            if (currentBalance > 0) {
                // Deduct 1 coin
                prefs[PreferencesKeys.COIN_BALANCE] = currentBalance - 1
                
                // Mark as consumed
                prefs[PreferencesKeys.CONSUMED_REELS] = consumedReels + videoId
                
                // Add activity
                val json = prefs[PreferencesKeys.ACTIVITIES_JSON] ?: "[]"
                val activities = try {
                    Json.decodeFromString<MutableList<RewardActivity>>(json)
                } catch (e: Exception) {
                    mutableListOf()
                }
                activities.add(
                    RewardActivity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        type = "Watch Reel",
                        amount = -1,
                        description = "Watched Reel"
                    )
                )
                prefs[PreferencesKeys.ACTIVITIES_JSON] = Json.encodeToString(activities.takeLast(50))
                success = true
            } else {
                success = false
            }
        }
        return success
    }

    override suspend fun claimDailyReward(): Result<Unit> {
        val prefs = dataStore.data.first()
        val lastClaimDate = prefs[PreferencesKeys.LAST_CLAIM_DATE] ?: 0L
        val streakCount = prefs[PreferencesKeys.STREAK_COUNT] ?: 0
        
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val today = calendar.timeInMillis
        
        if (lastClaimDate == today) return Result.failure(Exception("Already claimed today"))
        
        val effectiveStreak = if (lastClaimDate < today - 86400000L) 0 else streakCount
        val newStreak = if (effectiveStreak >= 7) 1 else effectiveStreak + 1
        
        val amount = dailyRewardAmounts[newStreak - 1]
        
        dataStore.edit {
            it[PreferencesKeys.LAST_CLAIM_DATE] = today
            it[PreferencesKeys.STREAK_COUNT] = newStreak
        }
        
        addActivity("Daily Claim", amount, "Day $newStreak check-in")
        return Result.success(Unit)
    }

    override suspend fun addActivity(type: String, amount: Int, description: String) {
        dataStore.edit { prefs ->
            val currentBalance = prefs[PreferencesKeys.COIN_BALANCE] ?: 0
            val newBalance = (currentBalance + amount).coerceAtLeast(0)
            prefs[PreferencesKeys.COIN_BALANCE] = newBalance
            
            val json = prefs[PreferencesKeys.ACTIVITIES_JSON] ?: "[]"
            val activities = try {
                Json.decodeFromString<MutableList<RewardActivity>>(json)
            } catch (e: Exception) {
                mutableListOf()
            }
            
            activities.add(
                RewardActivity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    amount = amount,
                    description = description
                )
            )
            
            val limited = if (activities.size > 50) activities.takeLast(50) else activities
            prefs[PreferencesKeys.ACTIVITIES_JSON] = Json.encodeToString(limited)
        }
    }
}
