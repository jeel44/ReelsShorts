package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class responsible for orchestrating video playback across the application.
 * It maintains a window of active [ExoPlayer] instances (Previous, Current, Next)
 * to ensure smooth transitions and optimal resource usage.
 */
@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val players = mutableMapOf<String, ExoPlayer>()
    private var currentVideoId: String? = null

    fun getPlayer(videoId: String, url: String): ExoPlayer {
        return players.getOrPut(videoId) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    fun play(videoId: String) {
        if (currentVideoId != videoId) {
            currentVideoId?.let { players[it]?.pause() }
            currentVideoId = videoId
        }
        players[videoId]?.playWhenReady = true
    }

    fun pause(videoId: String) {
        players[videoId]?.playWhenReady = false
    }

    fun setPlaybackSpeed(videoId: String, speed: Float) {
        players[videoId]?.setPlaybackSpeed(speed)
    }

    fun togglePlayPause(videoId: String) {
        players[videoId]?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun stopAndRelease(videoId: String) {
        players.remove(videoId)?.release()
    }

    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
    }
}
