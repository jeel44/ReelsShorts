package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.AdConfirmationDialog
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager
import kotlinx.coroutines.launch

/**
 * The main container for the Reels Feed.
 * Displays a single continuous vertical reels feed.
 * Also supports a single-category view when categoryId is provided.
 */
@Composable
fun ReelsFeedScreen(
    uiState: FeedUiState,
    playerManager: VideoPlayerManager,
    onEvent: (FeedEvent) -> Unit,
    onCoinClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val isCategoryView = uiState.categoryId != null
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val adDismissedEarlyMessage = stringResource(R.string.rewards_ad_dismissed_early)
    val adNotAvailableMessage = stringResource(R.string.rewards_ad_not_available)

    // Show one-shot feedback for the last rewarded-ad attempt, then clear it so it doesn't
    // reappear on recomposition/rotation. The earned amount comes from the ad SDK itself
    // (not a hardcoded guess), so this always matches what was actually credited.
    // stringResource() can't be called from this suspend (non-@Composable) block, so the
    // earned message is formatted via the plain Context API instead.
    LaunchedEffect(uiState.rewardedAdFeedback) {
        val feedback = uiState.rewardedAdFeedback ?: return@LaunchedEffect
        val message = when (feedback) {
            is RewardedAdFeedback.Earned -> context.getString(R.string.rewards_ad_earned, feedback.amount)
            RewardedAdFeedback.DismissedEarly -> adDismissedEarlyMessage
            RewardedAdFeedback.NotAvailable -> adNotAvailableMessage
        }
        snackbarHostState.showSnackbar(message)
        onEvent(FeedEvent.ConsumeRewardedAdFeedback)
    }

    // Every-5th-swipe interstitial trigger. Runs once per rising edge of showInterstitial;
    // VerticalReelsPager reacts to the same flag itself to pause/resume video around it.
    LaunchedEffect(uiState.showInterstitial) {
        if (uiState.showInterstitial) {
            val currentActivity = activity
            if (currentActivity != null) {
                onEvent(FeedEvent.ShowInterstitialAd(currentActivity))
            } else {
                // No Activity in this composition (shouldn't normally happen) - don't leave
                // playback paused waiting for an ad that can never be shown.
                onEvent(FeedEvent.DismissInterstitialTrigger)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalReelsPager(
            videos = uiState.videos,
            playerManager = playerManager,
            insufficientCoins = uiState.insufficientCoins,
            showInterstitial = uiState.showInterstitial,
            onLoadMore = { onEvent(FeedEvent.LoadMoreVideos) },
            onEvent = onEvent
        )

        if (!isCategoryView) {
            // Coin Balance Indicator for Home
            Surface(
                onClick = onCoinClick,
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFF5C542),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.coinBalance} Coins",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Category View Header (Back Button)
            IconButton(
                onClick = { onBackClick?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Out of Coins Overlay
        if (uiState.insufficientCoins) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFFF5C542),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.rewards_out_of_coins),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.rewards_out_of_coins_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onEvent(FeedEvent.ToggleAdConfirmation(true))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.rewards_watch_ad))
                    }
                }
            }
        }
        
        if (uiState.showAdConfirmation) {
            AdConfirmationDialog(
                isAdReady = uiState.isRewardedAdReady,
                onConfirm = {
                    onEvent(FeedEvent.ToggleAdConfirmation(false))
                    val currentActivity = activity
                    if (currentActivity != null) {
                        onEvent(FeedEvent.WatchRewardedAd(currentActivity))
                    } else {
                        // No Activity in this composition (shouldn't normally happen) -
                        // surface the same "not available" feedback rather than doing nothing.
                        scope.launch {
                            snackbarHostState.showSnackbar(adNotAvailableMessage)
                        }
                    }
                },
                onDismiss = {
                    onEvent(FeedEvent.ToggleAdConfirmation(false))
                }
            )
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
