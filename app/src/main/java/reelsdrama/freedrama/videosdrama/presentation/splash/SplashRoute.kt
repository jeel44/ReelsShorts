package reelsdrama.freedrama.videosdrama.presentation.splash

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.Build
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.presentation.theme.InstrumentSerif

/**
 * "Cliffhanger" splash - visual layer only. Navigation is still driven by
 * [SplashViewModel.startTimer]/[SplashViewModel.navigationEvent], NOT a hardcoded delay here, so
 * the cold-start app-open-ad gating in the ViewModel (SPLASH_DELAY_MS + ad-ready wait + ad
 * show/dismiss) keeps working unchanged. This composable's own animation timeline (typed subtitle
 * -> hard cut -> wordmark/rule/kicker) is a fixed, independent sequence that is NOT tied to when
 * navigation actually happens - the two are intentionally decoupled, same as the previous
 * "Editorial fade" splash this replaces.
 */
@Composable
fun SplashRoute(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.startTimer(activity)

        viewModel.navigationEvent.collectLatest {
            onNavigateToHome()
        }
    }

    SplashScreenContent()
}

@Composable
private fun SplashScreenContent() {
    val density = LocalDensity.current
    val context = LocalContext.current

    // Accessibility "Remove animations" - system-wide animator duration scale of 0. Mirrors the
    // check StoryCard.kt's StoryTextContent already uses for the same purpose: when set, skip
    // straight to the final resting frame (wordmark + rule + kicker, no typing/cut/fades).
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    var charsRevealed by remember { mutableIntStateOf(0) }
    // Flips once, instantly (no animation) - see the LaunchedEffect below. Starts true under
    // reduced motion so the subtitle/glow phase never composes at all.
    var cutTriggered by remember { mutableStateOf(reduceMotion) }

    val wordmarkOffsetPx = remember { Animatable(with(density) { WORDMARK_RISE.toPx() }) }
    val wordmarkBlurDp = remember { Animatable(WORDMARK_START_BLUR_DP) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val ruleWidthDp = remember { Animatable(0f) }
    val kickerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            wordmarkOffsetPx.snapTo(0f)
            wordmarkBlurDp.snapTo(0f)
            wordmarkAlpha.snapTo(1f)
            ruleWidthDp.snapTo(RULE_WIDTH_DP)
            kickerAlpha.snapTo(1f)
            return@LaunchedEffect
        }

        // 2-3. Type the subtitle line character by character, then append the em dash and hold.
        for (i in 1..DIALOGUE_LINE.length) {
            charsRevealed = i
            delay(TYPE_CHAR_MS)
        }
        delay(EM_DASH_HOLD_MS)

        // 4. Hard cut. Flipping this boolean removes the subtitle + scene glow on the very next
        // recomposition/frame (well under the ~60ms budget) - deliberately NOT an animated fade,
        // that abruptness is the whole point of this concept.
        cutTriggered = true

        // 5. A beat of plain black before the wordmark appears.
        delay(BLACK_HOLD_MS)

        // 6-8. Wordmark fades/rises/settles in; the rule and kicker follow on their own delays,
        // all running concurrently off the same start point, same shape as the old per-letter
        // wordmark reveal this replaces.
        val wordmarkSpec = tween<Float>(durationMillis = WORDMARK_REVEAL_MS, easing = SETTLE_EASING)
        coroutineScope {
            launch { wordmarkOffsetPx.animateTo(0f, wordmarkSpec) }
            launch { wordmarkBlurDp.animateTo(0f, wordmarkSpec) }
            launch { wordmarkAlpha.animateTo(1f, wordmarkSpec) }
            launch {
                delay(RULE_DELAY_MS)
                ruleWidthDp.animateTo(
                    targetValue = RULE_WIDTH_DP,
                    animationSpec = tween(durationMillis = RULE_DURATION_MS, easing = SETTLE_EASING)
                )
            }
            launch {
                delay(KICKER_DELAY_MS)
                kickerAlpha.animateTo(1f, tween(durationMillis = KICKER_FADE_MS))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        if (!cutTriggered) {
            SceneGlow()
        }

        if (!cutTriggered) {
            BurnedInSubtitle(
                text = DIALOGUE_LINE.take(charsRevealed) + (if (charsRevealed >= DIALOGUE_LINE.length) "—" else ""),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedWordmark(
                offsetY = wordmarkOffsetPx.value,
                blurDp = wordmarkBlurDp.value,
                alpha = wordmarkAlpha.value
            )

            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .width(ruleWidthDp.value.dp)
                    .height(1.dp)
                    .background(RULE_COLOR)
            )

            Text(
                text = KICKER_TEXT,
                fontFamily = FontFamily.Default,
                fontSize = 8.5.sp,
                letterSpacing = 0.42.em,
                color = KICKER_COLOR,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .graphicsLayer { this.alpha = kickerAlpha.value }
            )
        }

        FilmGrainVignette(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Faint radial glow toward the lower third, behind where [BurnedInSubtitle] sits - only ever
 * composed pre-[cutTriggered]; it and the subtitle disappear together on the hard cut.
 */
@Composable
private fun SceneGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to GLOW_COLOR.copy(alpha = 0.16f),
                    0.6f to Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.86f),
                radius = size.maxDimension * 0.7f
            )
        )
    }
}

