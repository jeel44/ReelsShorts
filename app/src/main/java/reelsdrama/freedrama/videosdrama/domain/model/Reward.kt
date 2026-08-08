package reelsdrama.freedrama.videosdrama.domain.model

import kotlinx.serialization.Serializable

/**
 * Data model for a daily reward item in the 7-day timeline.
 */
data class DailyReward(
    val day: Int,
    val amount: Int,
    val isClaimed: Boolean,
    val isToday: Boolean = false,
    val isSpecial: Boolean = false // Day 7
)

/**
 * Data model for a coin activity record.
 */
@Serializable
data class RewardActivity(
    val id: String,
    val timestamp: Long,
    val type: String, // e.g., "Daily Claim", "Watch Video"
    val amount: Int,
    val description: String
)
