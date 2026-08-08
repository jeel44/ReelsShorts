package reelsdrama.freedrama.videosdrama.presentation.splash.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedLogo(
    scale: Float,
    alpha: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        // Logo Glow
        Canvas(modifier = Modifier.size(220.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFF6A2DFF).copy(alpha = 0.3f * pulse * alpha),
                    1.0f to Color.Transparent
                ),
                radius = size.width / 2
            )
        }

        // The 3D 'D' Logo
        Canvas(modifier = Modifier.size(160.dp)) {
            val w = size.width
            val h = size.height
            
            val path = Path().apply {
                moveTo(w * 0.2f, 0f)
                lineTo(w * 0.6f, 0f)
                cubicTo(w, 0f, w, h, w * 0.6f, h)
                lineTo(w * 0.2f, h)
                close()
            }

            drawPath(
                path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF5A84), Color(0xFFFF2D7A), Color(0xFF8A2BE2))
                ),
                alpha = alpha
            )

            // Glossy highlight
            drawPath(
                path,
                brush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.3f * alpha),
                    1f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Film strip holes
            for (i in 0 until 5) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f * alpha),
                    topLeft = Offset(w * 0.28f, h * (0.15f + i * 0.18f)),
                    size = Size(w * 0.08f, h * 0.1f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }

            // Play symbol
            val playPath = Path().apply {
                val pw = w * 0.22f
                val ph = h * 0.22f
                val px = w * 0.48f
                val py = h * 0.39f
                moveTo(px, py)
                lineTo(px + pw, py + ph / 2)
                lineTo(px, py + ph)
                close()
            }
            drawPath(playPath, Color.White, alpha = alpha)
        }
    }
}
