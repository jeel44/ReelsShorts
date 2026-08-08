package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LikeButton(
    isLiked: Boolean,
    likeCount: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
        animationSpec = tween(durationMillis = 250),
        label = "LikeColor"
    )

    LaunchedEffect(isLiked) {
        // Animation for both like and unlike as requested "Play reverse animation"
        // In Compose, reverse spring usually just means animating back to 1f
        scale.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            if (!isLiked) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // "Light" haptic simulation
            }
            onClick()
        }
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = heartColor,
            modifier = Modifier
                .size(32.dp)
                .scale(scale.value)
        )
        Text(
            text = likeCount,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