/**
 * The typed dialogue line, styled to read as a real burned-in video subtitle rather than app
 * chrome: pure white, semi-bold sans, small, with a tight drop shadow so it holds up over the
 * [SceneGlow] behind it. Positioned near the bottom edge (not vertically centered) to match where
 * subtitles actually sit in the short-drama vertical video this app plays.
 */
@Composable
private fun BurnedInSubtitle(text: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val shadow = remember(density) {
        Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = with(density) { Offset(0f, 1.dp.toPx()) },
            blurRadius = with(density) { 3.dp.toPx() }
        )
    }

    Text(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            shadow = shadow
        ),
        textAlign = TextAlign.Center,
        modifier = modifier.padding(start = 28.dp, end = 28.dp, bottom = SUBTITLE_BOTTOM_INSET)
    )
}

/**
 * The "Free Drama" wordmark as a single block (unlike the old "Editorial fade" splash's
 * per-letter [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]-style reveal) -
 * italic [InstrumentSerif], fading up + rising + settling out of blur in one motion. Blur requires
 * API 31+ (`Modifier.blur`); below that this falls back to offset+alpha only, same constraint as
 * the rest of this codebase's rise/blur/fade idiom.
 */
@Composable
private fun AnimatedWordmark(offsetY: Float, blurDp: Float, alpha: Float) {
    Text(
        text = "Free Drama",
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 42.sp,
        color = WORDMARK_COLOR,
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY
                this.alpha = alpha
            }
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurDp.dp)
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Cheap, static film grain + vignette, drawn once as the topmost layer. The grain is a small
 * (64x64) procedural noise [Bitmap] built once via [remember] and tiled across the screen through
 * a [BitmapShader] rather than per-pixel drawing every frame - kept deliberately cheap since this
 * composable is on the cold-start path.
 */
@Composable
private fun FilmGrainVignette(modifier: Modifier = Modifier) {
    val grainBrush = remember { NoiseShaderBrush(createNoiseBitmap()) }

    Box(
        modifier = modifier.drawWithCache {
            onDrawBehind {
                // Vignette: transparent center, darkening toward the edges.
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.maxDimension * 0.75f
                    )
                )
                drawRect(brush = grainBrush, alpha = GRAIN_ALPHA, blendMode = BlendMode.Overlay)
            }
        }
    )
}

/** Builds a small tileable noise bitmap once; [FilmGrainVignette] repeats it via [BitmapShader]. */
private fun createNoiseBitmap(size: Int = 64): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = Random(NOISE_SEED)
    val pixels = IntArray(size * size) {
        val gray = random.nextInt(256)
        val alpha = random.nextInt(256)
        (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap
}

/** Wraps [bitmap] in a repeating [BitmapShader] so [FilmGrainVignette] can paint it as a [Brush]. */
private class NoiseShaderBrush(private val bitmap: Bitmap) : ShaderBrush() {
    override fun createShader(size: androidx.compose.ui.geometry.Size): Shader =
        BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
}

// --- Timeline ---------------------------------------------------------------------------------

/** Step 2: the line that types out at the lower third. */
private const val DIALOGUE_LINE = "She said yes to the wrong"
private const val TYPE_CHAR_MS = 50L

/** Step 3: hold once the em dash is appended, before the hard cut. */
private const val EM_DASH_HOLD_MS = 260L

/** Step 5: beat of black between the cut and the wordmark appearing. */
private const val BLACK_HOLD_MS = 520L

/** Step 6: wordmark fade/rise/blur-settle duration. */
private const val WORDMARK_REVEAL_MS = 900
private val WORDMARK_RISE = 8.dp
private const val WORDMARK_START_BLUR_DP = 7f

/** Step 7: hairline rule - starts partway into the wordmark reveal, same easing. */
private const val RULE_DELAY_MS = 420L
private const val RULE_DURATION_MS = 700
private const val RULE_WIDTH_DP = 26f

/**
 * Step 8: kicker line. Duration isn't spelled out by the concept beyond "fading in ~620ms after
 * the wordmark" (the delay); 500ms was chosen so it lands at ~1120ms after the wordmark starts,
 * the same moment the rule finishes, rather than trailing off on its own.
 */
private const val KICKER_DELAY_MS = 620L
private const val KICKER_FADE_MS = 500
private const val KICKER_TEXT = "IT NEVER ENDS WHERE YOU WANT"

/** Same soft, overshoot-free deceleration curve [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]'s
 *  own SETTLE_EASING matches - kept here as the concept's canonical copy of the value. */
private val SETTLE_EASING = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private const val GRAIN_ALPHA = 0.05f
private const val NOISE_SEED = 20260817L
private val SUBTITLE_BOTTOM_INSET = 64.dp

private val BACKGROUND_COLOR = Color(0xFF0A0508)
private val GLOW_COLOR = Color(0xFFFF4F94)
private val WORDMARK_COLOR = Color(0xFFF2E9E4)
private val RULE_COLOR = Color(0xFFFF4F94)
private val KICKER_COLOR = Color(0xFFF2E9E4).copy(alpha = 0.42f)
