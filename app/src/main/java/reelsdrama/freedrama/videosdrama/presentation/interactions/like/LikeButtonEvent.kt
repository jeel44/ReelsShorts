package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.compose.ui.geometry.Offset

sealed interface LikeButtonEvent {
    data class OnDoubleTap(val videoId: String, val offset: Offset) : LikeButtonEvent
    data class OnLikeClick(val videoId: String) : LikeButtonEvent
    data class AnimationFinished(val animationId: Long) : LikeButtonEvent
}
