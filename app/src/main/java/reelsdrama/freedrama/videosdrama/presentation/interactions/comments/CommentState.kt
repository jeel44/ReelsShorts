package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

import androidx.compose.runtime.Immutable

@Immutable
data class CommentState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val currentInput: String = ""
)
