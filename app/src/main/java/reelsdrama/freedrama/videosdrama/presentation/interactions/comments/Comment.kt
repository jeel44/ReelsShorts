package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

import androidx.compose.runtime.Immutable

@Immutable
data class Comment(
    val id: String,
    val username: String,
    val userAvatarUrl: String,
    val text: String,
    val timeAgo: String,
    val likeCount: String,
    val isVerified: Boolean = false,
    val isLiked: Boolean = false
)
