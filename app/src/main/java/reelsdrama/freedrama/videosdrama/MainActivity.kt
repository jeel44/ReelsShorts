package reelsdrama.freedrama.videosdrama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import reelsdrama.freedrama.videosdrama.domain.settings.SettingsRepository
import reelsdrama.freedrama.videosdrama.presentation.navigation.AppNavigation
import reelsdrama.freedrama.videosdrama.presentation.theme.FreeDramaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
