package reelsdrama.freedrama.videosdrama.presentation.settings.state

import androidx.compose.runtime.Immutable
import reelsdrama.freedrama.videosdrama.domain.settings.UserSettings

@Immutable
data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val activeDialog: SettingsDialog? = null
)

sealed interface SettingsDialog {
    data object VideoQuality : SettingsDialog
    data object DownloadQuality : SettingsDialog
    data object Theme : SettingsDialog
    data object AccentColor : SettingsDialog
    data object Language : SettingsDialog
}
