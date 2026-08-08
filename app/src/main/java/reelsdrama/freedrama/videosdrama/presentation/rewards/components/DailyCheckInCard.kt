package reelsdrama.freedrama.videosdrama.presentation.rewards.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.domain.model.DailyReward

/**
 * A specialized card for daily check-ins with a timeline visualization.
 */
@Composable
fun DailyCheckInCard(
    rewards: List<DailyReward>,
    isClaimedToday: Boolean,
    currentStreak: Int,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Check-in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Day $currentStreak / 7",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rewards.forEachIndexed { index, reward ->
                    TimelineItem(
                        reward = reward,
                        showLine = index < rewards.size - 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Button(
                onClick = onClaimClick,
                enabled = !isClaimedToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isClaimedToday) "Claimed Today" else "Claim Today's Reward",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RowScope.TimelineItem(
    reward: DailyReward,
    showLine: Boolean
) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
        // Line
        if (showLine) {
            Box(
                modifier = Modifier
                    .padding(top = 15.dp) // Align with circle center
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 15.dp) // Half of circle size
                    .background(
                        if (reward.isClaimed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Circle
            val circleColor = when {
                reward.isClaimed -> Color(0xFF4CAF50)
                reward.isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .border(
                        width = if (reward.isToday) 4.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (reward.isClaimed) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else if (reward.isSpecial) {
                    Icon(Icons.Default.CardGiftcard, null, tint = if (reward.isToday) Color.White else Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Day ${reward.day}",
                style = MaterialTheme.typography.labelSmall,
                color = if (reward.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "+${reward.amount}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (reward.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
