package reelsdrama.freedrama.videosdrama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import reelsdrama.freedrama.videosdrama.presentation.navigation.AppNavigation
import reelsdrama.freedrama.videosdrama.presentation.theme.FreeDramaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeDramaTheme {
                AppNavigation()
            }
        }
    }
}
