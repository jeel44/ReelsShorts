package reelsdrama.freedrama.videosdrama.presentation.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import reelsdrama.freedrama.videosdrama.R

/**
 * Instrument Serif (SIL Open Font License - full text at `app/licenses/instrument-serif-OFL.txt`,
 * font files at `res/font/`, sourced from Google's `google/fonts` OFL collection). High
 * stroke-contrast display serif used for the splash screen's "Cliffhanger" wordmark
 * ([reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute]'s `AnimatedWordmark`).
 *
 * This is the first real bundled font asset in the app - everywhere else that wants a serif
 * (e.g. [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]'s
 * `EDITORIAL_TEXT_STYLE`) still deliberately uses the generic [FontFamily.Serif] fallback rather
 * than this family; pulling those in too was out of scope for the change that added this file.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.instrument_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
)
