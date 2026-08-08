package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import reelsdrama.freedrama.videosdrama.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    videoId: String,
    onDismissRequest: () -> Unit,
    viewModel: CommentViewModel = hiltViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(videoId) {
        viewModel.loadComments(videoId)
    }

    // Auto-scroll to top when a new comment is added (first in list)
    LaunchedEffect(state.comments.size) {
        if (state.comments.isNotEmpty() && !state.isLoading) {
            listState.animateScrollToItem(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = BottomSheetDefaults.ScrimColor.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f) // 80% height as requested
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.comments_count, state.comments.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                onLikeClick = { viewModel.onEvent(CommentEvent.OnLikeClick(comment.id)) },
                                onReplyClick = { viewModel.onEvent(CommentEvent.OnReplyClick(comment.username)) },
                                onDeleteClick = { viewModel.onEvent(CommentEvent.OnDeleteClick(comment.id)) },
                                onCopyClick = { viewModel.onEvent(CommentEvent.OnCopyClick(comment.text)) },
                                onReportClick = { viewModel.onEvent(CommentEvent.OnReportClick(comment.id)) }
                            )
                        }
                    }
                }
            }

            CommentInput(
                value = state.currentInput,
                onValueChange = { viewModel.onEvent(CommentEvent.OnInputChanged(it)) },
                onSendClick = { viewModel.onEvent(CommentEvent.OnSendClick) }
            )
        }
    }
}
