package reelsdrama.freedrama.videosdrama.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DramaRed,
    background = DramaDark,
    surface = DramaSurface,
    onPrimary = DramaOnDark,
    onBackground = DramaOnDark,
    onSurface = DramaOnDark,
    secondary = DramaGray,
)

private val LightColorScheme = lightColorScheme(primary = DramaRed, secondary = DramaGray)

@Composable
fun FreeDramaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = FreeDramaTypography,
        shapes = FreeDramaShapes,
        content = content,
    )
}
