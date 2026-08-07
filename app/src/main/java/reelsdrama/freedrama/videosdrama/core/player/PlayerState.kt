package reelsdrama.freedrama.videosdrama.core.player

import androidx.media3.common.PlaybackException

sealed interface PlayerState {
    data object Loading : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data object Buffering : PlayerState
    data object Completed : PlayerState
    data class Error(
        val message: String,
        val cause: PlaybackException? = null,
    ) : PlayerState
}
