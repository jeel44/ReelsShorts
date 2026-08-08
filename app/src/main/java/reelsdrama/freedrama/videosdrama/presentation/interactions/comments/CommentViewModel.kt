package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val repository: FakeCommentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CommentState())
    val state: StateFlow<CommentState> = _state.asStateFlow()

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val comments = repository.getComments(videoId)
            _state.update { it.copy(comments = comments, isLoading = false) }
        }
    }

    fun onEvent(event: CommentEvent) {
        when (event) {
            is CommentEvent.OnInputChanged -> {
                _state.update { it.copy(currentInput = event.text) }
            }
            is CommentEvent.OnSendClick -> {
                sendComment()
            }
            is CommentEvent.OnLikeClick -> {
                toggleLike(event.commentId)
            }
            is CommentEvent.OnReplyClick -> {
                _state.update { it.copy(currentInput = "@${event.username} " + it.currentInput) }
            }
            is CommentEvent.OnDeleteClick -> {
                _state.update { it.copy(comments = it.comments.filter { c -> c.id != event.commentId }) }
            }
            is CommentEvent.OnCopyClick -> {
                // Clipboard logic would go here
            }
            is CommentEvent.OnReportClick -> {
                // Report logic
            }
        }
    }

    private fun sendComment() {
        val text = _state.value.currentInput
        if (text.isBlank()) return

        val newComment = Comment(
            id = "new_comment_${System.currentTimeMillis()}",
            username = "me",
            userAvatarUrl = "https://i.pravatar.cc/150?u=me",
            text = text,
            timeAgo = "1s",
            likeCount = "0"
        )

        _state.update { 
            it.copy(
                comments = listOf(newComment) + it.comments,
                currentInput = ""
            )
        }
    }

    private fun toggleLike(commentId: String) {
        _state.update { state ->
            state.copy(
                comments = state.comments.map { 
                    if (it.id == commentId) it.copy(isLiked = !it.isLiked) else it 
                }
            )
        }
    }
}
