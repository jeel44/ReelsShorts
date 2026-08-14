package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.presentation.theme.BrandGradientColors

/**
 * "Reward preview" out-of-coins dialog - shown via [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedUiState.showAdConfirmation]
 * whenever the user's coin balance hits 0 (see [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedViewModel.observeRewardsData]).
 *
 * Deliberately has no Cancel/dismiss affordance anywhere - no button, no back-press, no
 * scrim-tap (see [Dialog]'s `properties` below). The CTA is the only way this closes. That's
 * safe rather than a soft-lock because of how the call site handles it: [reelsdrama.freedrama.videosdrama.presentation.home.feed.ReelsFeedScreen]
 * dismisses [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedUiState.showAdConfirmation]
 * unconditionally the instant [onConfirm] fires - *before* [reelsdrama.freedrama.videosdrama.core.ads.RewardedAdManager.show]'s
 * outcome is even known - so a failed-to-show or unavailable ad can never leave this dialog
 * stuck on screen with nothing to tap; the user just doesn't get coins from that attempt (a
 * snackbar says so, driven by [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedUiState.rewardedAdFeedback])
 * and the dialog naturally reopens next time a swipe is blocked, same as any other zero-balance
 * occasion.
 *
 * @param rewardAmount The coins this ad actually grants, sourced live from the loaded ad's own
 * `RewardItem.amount` (see [reelsdrama.freedrama.videosdrama.core.ads.RewardedAdManager.getRewardAmount]
 * and [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedUiState.coinUnlockRewardAmount])
 * - never a hardcoded display number that could drift from what's actually granted.
 * @param isAdReady Whether a rewarded ad is currently loaded and ready to show. While false,
 * the CTA is disabled and shows a loading label instead, since there's no ad to show yet.
 */
@Composable
fun AdConfirmationDialog(
    rewardAmount: Int,
    isAdReady: Boolean,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {}, // No-op - the properties below are the real block, this is just
        // defensive belt-and-suspenders in case anything else ever calls it.
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(CARD_CORNER_RADIUS),
            color = CARD_BACKGROUND_COLOR,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.rewards_out_of_coins),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                RewardPreviewBlock(rewardAmount = rewardAmount)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.rewards_out_of_coins_desc),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                ClaimCoinsButton(
                    rewardAmount = rewardAmount,
                    isAdReady = isAdReady,
                    onClick = onConfirm
                )
            }
        }
    }
}

/**
 * The rounded, brand-gradient-tinted block previewing the exact coin reward - a coin icon next
 * to "+[rewardAmount]" rendered in a warm gold gradient, so the user sees precisely what
 * tapping the CTA below is worth before they commit to watching anything.
 */
