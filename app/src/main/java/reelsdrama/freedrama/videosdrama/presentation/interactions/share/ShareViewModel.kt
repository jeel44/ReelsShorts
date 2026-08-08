package reelsdrama.freedrama.videosdrama.presentation.interactions.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ShareRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ShareState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(availableApps = repository.getInstalledShareApps()) }
    }

    fun onEvent(event: ShareEvent) {
        when (event) {
            is ShareEvent.OnAppClick -> {
                val text = formatShareText(event.videoId, event.caption)
                repository.shareToApp(event.packageName, text)
            }
            is ShareEvent.OnCopyLink -> {
                copyToClipboard(event.videoId)
                _state.update { it.copy(showCopyLinkSnackbar = true) }
            }
            is ShareEvent.OnMoreClick -> {
                val text = formatShareText(event.videoId, event.caption)
                repository.shareMore(text)
            }
            is ShareEvent.SnackbarDismissed -> {
                _state.update { it.copy(showCopyLinkSnackbar = false) }
            }
        }
    }

    private fun formatShareText(videoId: String, caption: String): String {
        return "Check out this video on Free Drama: $caption\n\nhttps://freedrama.app/video/$videoId"
    }

    private fun copyToClipboard(videoId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Video Link", "https://freedrama.app/video/$videoId")
        clipboard.setPrimaryClip(clip)
    }
}
