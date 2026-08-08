package reelsdrama.freedrama.videosdrama.core.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import reelsdrama.freedrama.videosdrama.core.constants.PlayerConstants
import reelsdrama.freedrama.videosdrama.core.player.components.PlayPauseOverlay
import reelsdrama.freedrama.videosdrama.core.player.components.SpeedBadge
import reelsdrama.freedrama.videosdrama.core.player.components.VideoProgressBar
import reelsdrama.freedrama.videosdrama.core.player.components.VideoShimmer
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * A reusable Video Player component using Media3 ExoPlayer.
 * Handles playback gestures, loading states, and progress visualization.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    video: Video,
    playerManager: VideoPlayerManager,
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val player = playerManager.getPlayer(video.id, video.videoUrl)
    
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackSpeed by remember { mutableStateOf(PlayerConstants.DEFAULT_SPEED) }
    var progress by remember { mutableStateOf(0f) }

    // Position tracking for progress bar - Only poll when playing to save CPU/Battery
    LaunchedEffect(player, isPlaying) {
        if (isPlaying) {
            while (true) {
                if (player.duration > 0) {
                    progress = player.currentPosition.toFloat() / player.duration
                }
                
                delay(PlayerConstants.POSITION_POLLING_MS)
            }
        }
    }

    DisposableEffect(video.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            playerManager.stopAndRelease(video.id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleTap(offset)
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerManager.togglePlayPause(video.id)
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playbackSpeed = PlayerConstants.LONG_PRESS_SPEED
                        playerManager.setPlaybackSpeed(video.id, PlayerConstants.LONG_PRESS_SPEED)
                    },
                    onPress = {
                        tryAwaitRelease()
                        playbackSpeed = PlayerConstants.DEFAULT_SPEED
                        playerManager.setPlaybackSpeed(video.id, PlayerConstants.DEFAULT_SPEED)
                    }
                )
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = player }
        )

        // Shimmer Loading / Thumbnail Placeholder
        VideoShimmer(visible = !isFirstFrameRendered)

        // Top Progress Bar
        VideoProgressBar(
            progress = progress,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 2x Speed Badge
        SpeedBadge(
            visible = playbackSpeed > PlayerConstants.DEFAULT_SPEED,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Play/Pause Icon Overlay
        PlayPauseOverlay(
            visible = !isPlaying && isFirstFrameRendered,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
