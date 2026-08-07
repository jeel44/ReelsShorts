package reelsdrama.freedrama.videosdrama.core.player

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun VideoPlayer(
    videoId: String,
    videoUrl: String,
    modifier: Modifier = Modifier,
    playerManager: VideoPlayerManager = rememberVideoPlayerManager(),
    autoPlay: Boolean = true,
    muted: Boolean = false,
    loop: Boolean = true,
    scaling: VideoScaling = VideoScaling.Crop,
    placeholderImageUrl: String? = null,
    showControls: Boolean = false,
    showFullscreenButton: Boolean = true,
    onFullscreenChanged: (Boolean) -> Unit = {},
    overlay: @Composable BoxScope.(PlayerState) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(videoId, videoUrl) { playerManager.playerFor(videoId, videoUrl, autoPlay, loop) }
    val playerState by playerManager.state(videoId).collectAsState()
    var isMuted by remember(videoId) { mutableStateOf(muted) }
    var isFullscreen by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(isMuted) { playerManager.setMuted(videoId, isMuted) }
    LaunchedEffect(autoPlay) { if (autoPlay) playerManager.play(videoId) else playerManager.pause(videoId) }

    DisposableEffect(lifecycleOwner, videoId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (autoPlay) playerManager.resume(videoId)
                Lifecycle.Event.ON_STOP -> playerManager.pause(videoId)
                Lifecycle.Event.ON_DESTROY -> playerManager.release(videoId)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.release(videoId)
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Placeholder(placeholderImageUrl = placeholderImageUrl, visible = playerState is PlayerState.Loading)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = showControls
                    resizeMode = scaling.resizeMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = showControls
                playerView.resizeMode = scaling.resizeMode
            },
        )

        when (val state = playerState) {
            PlayerState.Loading -> LoadingShimmer()
            PlayerState.Buffering -> CircularProgressIndicator(modifier = Modifier.size(42.dp), color = Color.White)
            is PlayerState.Error -> ErrorOverlay(message = state.message) { playerManager.retry(videoId, videoUrl, autoPlay, loop) }
            PlayerState.Paused -> IconButton(onClick = { playerManager.play(videoId) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(56.dp))
            }
            else -> Unit
        }

        IconButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            onClick = { isMuted = !isMuted },
        ) {
            Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = if (isMuted) "Unmute" else "Mute", tint = Color.White)
        }

        if (showFullscreenButton) {
            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = {
                    isFullscreen = !isFullscreen
                    context.findActivity()?.requestedOrientation = if (isFullscreen) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    onFullscreenChanged(isFullscreen)
                },
            ) {
                Icon(if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
            }
        }

        overlay(playerState)
    }
}

enum class VideoScaling(val resizeMode: Int) {
    Fit(AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Crop(AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    Fill(AspectRatioFrameLayout.RESIZE_MODE_FILL),
}

@Composable
fun rememberVideoPlayerManager(): VideoPlayerManager {
    val context = LocalContext.current.applicationContext
    return remember(context) { VideoPlayerManager(context) }
}

@Composable
private fun Placeholder(placeholderImageUrl: String?, visible: Boolean) {
    if (!visible || placeholderImageUrl == null) return
    val painter = rememberAsyncImagePainter(placeholderImageUrl)
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().alpha(if (painter.state is AsyncImagePainter.State.Success) 1f else 0f),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun LoadingShimmer() {
    val transition = rememberInfiniteTransition(label = "video_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "video_shimmer_alpha",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.DarkGray.copy(alpha = alpha), Color.Black))),
    )
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Surface(color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            IconButton(modifier = Modifier.align(Alignment.Center).size(96.dp), onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
            }
        }
    }
}

private fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
