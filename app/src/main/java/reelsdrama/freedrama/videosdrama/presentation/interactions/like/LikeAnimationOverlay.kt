package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

@Composable
fun LikeAnimationOverlay(
    activeAnimations: List<HeartAnimationInfo>,
    onAnimationFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        activeAnimations.forEach { info ->
            key(info.id) {
                DoubleTapLikeAnimation(
                    info = info,
                    onAnimationEnd = { onAnimationFinished(info.id) }
                )
            }
        }
    }
}
