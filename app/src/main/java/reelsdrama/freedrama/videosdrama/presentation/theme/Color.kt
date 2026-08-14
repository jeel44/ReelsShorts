package reelsdrama.freedrama.videosdrama.presentation.theme

import androidx.compose.ui.graphics.Color

val DramaRed = Color(0xFFE50914)
val DramaDark = Color(0xFF09090B)
val DramaSurface = Color(0xFF111113)
val DramaOnDark = Color(0xFFF8FAFC)
val DramaGray = Color(0xFF9CA3AF)

/**
 * The app's brand gradient (pink -> magenta -> purple) - originally defined inline in
 * [reelsdrama.freedrama.videosdrama.presentation.splash.components.AnimatedLogo] for the
 * splash logo. Pulled out here as the single source of truth so every other placement that
 * wants the same brand gradient (e.g. the bottom nav's active-tab pill) references these
 * exact values instead of re-guessing/duplicating the hex codes.
 */
val BrandGradientColors = listOf(Color(0xFFFF5A84), Color(0xFFFF2D7A), Color(0xFF8A2BE2))

// Accent Colors Palettes (Simplified M3 Sets)
object AccentPalettes {
    val Blue = AccentSet(
        primary = Color(0xFF005AC1),
        secondary = Color(0xFF535F70),
        tertiary = Color(0xFF6B5778),
        primaryContainer = Color(0xFFD8E2FF),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF001A41)
    )
    val Purple = AccentSet(
        primary = Color(0xFF6750A4),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
        primaryContainer = Color(0xFFEADDFF),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF21005D)
    )
    val Pink = AccentSet(
        primary = Color(0xFF984061),
        secondary = Color(0xFF705761),
        tertiary = Color(0xFF7E5244),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF3E001D)
    )
    val Green = AccentSet(
        primary = Color(0xFF006D32),
        secondary = Color(0xFF4F6354),
        tertiary = Color(0xFF3B6470),
        primaryContainer = Color(0xFF98F6AB),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF00210B)
    )
    val Orange = AccentSet(
        primary = Color(0xFF8B5000),
        secondary = Color(0xFF715A41),
        tertiary = Color(0xFF57633B),
        primaryContainer = Color(0xFFFFDCBE),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF2D1600)
    )
    val Red = AccentSet(
        primary = Color(0xFFBA1A1A),
        secondary = Color(0xFF775652),
        tertiary = Color(0xFF715B29),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF410002)
    )
    val Default = Red // Default to Red as per brand
}

data class AccentSet(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color
)
