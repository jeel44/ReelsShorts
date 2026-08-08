package reelsdrama.freedrama.videosdrama.presentation.interactions.like

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LikeViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(LikeButtonState())
    val state = _state.asStateFlow()

    fun onEvent(event: LikeButtonEvent) {
        when (event) {
            is LikeButtonEvent.OnDoubleTap -> {
                if (!_state.value.likedVideoIds.contains(event.videoId)) {
                    handleLike(event.videoId)
                }
                addAnimation(event.offset)
            }
            is LikeButtonEvent.OnLikeClick -> {
                val isCurrentlyLiked = _state.value.likedVideoIds.contains(event.videoId)
                toggleLike(event.videoId)
                if (!isCurrentlyLiked) {
                    // Trigger overlay animation at screen center for button click
                    addAnimation(androidx.compose.ui.geometry.Offset(500f, 1000f))
                }
            }
            is LikeButtonEvent.AnimationFinished -> removeAnimation(event.animationId)
        }
    }

    private fun handleLike(videoId: String) {
        _state.update { currentState ->
            currentState.copy(
                likedVideoIds = currentState.likedVideoIds + videoId,
                videoLikeCounts = currentState.videoLikeCounts.toMutableMap().apply {
                    this[videoId] = (this[videoId] ?: 0) + 1
                }
            )
        }
    }

    private fun toggleLike(videoId: String) {
        val isCurrentlyLiked = _state.value.likedVideoIds.contains(videoId)
        _state.update { currentState ->
            if (isCurrentlyLiked) {
                currentState.copy(
                    likedVideoIds = currentState.likedVideoIds - videoId,
                    videoLikeCounts = currentState.videoLikeCounts.toMutableMap().apply {
                        this[videoId] = (this[videoId] ?: 1) - 1
                    }
                )
            } else {
                currentState.copy(
                    likedVideoIds = currentState.likedVideoIds + videoId,
                    videoLikeCounts = currentState.videoLikeCounts.toMutableMap().apply {
                        this[videoId] = (this[videoId] ?: 0) + 1
                    }
                )
            }
        }
    }
    
    private fun addAnimation(offset: androidx.compose.ui.geometry.Offset) {
        _state.update {
            it.copy(
                activeAnimations = it.activeAnimations + HeartAnimationInfo(
                    id = System.nanoTime(),
                    offset = offset
                )
            )
        }
    }
    
    private fun removeAnimation(id: Long) {
        _state.update {
            it.copy(activeAnimations = it.activeAnimations.filter { anim -> anim.id != id })
        }
    }

    fun isLiked(videoId: String): Boolean = _state.value.likedVideoIds.contains(videoId)

    fun getLikeCount(videoId: String, initialCount: String): String {
        val extra = _state.value.videoLikeCounts[videoId] ?: 0
        if (extra == 0) return initialCount
        
        // Simple logic for fake data
        return if (initialCount.contains("K") || initialCount.contains("M")) {
            // If it's already a large number, just appending "+1" is weird, but let's just return it
            initialCount
        } else {
            val count = initialCount.toIntOrNull() ?: 0
            (count + extra).toString()
        }
    }
}
