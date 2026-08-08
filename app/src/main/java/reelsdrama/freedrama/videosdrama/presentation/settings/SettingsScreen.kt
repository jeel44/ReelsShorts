package reelsdrama.freedrama.videosdrama.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.presentation.settings.components.ConfirmDialog
import reelsdrama.freedrama.videosdrama.presentation.settings.components.SelectionDialog
import reelsdrama.freedrama.videosdrama.presentation.settings.components.SettingsClickableItem
import reelsdrama.freedrama.videosdrama.presentation.settings.components.SettingsGroup
import reelsdrama.freedrama.videosdrama.presentation.settings.components.SettingsSwitchItem
import reelsdrama.freedrama.videosdrama.presentation.settings.state.SettingsDialog
import reelsdrama.freedrama.videosdrama.presentation.settings.utils.IntentUtils
import reelsdrama.freedrama.videosdrama.presentation.settings.viewmodel.SettingsViewModel

/**
 * Main Settings screen.
 * Displays various application preferences categorized by groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.userSettings
    val snackbarHostState = remember { SnackbarHostState() }
    
    val shareAppText = stringResource(R.string.settings_share_app_text, context.packageName)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SettingsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SettingsViewModel.UiEvent.RateApp -> {
                    IntentUtils.rateApp(context)
                }
                is SettingsViewModel.UiEvent.ShareApp -> {
                    IntentUtils.shareApp(context, shareAppText)
                }
                is SettingsViewModel.UiEvent.OpenUrl -> {
                    IntentUtils.openUrl(context, event.url)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Playback
            SettingsGroup(title = stringResource(R.string.settings_playback)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_auto_play),
                    icon = Icons.Default.PlayCircle,
                    checked = settings.playback.autoPlay,
                    onCheckedChange = { viewModel.onAutoPlayToggle(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_auto_next),
                    icon = Icons.Default.SkipNext,
                    checked = settings.playback.autoNextEpisode,
                    onCheckedChange = { viewModel.onAutoNextEpisodeToggle(it) }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_video_quality),
                    icon = Icons.Default.HighQuality,
                    subtitle = settings.playback.videoQuality,
                    onClick = { viewModel.onShowDialog(SettingsDialog.VideoQuality) }
                )
            }

            // Data & Storage
            SettingsGroup(title = stringResource(R.string.settings_data_storage)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_data_saver),
                    icon = Icons.Default.DataUsage,
                    checked = settings.storage.dataSaver,
                    onCheckedChange = { viewModel.onDataSaverToggle(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_wifi_only),
                    icon = Icons.Default.Wifi,
                    checked = settings.storage.wifiOnlyDownloads,
                    onCheckedChange = { viewModel.onWifiOnlyDownloadsToggle(it) }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_download_quality),
                    icon = Icons.Default.Download,
                    subtitle = settings.storage.downloadQuality,
                    onClick = { viewModel.onShowDialog(SettingsDialog.DownloadQuality) }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_clear_cache),
                    icon = Icons.Default.DeleteSweep,
                    subtitle = stringResource(R.string.settings_used, settings.storage.cacheSize),
                    onClick = { viewModel.onClearCacheClick() }
                )
            }

            // Notifications
            SettingsGroup(title = stringResource(R.string.settings_notifications)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_likes),
                    icon = Icons.Default.Favorite,
                    checked = settings.notifications.likes,
                    onCheckedChange = { viewModel.onLikesNotifToggle(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_comments),
                    icon = Icons.Default.Comment,
                    checked = settings.notifications.comments,
                    onCheckedChange = { viewModel.onCommentsNotifToggle(it) }
                )
            }

            // Appearance
            SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_theme),
                    icon = Icons.Default.Palette,
                    subtitle = settings.appearance.theme,
                    onClick = { viewModel.onShowDialog(SettingsDialog.Theme) }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_accent_color),
                    icon = Icons.Default.ColorLens,
                    subtitle = settings.appearance.accentColor,
                    onClick = { viewModel.onShowDialog(SettingsDialog.AccentColor) }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_language),
                    icon = Icons.Default.Language,
                    subtitle = settings.appearance.language,
                    onClick = { viewModel.onShowDialog(SettingsDialog.Language) }
                )
            }

            // Privacy
            SettingsGroup(title = stringResource(R.string.settings_privacy)) {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_privacy_policy),
                    icon = Icons.Default.Policy,
                    onClick = { viewModel.onPrivacyPolicyClick() }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_terms),
                    icon = Icons.Default.Description,
                    onClick = { viewModel.onTermsClick() }
                )
            }

            // About
            SettingsGroup(title = stringResource(R.string.settings_about)) {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_version),
                    icon = Icons.Default.Info,
                    subtitle = "1.0.0 (Build 20260808)",
                    onClick = { }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_rate_app),
                    icon = Icons.Default.Star,
                    onClick = { viewModel.onRateAppClick() }
                )
                SettingsClickableItem(
                    title = stringResource(R.string.settings_share_app),
                    icon = Icons.Default.Share,
                    onClick = { viewModel.onShareAppClick() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (uiState.showClearCacheDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache),
            message = stringResource(R.string.dialog_confirm_clear_cache, settings.storage.cacheSize),
            confirmLabel = stringResource(R.string.dialog_clear),
            onConfirm = { viewModel.onClearCacheConfirm() },
            onDismiss = { viewModel.onClearCacheCancel() }
        )
    }

    uiState.activeDialog?.let { dialog ->
        when (dialog) {
            SettingsDialog.VideoQuality -> SelectionDialog(
                title = stringResource(R.string.settings_video_quality),
                options = listOf("Auto", "1080p", "720p", "480p", "360p"),
                selectedOption = settings.playback.videoQuality,
                onOptionSelected = {
                    viewModel.onVideoQualityChange(it)
                    viewModel.onDismissDialog()
                },
                onDismiss = { viewModel.onDismissDialog() }
            )
            SettingsDialog.DownloadQuality -> SelectionDialog(
                title = stringResource(R.string.settings_download_quality),
                options = listOf("High", "Medium", "Low"),
                selectedOption = settings.storage.downloadQuality,
                onOptionSelected = {
                    viewModel.onDownloadQualityChange(it)
                    viewModel.onDismissDialog()
                },
                onDismiss = { viewModel.onDismissDialog() }
            )
            SettingsDialog.Theme -> SelectionDialog(
                title = stringResource(R.string.settings_theme),
                options = listOf("Light", "Dark", "System"),
                selectedOption = settings.appearance.theme,
                onOptionSelected = {
                    viewModel.onThemeChange(it)
                    viewModel.onDismissDialog()
                },
                onDismiss = { viewModel.onDismissDialog() }
            )
            SettingsDialog.AccentColor -> SelectionDialog(
                title = stringResource(R.string.settings_accent_color),
                options = listOf("Default", "Blue", "Purple", "Pink", "Green"),
                selectedOption = settings.appearance.accentColor,
                onOptionSelected = {
                    viewModel.onAccentColorChange(it)
                    viewModel.onDismissDialog()
                },
                onDismiss = { viewModel.onDismissDialog() }
            )
            SettingsDialog.Language -> SelectionDialog(
                title = stringResource(R.string.settings_language),
                options = listOf("English", "Hindi", "Spanish", "French", "German"),
                selectedOption = settings.appearance.language,
                onOptionSelected = {
                    viewModel.onLanguageChange(it)
                    viewModel.onDismissDialog()
                },
                onDismiss = { viewModel.onDismissDialog() }
            )
        }
    }
}
