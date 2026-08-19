package reelsdrama.freedrama.videosdrama.presentation.interactions.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import reelsdrama.freedrama.videosdrama.R

/**
 * Shared by [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelCard] and
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard] - takes the id/caption
 * primitives it actually needs rather than a
 * [reelsdrama.freedrama.videosdrama.presentation.home.model.Video] directly, so both a video
 * reel and a text story can share this one sheet instead of forking a second copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    contentId: String,
    caption: String,
    onDismissRequest: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val linkCopiedText = stringResource(R.string.link_copied)

    LaunchedEffect(state.showCopyLinkSnackbar) {
        if (state.showCopyLinkSnackbar) {
            snackbarHostState.showSnackbar(linkCopiedText)
            viewModel.onEvent(ShareEvent.SnackbarDismissed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f) // 55% height
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.share_to),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        ShareOptionItem(
                            name = stringResource(R.string.copy_link),
                            icon = Icons.Default.ContentCopy,
                            onClick = { viewModel.onEvent(ShareEvent.OnCopyLink(contentId)) }
                        )
                    }

                    items(state.availableApps) { app ->
                        ShareOptionItem(
                            name = app.name,
                            // In a real app we'd load the actual app icon
                            icon = Icons.Default.MoreHoriz,
                            onClick = { viewModel.onEvent(ShareEvent.OnAppClick(app.packageName, contentId, caption)) }
                        )
                    }

                    item {
                        ShareOptionItem(
                            name = stringResource(R.string.more),
                            icon = Icons.Default.MoreHoriz,
                            onClick = { viewModel.onEvent(ShareEvent.OnMoreClick(contentId, caption)) }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun ShareOptionItem(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}
