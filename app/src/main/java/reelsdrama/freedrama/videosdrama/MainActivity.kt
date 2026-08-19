package reelsdrama.freedrama.videosdrama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.notifications.OneSignalManager
import reelsdrama.freedrama.videosdrama.domain.settings.SettingsRepository
import reelsdrama.freedrama.videosdrama.presentation.navigation.AppNavigation
import reelsdrama.freedrama.videosdrama.presentation.theme.FreeDramaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var oneSignalManager: OneSignalManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // OneSignal's required Push Subscription Verification Dialog flow (see
        // OneSignalManager's doc comment) - MainActivity is this single-Activity app's one
        // "screen" to own the observer's setup call. Waits on isInitialized first since
        // OneSignal.initWithContext (App.onCreate) is dispatched to a background coroutine and
        // isn't guaranteed to have completed by the time this Activity is created.
        lifecycleScope.launch {
            oneSignalManager.isInitialized.first { it }
            oneSignalManager.setupPushSubscriptionObserver(this@MainActivity)
        }

        setContent {
            val userSettings by settingsRepository.getUserSettings()
                .collectAsStateWithLifecycle(initialValue = null)

            val darkTheme = when (userSettings?.appearance?.theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            val accentColor = userSettings?.appearance?.accentColor ?: "Default"

            FreeDramaTheme(
                darkTheme = darkTheme,
                accentColor = accentColor,
                dynamicColor = false
            ) {
                AppNavigation()
            }
        }
    }
}
