package reelsdrama.freedrama.videosdrama.core.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import reelsdrama.freedrama.videosdrama.R

@Composable
fun SpeedBadge(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier.padding(top = 40.dp)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.player_speed_2x),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun PlayPauseOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)) + scaleIn(tween(100)),
        exit = fadeOut(tween(100)) + scaleOut(tween(100)),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(80.dp)
        )
    }
}

/**
 * Replaces [VideoShimmer] when [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer]'s
 * player reports [androidx.media3.common.Player.Listener.onPlayerError] - a real, visible failure
 * state instead of leaving the shimmer's "still loading" look up forever with no indication
 * anything went wrong (see [reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource]'s doc
 * comment for the outage this was written for). Deliberately kept in the same dark/white-icon
 * language [PlayPauseOverlay]/[SpeedBadge] already use above - no red/alarming error styling.
 *
 * The whole overlay is tap-to-retry, not just a small button, since it already fully occludes the
 * video area whenever it's showing - [onRetry] is expected to call
 * [reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager.retry] for this video's id.
 */
@Composable
fun PlaybackErrorOverlay(
    visible: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onRetry)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.player_error_message),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.player_error_retry),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
