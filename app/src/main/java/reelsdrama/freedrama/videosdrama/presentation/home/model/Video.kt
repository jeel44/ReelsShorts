package reelsdrama.freedrama.videosdrama.presentation.home.model

import androidx.compose.ui.graphics.Color

/**
 * Presentation model used by the Home reels feed.
 */
data class Video(
    val id: String,
    val username: String,
    val caption: String,
    val musicName: String,
    val hashtags: List<String>,
    val isVerified: Boolean,
    val likeCount: String,
    val commentCount: String,
    val shareCount: String,
    val viewCount: String,
    val videoUrl: String,
    val thumbnailGradient: List<Color>,
)
