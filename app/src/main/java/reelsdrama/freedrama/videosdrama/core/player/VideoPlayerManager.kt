package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val pool = PlayerPool(appContext)
    private val stateFlows = mutableMapOf<String, MutableStateFlow<PlayerState>>()
    private val listeners = mutableMapOf<String, Player.Listener>()

    fun playerFor(videoId: String, videoUrl: String, autoPlay: Boolean = true, loop: Boolean = true): ExoPlayer {
        val player = pool.acquire(videoId)
        attachListener(videoId, player)
        stateFlowFor(videoId).value = PlayerState.Loading
        player.playUrl(videoUrl, playWhenReady = autoPlay, loop = loop)
        return player
    }

    fun state(videoId: String): StateFlow<PlayerState> = stateFlowFor(videoId).asStateFlow()

    fun play(videoId: String) {
        pool.get(videoId)?.play()
    }

    fun pause(videoId: String) {
        pool.get(videoId)?.pause()
        stateFlowFor(videoId).value = PlayerState.Paused
    }

    fun resume(videoId: String) = play(videoId)

    fun retry(videoId: String, videoUrl: String, autoPlay: Boolean = true, loop: Boolean = true): ExoPlayer {
        release(videoId)
        return playerFor(videoId, videoUrl, autoPlay, loop)
    }

    fun setMuted(videoId: String, muted: Boolean) {
        pool.get(videoId)?.volume = if (muted) 0f else 1f
    }

    fun preload(items: List<Pair<String, String>>, startIndex: Int, nextCount: Int = PRELOAD_NEXT_COUNT) {
        val nextItems = items.drop(startIndex + 1).take(nextCount)
        nextItems.forEach { (id, url) ->
            val player = pool.acquire(id)
            attachListener(id, player)
            player.playUrl(url, playWhenReady = false, loop = true)
        }
    }

    fun retainAroundViewport(items: List<Pair<String, String>>, visibleIndex: Int, distance: Int = RETAIN_DISTANCE) {
        val idsToKeep = items
            .mapIndexedNotNull { index, item -> item.first.takeIf { kotlin.math.abs(index - visibleIndex) <= distance } }
            .toSet()
        releaseExcept(idsToKeep)
    }

    fun release(videoId: String) {
        detachListener(videoId)
        pool.release(videoId)
        stateFlows.remove(videoId)
    }

    fun releaseExcept(videoIdsToKeep: Set<String>) {
        listeners.keys.toList().filterNot(videoIdsToKeep::contains).forEach(::detachListener)
        stateFlows.keys.toList().filterNot(videoIdsToKeep::contains).forEach(stateFlows::remove)
        pool.releaseExcept(videoIdsToKeep)
    }

    fun releaseAll() {
        listeners.keys.toList().forEach(::detachListener)
        stateFlows.clear()
        pool.releaseAll()
    }

    private fun attachListener(videoId: String, player: ExoPlayer) {
        if (listeners[videoId] != null) return
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                stateFlowFor(videoId).value = when (playbackState) {
                    Player.STATE_BUFFERING -> PlayerState.Buffering
                    Player.STATE_READY -> if (player.isPlaying) PlayerState.Playing else PlayerState.Paused
                    Player.STATE_ENDED -> PlayerState.Completed
                    else -> PlayerState.Loading
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                stateFlowFor(videoId).value = if (isPlaying) PlayerState.Playing else {
                    if (player.playbackState == Player.STATE_BUFFERING) PlayerState.Buffering else PlayerState.Paused
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stateFlowFor(videoId).value = PlayerState.Error(error.localizedMessage ?: "Unable to play video", error)
            }
        }
        player.addListener(listener)
        listeners[videoId] = listener
    }

    private fun detachListener(videoId: String) {
        val listener = listeners.remove(videoId) ?: return
        pool.get(videoId)?.removeListener(listener)
    }

    private fun stateFlowFor(videoId: String): MutableStateFlow<PlayerState> =
        stateFlows.getOrPut(videoId) { MutableStateFlow(PlayerState.Loading) }

    companion object {
        const val PRELOAD_NEXT_COUNT = 2
        private const val RETAIN_DISTANCE = 3
    }
}