@Composable
private fun RewardPreviewBlock(rewardAmount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(REWARD_BLOCK_CORNER_RADIUS))
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandGradientColors.first().copy(alpha = REWARD_BLOCK_TINT_ALPHA),
                        BrandGradientColors.last().copy(alpha = REWARD_BLOCK_TINT_ALPHA)
                    )
                )
            )
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            tint = REWARD_GOLD_GRADIENT.first(),
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "+$rewardAmount",
            style = TextStyle(
                brush = Brush.linearGradient(REWARD_GOLD_GRADIENT),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

/**
 * The full-width, brand-gradient "Claim +N Coins" CTA - the dialog's only interactive element.
 *
 * The moving highlight is not a second layer on top - three earlier attempts (a rotated,
 * clipped translucent band, at various widths/angles/gradient-stop spreads) all rendered as a
 * visibly faceted/crumpled patch instead of a soft sweep, because a rotated rectangle's own
 * hard geometric corners kept intersecting the clip bounds no matter how the gradient inside it
 * was softened. This sidesteps that whole class of bug: [sweepOffset] instead drives a moving
 * color stop *inside the same gradient brush* that paints the button's base color - there is no
 * separate shape being rotated or clipped, so there is nothing left to facet.
 */
@Composable
private fun ClaimCoinsButton(
    rewardAmount: Int,
    isAdReady: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "claim_button_sheen")
    // Starts/ends outside [0f, 1f] on purpose so the highlight visibly enters from and exits
    // past the button's edges, rather than popping in already at full strength - buildButtonBrush
    // below handles that out-of-[0,1] range explicitly (see its own doc comment) instead of just
    // clamping it, which would otherwise flash the button to a single solid color whenever the
    // sweep is off-bounds.
    val sweepOffset by transition.animateFloat(
        initialValue = -SHEEN_STOP_SPREAD,
        targetValue = 1f + SHEEN_STOP_SPREAD,
        animationSpec = infiniteRepeatable(
            animation = tween(SHEEN_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepOffset"
    )

    val baseStart = if (isAdReady) BrandGradientColors.first() else BrandGradientColors.first().copy(alpha = 0.4f)
    val baseEnd = if (isAdReady) BrandGradientColors.last() else BrandGradientColors.last().copy(alpha = 0.4f)
    val highlight = if (isAdReady) SHEEN_COLOR else SHEEN_COLOR.copy(alpha = SHEEN_COLOR.alpha * 0.4f)

    Button(
        onClick = onClick,
        enabled = isAdReady,
        shape = CLAIM_BUTTON_SHAPE,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(CLAIM_BUTTON_HEIGHT)
            .clip(CLAIM_BUTTON_SHAPE)
            .background(buildButtonBrush(sweepOffset, baseStart, highlight, baseEnd))
    ) {
        Text(
            text = if (isAdReady) {
                stringResource(R.string.ad_confirmation_claim_cta, rewardAmount)
            } else {
                stringResource(R.string.rewards_ad_loading)
            },
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The button's diagonal base-color gradient (`baseStart` -> `baseEnd`) with a soft `highlight`
 * stop riding along it at [sweepOffset] (fractional position across the gradient, not clamped
 * to [0,1] by the caller - see [ClaimCoinsButton]).
 *
 * When [sweepOffset] (plus/minus [SHEEN_STOP_SPREAD]) doesn't actually overlap [0f, 1f] - i.e.
 * the sweep is currently off past one edge of the button - this returns the plain two-stop base
 * gradient with no highlight stops at all, rather than clamping them to the boundary. Clamping
 * would collapse 3 distinct stops onto the same position (0f or 1f) while their colors still
 * differ, which flashes the *entire* button to one flat color for that portion of the loop
 * instead of leaving the ordinary base gradient visible underneath.
 */
private fun buildButtonBrush(
    sweepOffset: Float,
    baseStart: Color,
    highlight: Color,
    baseEnd: Color
): Brush {
    val leftPos = sweepOffset - SHEEN_STOP_SPREAD
    val rightPos = sweepOffset + SHEEN_STOP_SPREAD
    val stops = if (rightPos <= 0f || leftPos >= 1f) {
        arrayOf(0f to baseStart, 1f to baseEnd)
    } else {
        arrayOf(
            0f to baseStart,
            leftPos.coerceIn(0f, 1f) to baseStart,
            sweepOffset.coerceIn(0f, 1f) to highlight,
            rightPos.coerceIn(0f, 1f) to baseEnd,
            1f to baseEnd
        )
    }
    return Brush.linearGradient(colorStops = stops, start = Offset.Zero, end = Offset.Infinite)
}

private val CARD_BACKGROUND_COLOR = Color(0xFF17121C)
private val CARD_CORNER_RADIUS: Dp = 22.dp
private val REWARD_BLOCK_CORNER_RADIUS: Dp = 16.dp
private const val REWARD_BLOCK_TINT_ALPHA = 0.12f
private val REWARD_GOLD_GRADIENT = listOf(Color(0xFFFFD57A), Color(0xFFFF9F5A))

private val CLAIM_BUTTON_SHAPE = RoundedCornerShape(14.dp)
private val CLAIM_BUTTON_HEIGHT: Dp = 52.dp

private const val SHEEN_DURATION_MS = 2200
// Fractional distance (of the gradient's own 0..1 span) from the highlight's peak to where it's
// fully back to the base color on either side - i.e. how wide/soft the sweep reads. Widen this
// if it still looks like a sharp line rather than a gentle wash once rendered.
private const val SHEEN_STOP_SPREAD = 0.2f
private val SHEEN_COLOR = Color.White.copy(alpha = 0.28f)
