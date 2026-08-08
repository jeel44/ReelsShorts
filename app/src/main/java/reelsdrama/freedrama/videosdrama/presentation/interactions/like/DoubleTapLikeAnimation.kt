package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants

/**
 * Animated heart overlay that appears on double tap.
 * Follows TikTok/Instagram style with scale, alpha, and rotation animations.
 */
@Composable
fun DoubleTapLikeAnimation(
    info: HeartAnimationInfo,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Heart should scale: 0.0f -> 1.3f -> 1.0f -> fade out
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(info.id) {
        // Parallel animations for ~650ms total
        launch {
            // Scale: 0.0 -> 1.3 -> 1.0
            scale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        launch {
            // Alpha: 0 -> 1 -> 0
            alpha.animateTo(1f, tween(150))
            delay(300)
            alpha.animateTo(0f, tween(200))
        }

        launch {
            // Rotation: 0 -> 8 -> 0
            rotation.animateTo(8f, tween(200))
            rotation.animateTo(0f, tween(400))
        }

        delay(AnimationConstants.LIKE_ANIM_DURATION_MS)
        onAnimationEnd()
    }

    val density = LocalDensity.current
    // Center the 110dp heart at tap position
    val offsetX = with(density) { info.offset.x.toDp() - 55.dp }
    val offsetY = with(density) { info.offset.y.toDp() - 55.dp }

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.alpha = alpha.value
                this.rotationZ = rotation.value
            }
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.98f),
            modifier = Modifier.size(110.dp)
        )
    }
}
