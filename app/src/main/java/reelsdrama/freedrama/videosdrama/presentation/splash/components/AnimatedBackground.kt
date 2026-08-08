package reelsdrama.freedrama.videosdrama.presentation.splash.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val breathingAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stars"
    )

    val particles = remember { List(80) { Particle() } }

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // Nebula / Breathing Glow
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF365BFF).copy(alpha = 0.15f * breathingAnim),
                0.5f to Color(0xFF6A2DFF).copy(alpha = 0.1f * breathingAnim),
                1.0f to Color.Transparent,
                center = center
            ),
            radius = size.maxDimension,
            center = center
        )

        // Stars & Particles
        particles.forEach { p ->
            drawCircle(
                color = Color.White.copy(alpha = if (p.isStar) p.alpha * starAlpha else p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

class Particle(
    val x: Float = Random.nextFloat(),
    val y: Float = Random.nextFloat(),
    val size: Float = Random.nextFloat() * 1.5f + 0.5f,
    val alpha: Float = Random.nextFloat() * 0.5f + 0.1f,
    val isStar: Boolean = Random.nextBoolean()
)
