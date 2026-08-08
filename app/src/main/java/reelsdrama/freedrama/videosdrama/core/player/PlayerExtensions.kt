package reelsdrama.freedrama.videosdrama.core.player

import androidx.media3.exoplayer.ExoPlayer

fun ExoPlayer.togglePlayPause() {
    if (isPlaying) {
        pause()
    } else {
        play()
    }
}
