package reelsdrama.freedrama.videosdrama.presentation.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.presentation.rewards.components.*
import reelsdrama.freedrama.videosdrama.presentation.rewards.viewmodel.RewardsViewModel

/**
 * The final production Rewards screen.
 * Simplified for virtual in-app engagement coins only.
 */
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

            // Section: Watch & Earn (Ads Placeholder)
            item {
                val adsComingSoon = stringResource(R.string.rewards_ads_coming_soon)
                Text(
                    text = stringResource(R.string.rewards_watch_earn),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                RewardedAdPlaceholderCard(
                    onWatchAdClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = adsComingSoon,
                                duration = SnackbarDuration.Short
                            )
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
