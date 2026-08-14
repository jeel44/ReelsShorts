package reelsdrama.freedrama.videosdrama.presentation.rewards

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.core.ads.RewardedAdFeedback
import reelsdrama.freedrama.videosdrama.presentation.rewards.components.*
import reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel.RewardsViewModel

/**
 * The final production Rewards screen.
 * Simplified for virtual in-app engagement coins only.
 */
@Composable
fun RewardsScreen(
    onBackClick: () -> Unit,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val adDismissedEarlyMessage = stringResource(R.string.rewards_ad_dismissed_early)
    val adNotAvailableMessage = stringResource(R.string.rewards_ad_not_available)

    // The system back gesture/button - this screen has no top-bar back icon of its own. Note
    // there's a second, separate exit path that also needs the same ad-then-navigate treatment:
    // tapping "Home" directly in the bottom nav while on this screen, wired in MainScreen.kt
    // (it calls navigate() straight from the nav bar, entirely outside this composable). Both
    // paths funnel through the same RewardsViewModel.onExitRewards - see its doc comment,
    // including for the re-entrancy guard that covers both together, not just repeated
    // back-presses.
    BackHandler {
        viewModel.onExitRewards(activity, onBackClick)
    }

    // Show one-shot feedback for the last rewarded-ad attempt, then clear it so it doesn't
    // reappear on recomposition/rotation. stringResource() can't be called from this suspend
    // (non-@Composable) block, so the earned message is formatted via the plain Context API.
    LaunchedEffect(uiState.rewardedAdFeedback) {
        val feedback = uiState.rewardedAdFeedback ?: return@LaunchedEffect
        val message = when (feedback) {
            is RewardedAdFeedback.Earned -> context.getString(R.string.rewards_ad_earned, feedback.amount)
            RewardedAdFeedback.DismissedEarly -> adDismissedEarlyMessage
            RewardedAdFeedback.NotAvailable -> adNotAvailableMessage
        }
        snackbarHostState.showSnackbar(message)
        viewModel.onConsumeRewardedAdFeedback()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                RewardHeader(
                    onHistoryClick = { viewModel.onToggleHistory(true) }
                )
            }

            // Section 1: Coin Wallet
            item {
                val adsComingSoon = stringResource(R.string.rewards_ads_coming_soon)
                RewardBalanceCard(
                    balance = uiState.coinBalance,
                    onEarnClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = adsComingSoon,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }

            // Section 2: Daily Check-in
            item {
                DailyCheckInCard(
                    rewards = uiState.dailyRewards,
                    isClaimedToday = uiState.isClaimedToday,
                    currentStreak = uiState.currentStreak,
                    onClaimClick = { viewModel.onClaimDailyReward() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section: Watch & Earn
            item {
                Text(
                    text = stringResource(R.string.rewards_watch_earn),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                RewardedAdPlaceholderCard(
                    isAdReady = uiState.isRewardedAdReady,
                    onWatchAdClick = {
                        val currentActivity = activity
                        if (currentActivity != null) {
                            viewModel.onWatchRewardedAd(currentActivity)
                        } else {
                            // No Activity in this composition (shouldn't normally happen) -
                            // surface the same "not available" feedback rather than doing nothing.
                            scope.launch {
                                snackbarHostState.showSnackbar(adNotAvailableMessage)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Footer / More Info
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "More Rewards Coming Soon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (uiState.showHistory) {
        CoinActivitySheet(
            activities = uiState.activities,
            onDismissRequest = { viewModel.onToggleHistory(false) }
        )
    }
}
