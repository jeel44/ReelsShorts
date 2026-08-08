package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayer
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoInfoOverlay
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoSideActionBar
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video
import reelsdrama.freedrama.videosdrama.presentation.interactions.comments.CommentsBottomSheet
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeAnimationOverlay
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeButtonEvent
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeViewModel
import reelsdrama.freedrama.videosdrama.presentation.interactions.share.ShareBottomSheet

/**
 * A full-screen card representing a single reel.
 * Coordinates video playback, user interactions (likes, comments), and UI overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelCard(
    video: Video,
    isTabSelected: Boolean,
    playerManager: VideoPlayerManager,
    likeViewModel: LikeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val state by likeViewModel.state.collectAsState()
    val isLiked = likeViewModel.isLiked(video.id)
    val likeCount = likeViewModel.getLikeCount(video.id, video.likeCount)
    
    var showComments by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Bottom-most layer: Video Player with Gestures
        VideoPlayer(
            video = video,
            playerManager = playerManager,
            onDoubleTap = { offset ->
                likeViewModel.onEvent(LikeButtonEvent.OnDoubleTap(video.id, offset))
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Heart Animation Overlay
        LikeAnimationOverlay(
            activeAnimations = state.activeAnimations,
            onAnimationFinished = { id ->
                likeViewModel.onEvent(LikeButtonEvent.AnimationFinished(id))
            }
        )

        // 3. Content Overlays (UI Info & Actions)
        VideoInfoOverlay(
            video = video,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        VideoSideActionBar(
            video = video,
            isLiked = isLiked,
            likeCount = likeCount,
            onLikeClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                likeViewModel.onEvent(LikeButtonEvent.OnLikeClick(video.id)) 
            },
            onCommentClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showComments = true 
            },
            onShareClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showShare = true 
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 8.dp)
        )

        if (showComments) {
            CommentsBottomSheet(
                videoId = video.id,
                onDismissRequest = { showComments = false }
            )
        }

        if (showShare) {
            ShareBottomSheet(
                video = video,
                onDismissRequest = { showShare = false }
            )
        }
    }
}
