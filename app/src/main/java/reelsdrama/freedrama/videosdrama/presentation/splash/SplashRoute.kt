package reelsdrama.freedrama.videosdrama.presentation.splash

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.presentation.theme.BrandGradientColors

/**
 * "Editorial fade" splash - visual layer only. Navigation is still driven by
 * [SplashViewModel.startTimer]/[SplashViewModel.navigationEvent], NOT a hardcoded delay here,
 * so the cold-start app-open-ad gating in the ViewModel (SPLASH_DELAY_MS + ad-ready wait +
 * ad show/dismiss) keeps working unchanged. This composable's own animation timeline (kicker
 * -> wordmark -> underline -> progress bar, completing ~2600ms in) is tuned to land around the
 * same moment navigation actually happens, but the two are intentionally decoupled.
 */
@Composable
fun SplashRoute(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.startTimer(activity)

        viewModel.navigationEvent.collectLatest {
            onNavigateToHome()
        }
    }

    SplashScreenContent()
}

@Composable
private fun SplashScreenContent() {
    val density = LocalDensity.current
    val brandUnderlineBrush = remember {
        Brush.horizontalGradient(listOf(BrandGradientColors.first(), BrandGradientColors.last()))
    }

    val kickerAlpha = remember { Animatable(0f) }
    val underlineWidth = remember { Animatable(0f) }
    val progressWidth = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            kickerAlpha.animateTo(1f, tween(durationMillis = 300))
        }
        launch {
            delay(1000)
            underlineWidth.animateTo(
                targetValue = 64f,
                animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
            )
        }
        launch {
            delay(600)
            progressWidth.animateTo(
                targetValue = 110f,
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A0E1C), Color(0xFF0A0A0F), Color(0xFF050208)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        // Glow 1 - upper-left, brand pink
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to BrandGradientColors.first().copy(alpha = 0.22f),
                        0.55f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                    radius = size.maxDimension
                )
            )
        }

        // Glow 2 - lower-right, brand purple
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to BrandGradientColors.last().copy(alpha = 0.15f),
                        0.55f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.75f, size.height * 0.75f),
                    radius = size.maxDimension
                )
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PRESENTING",
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.alpha(kickerAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val wordmarkStartOffsetPx = remember(density) { with(density) { 28.sp.toPx() } * 0.6f }
            Row {
                "Free Drama".forEachIndexed { index, char ->
                    AnimatedWordmarkLetter(
                        char = char,
                        index = index,
                        startOffsetPx = wordmarkStartOffsetPx
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .width(underlineWidth.value.dp)
                    .height(1.dp)
                    .background(brandUnderlineBrush)
            )
        }

        // Bottom progress bar - track + animated fill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .width(110.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .width(progressWidth.value.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(brandUnderlineBrush)
            )
        }
    }
}

/**
 * One letter of the "Free Drama" wordmark, animating its own offsetY/blur/alpha so the word
 * reveals letter-by-letter rather than as a single block. [startOffsetPx] is 60% of the
 * wordmark's font-size-derived height, in px (matches [Modifier.graphicsLayer]'s translationY
 * units). Blur requires API 31+ (`Modifier.blur`); below that we fall back to offset+alpha only.
 */
@Composable
private fun AnimatedWordmarkLetter(char: Char, index: Int, startOffsetPx: Float) {
    val offsetY = remember { Animatable(startOffsetPx) }
    val blurDp = remember { Animatable(6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(300 + index * 45L)
        val spec = tween<Float>(durationMillis = 900, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
        coroutineScope {
            launch { offsetY.animateTo(0f, spec) }
            launch { blurDp.animateTo(0f, spec) }
            launch { alpha.animateTo(1f, spec) }
        }
    }

    Text(
        text = char.toString(),
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        color = Color.White,
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY.value
                this.alpha = alpha.value
            }
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurDp.value.dp)
                } else {
                    Modifier
                }
            )
    )
}
