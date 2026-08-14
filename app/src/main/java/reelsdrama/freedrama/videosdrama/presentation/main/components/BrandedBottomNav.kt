package reelsdrama.freedrama.videosdrama.presentation.main.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.presentation.navigation.BottomNavItem
import reelsdrama.freedrama.videosdrama.presentation.theme.BrandGradientColors

/**
 * The app's bottom navigation bar, styled as a floating rounded "pill" (per the brand
 * redesign) rather than [androidx.compose.material3.NavigationBar]'s flat full-width default.
 * Same 4 items/routes/click behavior as before - this only replaces the visual shell.
 *
 * The active tab gets an animated pill filled with [BrandGradientColors] - the same
 * pink-to-purple gradient used for the splash logo - that slides between tabs via
 * [animateDpAsState] rather than snapping. Inactive tabs are a plain muted icon + label.
 *
 * @param isSelected Mirrors `MainScreen`'s existing `currentDestination?.hierarchy?.any { ... }`
 * check - passed in rather than reimplemented here so this composable stays nav-framework
 * agnostic.
 */
@Composable
fun BrandedBottomNav(
    items: List<BottomNavItem>,
    isSelected: (BottomNavItem) -> Boolean,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        // Same container color NavigationBarDefaults uses today - restyling the shape/active
        // state below, not the bar's base color.
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            // Explicit bounded height: the Row's content would otherwise wrap-size this fine
            // on its own, but the indicator Box below needs a real height constraint to
            // fillMaxHeight() *against* - left unbounded, it inherits the Scaffold bottomBar
            // slot's loose/near-infinite height and stretches the whole gradient pill down
            // the entire screen instead of just behind one row of icons.
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_CONTENT_HEIGHT)
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / items.size
            // -1 (no item matches `isSelected`, e.g. a route outside these 4 tabs) means
            // nothing to slide the pill under - purely a "don't draw a misleading indicator"
            // guard, not a change to which tab reports as selected (that's `isSelected`,
            // passed in unchanged from MainScreen).
            val selectedIndex = items.indexOfFirst(isSelected)
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0),
                animationSpec = tween(durationMillis = 300),
                label = "nav_indicator_offset"
            )

            // Animated gradient pill, positioned behind the Row of items below and slid into
            // place under whichever tab is currently selected.
            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(BrandGradientColors))
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    val selected = isSelected(item)
                    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                // No ripple - the sliding gradient pill above is the only
                                // active-state affordance this design wants.
                                indication = null
                            ) { onItemClick(item) }
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Height of the item row (icon + label) inside the pill, excluding the floating margin. */
private val NAV_CONTENT_HEIGHT = 72.dp
