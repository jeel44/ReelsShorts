package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.util.UnstableApi
import java.util.ArrayDeque

@OptIn(UnstableApi::class)
class PlayerPool(
    private val context: Context,
    private val maxPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
) {
    private val availablePlayers = ArrayDeque<ExoPlayer>()
    private val leasedPlayers = linkedMapOf<String, ExoPlayer>()

    fun acquire(key: String): ExoPlayer {
        leasedPlayers[key]?.let { return it }
        val player = availablePlayers.pollFirst() ?: createPlayer()
        leasedPlayers[key] = player
        return player
    }

    fun get(key: String): ExoPlayer? = leasedPlayers[key]

    fun release(key: String) {
        val player = leasedPlayers.remove(key) ?: return
        player.safeStop()
        if (availablePlayers.size < maxPoolSize) {
            availablePlayers.addLast(player)
        } else {
            player.release()
        }
    }

    fun releaseExcept(keysToKeep: Set<String>) {
        leasedPlayers.keys.toList()
            .filterNot(keysToKeep::contains)
            .forEach(::release)
    }

    fun releaseAll() {
        leasedPlayers.keys.toList().forEach(::release)
        while (availablePlayers.isNotEmpty()) {
            availablePlayers.removeFirst().release()
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(PlayerCache.dataSourceFactory(context)))
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            }
    }

    companion object {
        private const val DEFAULT_MAX_POOL_SIZE = 5
        private const val SEEK_INCREMENT_MS = 5_000L
    }
}
