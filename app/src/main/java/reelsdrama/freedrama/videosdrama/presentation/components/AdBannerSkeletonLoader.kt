package reelsdrama.freedrama.videosdrama.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants

/**
 * Skeleton loading placeholder for a banner ad slot, shown by [AdBannerView] while a banner
 * is genuinely still loading - never after a failed load, which collapses that slot to zero
 * height exactly as before (see [AdBannerView] for that gating logic; this composable has no
 * opinion on when it's shown, only on what it looks like).
 *
 * Deliberately reserves [modifier]'s size (callers pass the expected ad size - e.g. 320x50 for
 * a fixed [com.google.android.libraries.ads.mobile.sdk.banner.AdSize.BANNER] slot, or the
 * computed adaptive size for a full-width slot) so swapping this out for the real ad once it
 * loads causes no layout jump.
 *
 * Not clickable - no `clickable` modifier, no ripple - this is a pure loading indicator. Carries
 * a small muted "Ad" label so a reserved-but-still-loading ad slot doesn't read as if it's
 * already live content, per Google's guidance on identifying ad space as ad space.
 *
 * No existing generic shimmer utility fit this: [reelsdrama.freedrama.videosdrama.core.player.components.VideoShimmer]
 * is a full-screen, hardcoded-dark video placeholder (fillMaxSize, `Color.Black`/`DarkGray`
 * only), not a themed, arbitrarily-sized card. This reuses the same animation *technique* -
 * `rememberInfiniteTransition` driving a `Brush.linearGradient` sweep - rather than a second,
 * unrelated mechanism.
 */
@Composable
fun AdBannerSkeletonLoader(modifier: Modifier = Modifier) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val blockColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val sweepColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(SKELETON_CORNER_RADIUS))
            .background(baseColor)
            .border(1.dp, borderColor, RoundedCornerShape(SKELETON_CORNER_RADIUS))
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bandWidthPx = with(density) { SHIMMER_BAND_WIDTH.toPx() }

        // Diagonal light sweep: a translucent band whose start/end offsets both slide left to
        // right in lockstep, looping every AD_SKELETON_SHIMMER_DURATION_MS. Sized off this
        // card's own measured width/height (via BoxWithConstraints) rather than a fixed
        // pixel range, so the sweep reads as a clean band on any card size instead of a slow
        // wash on a small one or a too-narrow flicker on a wide one.
        val transition = rememberInfiniteTransition(label = "ad_skeleton_shimmer")
        val sweepOffset by transition.animateFloat(
            initialValue = -bandWidthPx,
            targetValue = widthPx + bandWidthPx,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    AnimationConstants.AD_SKELETON_SHIMMER_DURATION_MS.toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "sweep"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, sweepColor, Color.Transparent),
                        start = Offset(sweepOffset - bandWidthPx, 0f),
                        end = Offset(sweepOffset + bandWidthPx, heightPx)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon-placeholder block
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(blockColor)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // Title/subtitle placeholder bars
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(blockColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(blockColor.copy(alpha = blockColor.alpha * 0.7f))
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ad",
                color = labelColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val SKELETON_CORNER_RADIUS: Dp = 10.dp
private val SHIMMER_BAND_WIDTH: Dp = 60.dp
