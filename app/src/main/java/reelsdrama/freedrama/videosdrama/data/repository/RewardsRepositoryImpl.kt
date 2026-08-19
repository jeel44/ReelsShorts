package reelsdrama.freedrama.videosdrama.data.repository

import android.util.Log
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
        val WATCHED_REELS = stringSetPreferencesKey("watched_reels")
        /**
         * Day-boundary timestamp (see [todayDayBoundary]) of the day [initRewards] actually
         * granted the fresh-install welcome bonus - i.e. install day itself. Absent (defaults
         * to 0L, same as [LAST_CLAIM_DATE]'s default) for any install that predates this key,
         * which deliberately makes the [getDailyRewards]/[claimDailyReward] install-day gate a
         * no-op for those users rather than retroactively locking them out.
         */
        val INSTALL_DATE = longPreferencesKey("install_date")
    }

    private val dailyRewardAmounts = listOf(10, 10, 20, 20, 30, 30, 100)

    /** Today's day-boundary timestamp (local midnight) - the single day-comparison basis
     *  shared by [getDailyRewards], [claimDailyReward] and the install-date stamp written by
     *  [initRewards], so all three agree on what "the same day" means. */
    private fun todayDayBoundary(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    override fun getCoinBalance(): Flow<Int> = dataStore.data.map { it[PreferencesKeys.COIN_BALANCE] ?: 0 }

    override fun getDailyRewards(): Flow<List<DailyReward>> = dataStore.data.map { prefs ->
        val lastClaimDate = prefs[PreferencesKeys.LAST_CLAIM_DATE] ?: 0L
        val streakCount = prefs[PreferencesKeys.STREAK_COUNT] ?: 0
        val installDate = prefs[PreferencesKeys.INSTALL_DATE] ?: 0L
        val today = todayDayBoundary()

        val isClaimedToday = lastClaimDate == today
        val yesterday = today - ONE_DAY_MILLIS

        // If streak is broken (more than 1 day since last claim and not claimed today)
        val effectiveStreak = if (!isClaimedToday && lastClaimDate < yesterday) 0 else streakCount

        // Once a full 7-day cycle is complete (effectiveStreak == 7) and a new day begins
        // without a claim yet, wrap back to day 1 instead of pointing at a non-existent
        // "day 8" - otherwise isToday never matches any of the 7 entries and the claim
        // button looks permanently disabled. This mirrors claimDailyReward()'s own wrap
        // rule (newStreak = if (effectiveStreak >= 7) 1 else effectiveStreak + 1), so the
        // two stay in sync.
        val wrapsToNewCycle = !isClaimedToday && effectiveStreak >= 7
        val cycleClaimedCount = if (wrapsToNewCycle) 0 else effectiveStreak
        val todayDayNum = if (wrapsToNewCycle) 1 else effectiveStreak + 1

        // Day 1 of a fresh streak isn't available until the calendar day AFTER install -
        // install day itself only grants the one-time starter coins (initRewards()), not a
        // streak day. Only reachable when todayDayNum == 1 with nothing claimed yet
        // (effectiveStreak == 0 is what puts todayDayNum at 1 in the first place), so this
        // can't accidentally lock a returning user's actual next day.
        val isFreshStreakLockedOnInstallDay = effectiveStreak == 0 && !isClaimedToday && today <= installDate

        List(7) { i ->
            val dayNum = i + 1
            DailyReward(
                day = dayNum,
                amount = dailyRewardAmounts[i],
                isClaimed = dayNum <= cycleClaimedCount,
                isToday = (dayNum == todayDayNum) && !isClaimedToday && !isFreshStreakLockedOnInstallDay,
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

    override fun getWatchedReelIds(): Flow<Set<String>> = dataStore.data.map { it[PreferencesKeys.WATCHED_REELS] ?: emptySet() }

    override suspend fun isFirstLaunch(): Boolean =
        dataStore.data.first()[PreferencesKeys.WELCOME_BONUS_GRANTED] != true

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

            // 3. fresh install flow: Grant Welcome Bonus (+5 Coins) and record the install day
            // so the daily streak's Day 1 doesn't become claimable until the day after (see
            // getDailyRewards()/claimDailyReward()'s install-day gate). Deliberately only set
            // here, not in the "existing user" branch above - that branch covers a returning
            // user whose real streak history (if any) already stands on its own and shouldn't
            // be retroactively gated by a same-day install lock they didn't actually have.
            prefs[PreferencesKeys.COIN_BALANCE] = 5
            prefs[PreferencesKeys.INSTALL_DATE] = todayDayBoundary()

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
            // TEMP DIAGNOSTIC (CoinDebug) - covers BOTH the story and video paths, since this is
            // the single shared function both StoriesViewModel and FeedViewModel call.
            Log.d(
                "CoinDebug",
                "[consumeCoinForReel] id=$videoId alreadyConsumed=${consumedReels.contains(videoId)}"
            )
            if (consumedReels.contains(videoId)) {
                success = true
                // If it was already consumed, ensure it's marked as watched as well
                val watchedReels = prefs[PreferencesKeys.WATCHED_REELS] ?: emptySet()
                if (!watchedReels.contains(videoId)) {
                    prefs[PreferencesKeys.WATCHED_REELS] = watchedReels + videoId
                }
                return@edit
            }

            val currentBalance = prefs[PreferencesKeys.COIN_BALANCE] ?: 0
            // TEMP DIAGNOSTIC (CoinDebug)
            Log.d("CoinDebug", "[consumeCoinForReel] id=$videoId currentBalance=$currentBalance")
            if (currentBalance > 0) {
                // Deduct 1 coin
                prefs[PreferencesKeys.COIN_BALANCE] = currentBalance - 1
                
                // Mark as consumed
                prefs[PreferencesKeys.CONSUMED_REELS] = consumedReels + videoId
                
                // Mark as watched
                val watchedReels = prefs[PreferencesKeys.WATCHED_REELS] ?: emptySet()
                prefs[PreferencesKeys.WATCHED_REELS] = watchedReels + videoId
                
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
        // TEMP DIAGNOSTIC (CoinDebug)
        Log.d("CoinDebug", "[consumeCoinForReel] id=$videoId finalSuccess=$success")
        return success
    }

    override suspend fun markReelAsWatched(videoId: String) {
        dataStore.edit { prefs ->
            val watchedReels = prefs[PreferencesKeys.WATCHED_REELS] ?: emptySet()
            if (!watchedReels.contains(videoId)) {
                prefs[PreferencesKeys.WATCHED_REELS] = watchedReels + videoId
            }
        }
    }

    override suspend fun claimDailyReward(): Result<Unit> {
        val prefs = dataStore.data.first()
        val lastClaimDate = prefs[PreferencesKeys.LAST_CLAIM_DATE] ?: 0L
        val streakCount = prefs[PreferencesKeys.STREAK_COUNT] ?: 0
        val installDate = prefs[PreferencesKeys.INSTALL_DATE] ?: 0L

        val today = todayDayBoundary()

        if (lastClaimDate == today) return Result.failure(Exception("Already claimed today"))

        val effectiveStreak = if (lastClaimDate < today - ONE_DAY_MILLIS) 0 else streakCount

        // Mirrors getDailyRewards()'s isFreshStreakLockedOnInstallDay gate: a fresh streak's
        // Day 1 can't be claimed on install day itself, only from the day after install
        // onward. Belt-and-suspenders against the claim button somehow firing while the UI
        // still thinks Day 1 is locked (getDailyRewards() already disables it via isToday).
        if (effectiveStreak == 0 && today <= installDate) {
            return Result.failure(Exception("Day 1 isn't available until the day after install"))
        }

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
            Log.d(TAG, "addActivity: type=$type amount=$amount balanceBefore=$currentBalance balanceAfter=$newBalance")
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

    private companion object {
        const val TAG = "AdDebug"
        const val ONE_DAY_MILLIS = 86_400_000L
    }
}
