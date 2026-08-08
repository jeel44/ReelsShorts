package reelsdrama.freedrama.videosdrama.presentation.splash.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDots(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    // Animation timing: 1.2s total cycle
    val cycleMillis = 1200
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 64.dp)
    ) {
        Dot(index = 0, phase = phase, baseColor = Color(0xFF8A2BE2), alpha = alpha)
        Dot(index = 1, phase = phase, baseColor = Color(0xFFFF2D7A), alpha = alpha)
        Dot(index = 2, phase = phase, baseColor = Color(0xFF8A2BE2), alpha = alpha)
    }
}

@Composable
private fun Dot(index: Int, phase: Float, baseColor: Color, alpha: Float) {
    // Phase logic: 0..1 (Dot1), 1..2 (Dot2), 2..3 (Dot3), 3..4 (Dot2 back)
    val isActive = when(index) {
        0 -> phase < 1f
        1 -> (phase >= 1f && phase < 2f) || (phase >= 3f)
        2 -> phase >= 2f && phase < 3f
        else -> false
    }

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val currentAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .background(baseColor.copy(alpha = currentAlpha * alpha), CircleShape)
            .then(if (isActive) Modifier.blur(2.dp) else Modifier)
    )
}
