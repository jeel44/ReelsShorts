package reelsdrama.freedrama.videosdrama.core.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants

@Composable
fun VideoShimmer(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(AnimationConstants.FADE_DURATION_MS)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val shimmerColors = listOf(
                Color.DarkGray.copy(alpha = 0.6f),
                Color.Gray.copy(alpha = 0.2f),
                Color.DarkGray.copy(alpha = 0.6f),
            )
            val transition = rememberInfiniteTransition(label = "shimmer")
            val translateAnim by transition.animateFloat(
                initialValue = 0f,
                targetValue = 2000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(AnimationConstants.SHIMMER_DURATION, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "translate"
            )
            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset.Zero,
                end = Offset(x = translateAnim, y = translateAnim)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush)
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
        }
    }
}
