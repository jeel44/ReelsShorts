package reelsdrama.freedrama.videosdrama.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

fun ExoPlayer.playUrl(
    url: String,
    playWhenReady: Boolean,
    loop: Boolean = true,
    startPositionMs: Long = 0L,
) {
    if (currentMediaItem?.localConfiguration?.uri.toString() != url) {
        setMediaItem(MediaItem.fromUri(url), startPositionMs)
        repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        this.playWhenReady = playWhenReady
        prepare()
    } else {
        repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        this.playWhenReady = playWhenReady
    }
}

fun ExoPlayer.mute() {
    volume = 0f
}

fun ExoPlayer.unmute() {
    volume = 1f
}

fun ExoPlayer.safeStop() {
    playWhenReady = false
    stop()
    clearMediaItems()
}
