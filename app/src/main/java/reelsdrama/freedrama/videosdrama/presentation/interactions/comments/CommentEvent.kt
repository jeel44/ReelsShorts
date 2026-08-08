package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

sealed interface CommentEvent {
    data class OnInputChanged(val text: String) : CommentEvent
    data object OnSendClick : CommentEvent
    data class OnLikeClick(val commentId: String) : CommentEvent
    data class OnReplyClick(val username: String) : CommentEvent
    data class OnDeleteClick(val commentId: String) : CommentEvent
    data class OnCopyClick(val text: String) : CommentEvent
    data class OnReportClick(val commentId: String) : CommentEvent
}
