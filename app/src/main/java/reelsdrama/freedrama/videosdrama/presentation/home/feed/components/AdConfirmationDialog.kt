package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.R

/**
 * A Material 3 confirmation dialog for watching a rewarded ad.
 *
 * @param isAdReady Whether a rewarded ad is currently loaded and ready to show. While false,
 * the confirm button is disabled and shows a loading label instead, since there's no ad to
 * show yet.
 */
@Composable
fun AdConfirmationDialog(
    isAdReady: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = null,
                tint = Color(0xFFF5C542),
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.rewards_earn_50_coins),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.rewards_earn_50_coins_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isAdReady,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isAdReady) stringResource(R.string.rewards_watch_ad)
                    else stringResource(R.string.rewards_ad_loading)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
