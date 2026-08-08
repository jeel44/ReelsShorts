package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.compose.ui.geometry.Offset

data class LikeButtonState(
    val likedVideoIds: Set<String> = emptySet(),
    val videoLikeCounts: Map<String, Int> = emptyMap(),
    val activeAnimations: List<HeartAnimationInfo> = emptyList()
)

data class HeartAnimationInfo(
    val id: Long,
    val offset: Offset
)
