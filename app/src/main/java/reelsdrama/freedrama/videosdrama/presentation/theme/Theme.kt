package reelsdrama.freedrama.videosdrama.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun FreeDramaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "Default",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            val palette = when (accentColor) {
                "Blue" -> AccentPalettes.Blue
                "Purple" -> AccentPalettes.Purple
                "Pink" -> AccentPalettes.Pink
                "Green" -> AccentPalettes.Green
                "Orange" -> AccentPalettes.Orange
                "Red" -> AccentPalettes.Red
                else -> AccentPalettes.Default
            }
            darkColorScheme(
                primary = palette.primary,
                secondary = palette.secondary,
                tertiary = palette.tertiary,
                primaryContainer = palette.primaryContainer,
                onPrimary = palette.onPrimary,
                onPrimaryContainer = palette.onPrimaryContainer,
                background = DramaDark,
                surface = DramaSurface,
                onBackground = DramaOnDark,
                onSurface = DramaOnDark,
            )
        }
        else -> {
            val palette = when (accentColor) {
                "Blue" -> AccentPalettes.Blue
                "Purple" -> AccentPalettes.Purple
                "Pink" -> AccentPalettes.Pink
                "Green" -> AccentPalettes.Green
                "Orange" -> AccentPalettes.Orange
                "Red" -> AccentPalettes.Red
                else -> AccentPalettes.Default
            }
            lightColorScheme(
                primary = palette.primary,
                secondary = palette.secondary,
                tertiary = palette.tertiary,
                primaryContainer = palette.primaryContainer,
                onPrimary = palette.onPrimary,
                onPrimaryContainer = palette.onPrimaryContainer,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FreeDramaTypography,
        shapes = FreeDramaShapes,
        content = content,
    )
}
