package reelsdrama.freedrama.videosdrama.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.domain.settings.SettingsRepository
import reelsdrama.freedrama.videosdrama.presentation.settings.state.SettingsDialog
import reelsdrama.freedrama.videosdrama.presentation.settings.state.SettingsUiState
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Handles user preference updates and coordinates with [SettingsRepository] for persistence.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        repository.getUserSettings()
            .onEach { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
            .launchIn(viewModelScope)
    }

    fun onShowDialog(dialog: SettingsDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun onAutoPlayToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoPlay(enabled) }
    }

    fun onAutoNextEpisodeToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoNextEpisode(enabled) }
    }

    fun onVideoQualityChange(quality: String) {
        viewModelScope.launch { repository.updateVideoQuality(quality) }
    }

    fun onDataSaverToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateDataSaver(enabled) }
    }

    fun onWifiOnlyDownloadsToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateWifiOnlyDownloads(enabled) }
    }

    fun onDownloadQualityChange(quality: String) {
        viewModelScope.launch { repository.updateDownloadQuality(quality) }
    }

    fun onLikesNotifToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateLikesNotification(enabled) }
    }

    fun onCommentsNotifToggle(enabled: Boolean) {
        viewModelScope.launch { repository.updateCommentsNotification(enabled) }
    }

    fun onThemeChange(theme: String) {
        viewModelScope.launch { repository.updateTheme(theme) }
    }

    fun onAccentColorChange(color: String) {
        viewModelScope.launch { repository.updateAccentColor(color) }
    }

    fun onLanguageChange(language: String) {
        viewModelScope.launch { repository.updateLanguage(language) }
    }

    fun onClearCacheClick() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }

    fun onClearCacheConfirm() {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.update { it.copy(showClearCacheDialog = false) }
            _eventFlow.emit(UiEvent.ShowSnackbar("Cache cleared successfully"))
        }
    }

    fun onClearCacheCancel() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }

    fun onRateAppClick() {
        viewModelScope.launch { _eventFlow.emit(UiEvent.RateApp) }
    }

    fun onShareAppClick() {
        viewModelScope.launch { _eventFlow.emit(UiEvent.ShareApp) }
    }

    fun onPrivacyPolicyClick() {
        viewModelScope.launch { _eventFlow.emit(UiEvent.OpenUrl("https://freedrama.app/privacy")) }
    }

    fun onTermsClick() {
        viewModelScope.launch { _eventFlow.emit(UiEvent.OpenUrl("https://freedrama.app/terms")) }
    }

    sealed interface UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent
        data object RateApp : UiEvent
        data object ShareApp : UiEvent
        data class OpenUrl(val url: String) : UiEvent
    }
}
