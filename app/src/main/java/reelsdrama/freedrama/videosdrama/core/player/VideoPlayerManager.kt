package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
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
 *
 * This is a Hilt `@Singleton` - it lives for the whole process and outlives any single
 * screen/Activity, so it observes [ProcessLifecycleOwner] directly (see [init]) rather than
 * relying on a screen-scoped `DisposableEffect`. That's what actually stops audio when the app
 * is backgrounded via Home/recents: Compose's composition (and therefore any screen-level
 * LaunchedEffect/DisposableEffect in the feed) survives an Activity stop/restart untouched, so
 * nothing screen-scoped ever observes backgrounding - only an app-process-level observer does.
 */
@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val players = mutableMapOf<String, ExoPlayer>()
    private var currentVideoId: String? = null

    init {
        // ON_PAUSE, not ON_STOP: fires the instant the app is backgrounded (Home/recents/app
        // switch), without the ON_STOP lag some OEMs/multi-window modes introduce. Registered
        // once for the process lifetime - never removed - since this manager itself lives for
        // the process lifetime.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            PlayerLifecycleObserver(
                onPause = { pauseAll() },
                // Mirrors onPause: resumes whichever video was current when the app returns to
                // the foreground. This can race a brief interstitial/coin-gate overlay
                // reappearing (which independently re-pauses via pause(videoId) once its own
                // state settles) - an existing, pre-audio-fix race in that ad flow, not one
                // this introduces - but it avoids the worse regression of playback staying
                // silently paused forever after a normal background/foreground cycle.
                onResume = { resumeCurrent() }
            )
        )
    }

    fun getPlayer(videoId: String, url: String): ExoPlayer {
        return players.getOrPut(videoId) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                // Explicit, not relying on ExoPlayer's own default: enables real audio-focus
                // negotiation (request on play, abandon on pause/stop) and auto-pause when
                // audio is rerouted away from a headset (ACTION_AUDIO_BECOMING_NOISY) - neither
                // is on by default from a bare ExoPlayer.Builder().build().
                setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
                setHandleAudioBecomingNoisy(true)
                prepare()
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    /**
     * Retries loading the current media item for [videoId]'s already-created player, after a
     * playback error (see [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer]'s
     * `onPlayerError` handling). Calls [ExoPlayer.prepare] again on the SAME cached instance -
     * ExoPlayer's own documented recovery path: a player that has moved to an error state
     * re-attempts loading its current [MediaItem] when [ExoPlayer.prepare] is called again, so
     * this reuses [getPlayer]'s own load call rather than rebuilding a player/[MediaItem] from
     * scratch. No-op if [videoId] has no cached player (e.g. already released via
     * [stopAndRelease]).
     */
    fun retry(videoId: String) {
        players[videoId]?.prepare()
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

    /**
     * Pauses every tracked player, not just [currentVideoId] - defensive against any preloaded
     * neighbor (previous/next page) that ended up `playWhenReady = true`, so backgrounding the
     * app can never leave orphaned audio behind regardless of which entry that turned out to
     * be. Does NOT release anything: this is a singleton meant to survive backgrounding, so
     * players stay warm and just resume instantly on [resumeCurrent] instead of re-preparing.
     */
    fun pauseAll() {
        players.values.forEach { it.playWhenReady = false }
    }

    private fun resumeCurrent() {
        currentVideoId?.let { players[it]?.playWhenReady = true }
    }

    /**
     * Releases every tracked player and its AudioFocus/surface resources. Only call this from
     * a proper app-level teardown (e.g. [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel.onCleared])
     * - never from a background/pause path, since this manager is a Hilt singleton meant to
     * survive backgrounding without re-initializing players on every resume.
     */
    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
    }
}
