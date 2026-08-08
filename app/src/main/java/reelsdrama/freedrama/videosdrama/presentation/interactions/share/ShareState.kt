package reelsdrama.freedrama.videosdrama.presentation.interactions.share

import androidx.compose.runtime.Immutable

@Immutable
data class ShareState(
    val availableApps: List<ShareApp> = emptyList(),
    val showCopyLinkSnackbar: Boolean = false
)

data class ShareApp(
    val name: String,
    val packageName: String,
    val iconRes: Int? = null // For simplicity, we might use system icons or placeholders
)
