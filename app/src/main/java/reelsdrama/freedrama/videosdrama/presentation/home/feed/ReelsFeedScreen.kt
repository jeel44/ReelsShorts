package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView
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

    // Column, not a single full-size Box: the video area (VerticalReelsPager + all its
    // overlays) gets weight(1f) and the banner - Home only - sits below it as a real
    // sibling, not layered on top. This screen is a full-bleed vertical video feed where
    // VideoInfoOverlay (caption/hashtags/music) sits flush against the bottom edge at full
    // width and VideoSideActionBar occupies the bottom-right - there is no empty strip at
    // the true bottom of the video itself to overlay a banner into without covering one of
    // those. Reserving real layout space below the video (rather than overlaying) is the
    // only placement that guarantees zero overlap. AdBannerView still collapses to zero
    // height whenever nothing is loaded, so the video stays genuinely full-bleed until a
    // banner actually has something to show, and only then does the video area shrink by
    // the banner's height to make room.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            VerticalReelsPager(
                videos = uiState.videos,
                playerManager = playerManager,
                insufficientCoins = uiState.insufficientCoins,
                nativeAdUnitId = AdConstants.NATIVE_FEED_UNIT_ID,
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

            // Out of Coins: a single AdConfirmationDialog is the entire prompt (see
            // FeedViewModel.observeRewardsData - showAdConfirmation mirrors insufficientCoins
            // directly, so no separate overlay/tap is needed to reveal it). VerticalReelsPager
            // already reads uiState.insufficientCoins directly to disable swiping and pause
            // playback, independent of this dialog's visibility, so the feed stays gated even
            // once the dialog itself closes on CTA tap.
            if (uiState.showAdConfirmation) {
                AdConfirmationDialog(
                    rewardAmount = uiState.coinUnlockRewardAmount,
                    isAdReady = uiState.isRewardedAdReady,
                    onConfirm = {
                        // Unconditional, not gated on the ad actually succeeding - this is what
                        // makes it safe for the dialog itself to have no Cancel/dismiss path of
                        // its own (see AdConfirmationDialog's doc comment). The dialog is
                        // already closed by the time WatchRewardedAd's outcome is known.
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
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (!isCategoryView) {
            // Home only, matching the coin-balance-indicator-vs-back-button split above.
            // Sits below the weighted video Box, not layered on it - see the comment on the
            // outer Column for why. Fixed AdSize.BANNER (320x50), not AdBannerView's default
            // adaptive full-width size - this screen is a full-bleed video feed, so a
            // small/compact banner belongs here instead of a dominant full-width one.
            // AdBannerView itself logs the actual load attempt/success/failure under the
            // "AdDebug" tag; this line just confirms the Home banner call site is reached
            // with the size it's asking for.
            Log.d("AdDebug", "ReelsFeedScreen (Home): requesting banner ad, adSize=BANNER (320x50)")
            AdBannerView(
                adUnitId = AdConstants.BANNER_FALLBACK_UNIT_ID,
                isInitialized = uiState.isAdInitialized,
                modifier = Modifier.background(Color.Black),
                adSize = AdSize.BANNER
            )
        }
    }
}
