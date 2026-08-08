package reelsdrama.freedrama.videosdrama.presentation.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants
import reelsdrama.freedrama.videosdrama.presentation.splash.components.AnimatedBackground
import reelsdrama.freedrama.videosdrama.presentation.splash.components.AnimatedLogo
import reelsdrama.freedrama.videosdrama.presentation.splash.components.LoadingDots

@Composable
fun SplashRoute(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    var state by remember { mutableStateOf(SplashAnimationState()) }

    LaunchedEffect(Unit) {
        viewModel.startTimer()
        
        // Start Animations
        launch {
            // Logo Entrance: 0.8 -> 1.08 -> 1.0
            animate(
                initialValue = 0.8f,
                targetValue = 1.08f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            ) { value, _ -> state = state.copy(logoScale = value) }
            animate(
                initialValue = 1.08f,
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) { value, _ -> state = state.copy(logoScale = value) }
        }

        launch {
            // Logo Alpha
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(1000)
            ) { value, _ -> state = state.copy(logoAlpha = value) }
        }

        launch {
            delay(800)
            // Title: Translate + Blur + Alpha
            animate(
                initialValue = 20f,
                targetValue = 0f,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            ) { value, _ -> state = state.copy(titleTranslationY = value) }
        }

        launch {
            delay(800)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(1000)
            ) { value, _ -> state = state.copy(titleAlpha = value) }
        }

        launch {
            delay(800)
            animate(
                initialValue = 10f,
                targetValue = 0f,
                animationSpec = tween(1200)
            ) { value, _ -> state = state.copy(titleBlur = value) }
        }

        launch {
            delay(1500)
            // Tagline
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(800)
            ) { value, _ -> state = state.copy(taglineAlpha = value) }
        }

        // Handle Navigation / Exit
        viewModel.navigationEvent.collectLatest {
            // Exit Animation: Fade out + Logo scale down
            launch {
                animate(
                    initialValue = 1.0f,
                    targetValue = 0.95f,
                    animationSpec = tween(500)
                ) { value, _ -> state = state.copy(logoScale = value) }
            }
            animate(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = tween(600)
            ) { value, _ -> state = state.copy(screenAlpha = value) }
            
            onNavigateToHome()
        }
    }

    SplashScreenContent(state)
}

@Composable
private fun SplashScreenContent(state: SplashAnimationState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(state.screenAlpha),
        contentAlignment = Alignment.Center
    ) {
        AnimatedBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = state.logoScale
                    scaleY = state.logoScale
                }
            ) {
                AnimatedLogo(scale = state.logoScale, alpha = state.logoAlpha)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // FREE DRAMA
            Row(
                modifier = Modifier
                    .offset(y = state.titleTranslationY.dp)
                    .alpha(state.titleAlpha)
                    .blur(state.titleBlur.dp)
            ) {
                Text(
                    text = "FREE",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DRAMA",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF2D7A), Color(0xFFFF4D6D))
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Unlimited Drama Shorts",
                color = Color.White.copy(alpha = 0.75f * state.taglineAlpha),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(state.taglineAlpha)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(state.logoAlpha) // Appear with logo
        ) {
            LoadingDots(alpha = state.logoAlpha)
        }
    }
}
