package reelsdrama.freedrama.videosdrama.core.player

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Loading : PlayerState
    data object Buffering : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data object Ended : PlayerState
    data class Error(val message: String) : PlayerState
}
