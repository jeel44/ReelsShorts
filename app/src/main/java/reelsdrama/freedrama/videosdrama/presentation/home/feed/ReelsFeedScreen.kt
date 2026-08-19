package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.AdConfirmationDialog
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalStoriesPager
import reelsdrama.freedrama.videosdrama.presentation.theme.BrandGradientColors
import kotlinx.coroutines.launch

/**
 * The main container for the Reels Feed.
 * Displays a single continuous vertical reels feed.
 * Also supports a single-category view when categoryId is provided.
 */
@Composable
fun ReelsFeedScreen(
    uiState: FeedUiState,
    storiesUiState: StoriesUiState,
    feedTabUiState: FeedTabUiState,
    playerManager: VideoPlayerManager,
    onEvent: (FeedEvent) -> Unit,
    onStoriesEvent: (StoriesEvent) -> Unit,
    onTabSelected: (FeedTab) -> Unit,
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

    // Owned by FeedTabViewModel (see that class's doc comment for why neither FeedViewModel nor
    // StoriesViewModel is a better fit) - not local remember state any more, since it now also
    // has to react to a remote /feedConfig change forcing a tab switch, not just a user tap.
    val selectedTab = feedTabUiState.selectedTab

    // Fires playerManager.pauseAll() every time selectedTab actually changes, regardless of
    // WHY it changed - a manual FeedTabSwitcher tap (onTabSelected below) and a remote
    // /feedConfig change forcing the tab (FeedTabViewModel.applyFeedConfig) both converge on
    // this same selectedTab value, so this one LaunchedEffect is the single place this has to
    // be handled, rather than duplicating the pauseAll() call at every place selectedTab can
    // change. VerticalReelsPager isn't composed at all once Stories is selected, so nothing
    // drives its settledPage-based pause logic any more - without this, whichever video was
    // last playing keeps its audio going underneath the Stories feed. Also fires harmlessly on
    // first composition (LaunchedEffect always runs once for a key it's never seen before) -
    // pauseAll() over an empty/not-yet-populated player map is a no-op.
    LaunchedEffect(selectedTab) {
        playerManager.pauseAll()
    }

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

    // Same one-shot feedback as above, mirrored for the Stories feed's own rewarded-ad outcome.
    LaunchedEffect(storiesUiState.rewardedAdFeedback) {
        val feedback = storiesUiState.rewardedAdFeedback ?: return@LaunchedEffect
        val message = when (feedback) {
            is RewardedAdFeedback.Earned -> context.getString(R.string.rewards_ad_earned, feedback.amount)
            RewardedAdFeedback.DismissedEarly -> adDismissedEarlyMessage
            RewardedAdFeedback.NotAvailable -> adNotAvailableMessage
        }
        snackbarHostState.showSnackbar(message)
        onStoriesEvent(StoriesEvent.ConsumeRewardedAdFeedback)
    }

    // Re-syncs the out-of-coins dialog against the ACTUAL current balance every time this
    // screen's own NavBackStackEntry becomes resumed - both on first display and on every
    // return trip from another tab (e.g. Home <-> Rewards via the bottom nav's saveState/
    // restoreState tab-switch pattern, which keeps this same FeedViewModel instance alive
    // rather than recreating it). See FeedViewModel.recheckCoinBalance's doc comment for why
    // this can't just rely on the coin-balance flow re-emitting on its own. Fires for both
    // feeds unconditionally - both ViewModels stay alive regardless of which tab is showing.
    LifecycleResumeEffect(Unit) {
        onEvent(FeedEvent.ScreenResumed)
        onStoriesEvent(StoriesEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    // Single full-size Box - this used to be a Column with the video area weighted to leave
    // room for a persistent bottom banner (Home only). That banner has been removed entirely,
    // so the video area is simply full-bleed now; nothing below it needs room reserved.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // isCategoryView always wins regardless of /feedConfig: a single-category view is
        // Videos-only by construction (StoriesViewModel has no categoryId concept at all - see
        // its own doc comment), and was never reachable via the tab switcher either (that's
        // hidden below whenever isCategoryView is true, same as before). Remote-disabling
        // Videos was never meant to reach into this Videos-only sub-screen, so this branch
        // deliberately ignores feedTabUiState entirely rather than risk showing
        // VerticalStoriesPager here if storiesEnabled ever became the only enabled flag while a
        // category screen happened to be open.
        if (isCategoryView) {
            VerticalReelsPager(
                videos = uiState.videos,
                playerManager = playerManager,
                insufficientCoins = uiState.insufficientCoins,
                nativeAdUnitId = AdConstants.NATIVE_FEED_UNIT_ID,
                bannerAdUnitId = AdConstants.BANNER_FALLBACK_UNIT_ID,
                mediumRectangleAdUnitId = AdConstants.MEDIUM_RECTANGLE_UNIT_ID,
                adConfig = uiState.adConfig,
                onLoadMore = { onEvent(FeedEvent.LoadMoreVideos) },
                onEvent = onEvent
            )
        } else if (feedTabUiState.bothDisabled) {
            // Both tabs disabled remotely - a misconfiguration, not a state either pager should
            // try to render through. An explicit empty state, not a crash and not a silent
            // fallback to content /feedConfig says shouldn't be shown.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No content available right now.", color = Color.White)
            }
        } else if (selectedTab == FeedTab.Videos) {
            VerticalReelsPager(
                videos = uiState.videos,
                playerManager = playerManager,
                insufficientCoins = uiState.insufficientCoins,
                nativeAdUnitId = AdConstants.NATIVE_FEED_UNIT_ID,
                bannerAdUnitId = AdConstants.BANNER_FALLBACK_UNIT_ID,
                mediumRectangleAdUnitId = AdConstants.MEDIUM_RECTANGLE_UNIT_ID,
                adConfig = uiState.adConfig,
                onLoadMore = { onEvent(FeedEvent.LoadMoreVideos) },
                onEvent = onEvent
            )
        } else {
            VerticalStoriesPager(
                stories = storiesUiState.stories,
                insufficientCoins = storiesUiState.insufficientCoins,
                nativeAdUnitId = AdConstants.NATIVE_FEED_UNIT_ID,
                bannerAdUnitId = AdConstants.BANNER_FALLBACK_UNIT_ID,
                mediumRectangleAdUnitId = AdConstants.MEDIUM_RECTANGLE_UNIT_ID,
                adConfig = storiesUiState.adConfig,
                onLoadMore = { onStoriesEvent(StoriesEvent.LoadMoreStories) },
                onEvent = onStoriesEvent
            )
        }

        if (!isCategoryView) {
            // Videos/Stories tab switcher - only on the main Home feed, same visibility rule as
            // the coin pill below (a single-category view has neither). ALSO only when both
            // feeds are remotely enabled - with only one enabled there's nothing to switch
            // between, and the screen shows that one feed full-time instead (see the branch
            // above); with neither enabled there's nothing to switch to at all.
            if (feedTabUiState.config.videosEnabled && feedTabUiState.config.storiesEnabled) {
                FeedTabSwitcher(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 16.dp)
                )
            }

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
        // directly, so no separate overlay/tap is needed to reveal it). VerticalReelsPager/
        // VerticalStoriesPager already read their own insufficientCoins directly to disable
        // swiping, independent of this dialog's visibility, so the active feed stays gated
        // even once the dialog itself closes on CTA tap. Gated on selectedTab too - only the
        // active feed's own uiState/dialog should ever show, since the other feed's pager isn't
        // even composed right now.
        if (selectedTab == FeedTab.Videos && uiState.showAdConfirmation) {
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
        } else if (selectedTab == FeedTab.Stories && storiesUiState.showAdConfirmation) {
            AdConfirmationDialog(
                rewardAmount = storiesUiState.coinUnlockRewardAmount,
                isAdReady = storiesUiState.isRewardedAdReady,
                onConfirm = {
                    onStoriesEvent(StoriesEvent.ToggleAdConfirmation(false))
                    val currentActivity = activity
                    if (currentActivity != null) {
                        onStoriesEvent(StoriesEvent.WatchRewardedAd(currentActivity))
                    } else {
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
}

/**
 * Top-left "Videos" / "Stories" pill switcher, styled off
 * [reelsdrama.freedrama.videosdrama.presentation.main.components.BrandedBottomNav]'s gradient
 * pill for visual consistency with the rest of the brand redesign - the selected tab gets a
 * [BrandGradientColors] fill, the other stays plain text. Simplified relative to
 * [reelsdrama.freedrama.videosdrama.presentation.main.components.BrandedBottomNav] (no sliding-
 * indicator animation, no icons) since this is a 2-item in-screen switcher, not the app's
 * primary navigation. Only ever composed by [ReelsFeedScreen] when both
 * [reelsdrama.freedrama.videosdrama.domain.model.FeedConfig] flags are enabled - see that call
 * site for why.
 */
@Composable
private fun FeedTabSwitcher(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FeedTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .let { base ->
                            if (selected) {
                                base.background(Brush.linearGradient(BrandGradientColors))
                            } else {
                                base
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            // No ripple - the gradient fill itself is the active-state affordance,
                            // matching BrandedBottomNav's own choice.
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
