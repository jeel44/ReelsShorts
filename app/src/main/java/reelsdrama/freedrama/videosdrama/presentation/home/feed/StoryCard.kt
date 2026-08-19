package reelsdrama.freedrama.videosdrama.presentation.home.feed

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.ceil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoInfoOverlay
import reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoSideActionBar
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story
import reelsdrama.freedrama.videosdrama.presentation.interactions.comments.CommentsBottomSheet
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeAnimationOverlay
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeButtonEvent
import reelsdrama.freedrama.videosdrama.presentation.interactions.like.LikeViewModel
import reelsdrama.freedrama.videosdrama.presentation.interactions.share.ShareBottomSheet
import reelsdrama.freedrama.videosdrama.presentation.theme.BrandGradientColors

/**
 * A full-screen card representing a single text story - the Stories-feed sibling of [ReelCard].
 * Same structure (heart-animation overlay, caption/action-rail overlay, comments/share sheets),
 * reusing the exact same [VideoInfoOverlay]/[VideoSideActionBar]/[CommentsBottomSheet]/
 * [ShareBottomSheet] composables bound to [Story]'s matching fields, rather than forking new
 * ones. The only real difference from [ReelCard] is the bottom-most content layer:
 * [StoryTextContent]'s character-by-character "Typewriter" reveal of [Story.lines] instead of
 * [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer].
 *
 * @param isActivePage Whether this specific page is the pager's actual current settled page -
 * threaded down from [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalStoriesPager]
 * (`page == pagerState.settledPage`, mirroring [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager]'s
 * own play-vs-pause check). Gates [StoryTextContent]'s reveal so it only runs while this page is
 * actually the one being looked at - see that composable's doc comment for why.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCard(
    story: Story,
    isActivePage: Boolean,
    likeViewModel: LikeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val state by likeViewModel.state.collectAsState()
    val isLiked = likeViewModel.isLiked(story.id)
    val likeCount = likeViewModel.getLikeCount(story.id, story.likeCount)

    var showComments by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Bottom-most layer: the "Typewriter" character-by-character reveal of the story text
        StoryTextContent(
            story = story,
            isActivePage = isActivePage,
            onDoubleTap = { offset ->
                likeViewModel.onEvent(LikeButtonEvent.OnDoubleTap(story.id, offset))
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Heart Animation Overlay
        LikeAnimationOverlay(
            activeAnimations = state.activeAnimations,
            onAnimationFinished = { id ->
                likeViewModel.onEvent(LikeButtonEvent.AnimationFinished(id))
            }
        )

        // 3. Content Overlays (UI Info & Actions) - same composables ReelCard uses for a video
        VideoInfoOverlay(
            username = story.username,
            isVerified = story.isVerified,
            caption = story.caption,
            hashtags = story.hashtags,
            audioLabel = story.audioLabel,
            viewCount = story.viewCount,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        VideoSideActionBar(
            isLiked = isLiked,
            likeCount = likeCount,
            commentCount = story.commentCount,
            shareCount = story.shareCount,
            onLikeClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                likeViewModel.onEvent(LikeButtonEvent.OnLikeClick(story.id))
            },
            onCommentClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showComments = true
            },
            onShareClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showShare = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 8.dp)
        )

        if (showComments) {
            CommentsBottomSheet(
                videoId = story.id,
                onDismissRequest = { showComments = false }
            )
        }

        if (showShare) {
            ShareBottomSheet(
                contentId = story.id,
                caption = story.caption,
                onDismissRequest = { showShare = false }
            )
        }
    }
}

/**
 * The "Typewriter" text content layer for a story page - a single fixed [EMBER_BACKGROUND_STOPS]
 * background (no longer [Story.thumbnailGradient], which was removed - see [FakeStoryRepository]
 * and [Story] for that change) behind a character-by-character reveal of [Story.lines].
 *
 * A single `charsRevealed` counter ticks up across *all* of [Story.lines] combined (not reset
 * per line - reaching the end of one line rolls straight into the next). Characters are grouped
 * into per-word chunks (`wordChunksPerLine`) so [FlowRow] only ever wraps between words, never
 * mid-word - each word is one atomic flow item. Within a word, characters split into two
 * rendering paths:
 * - **Settled** (revealed more than [ANIMATING_TAIL_CHARS] ticks ago, so [AnimatedChar]'s own
 *   255ms settle animation has had time to finish): baked into a single [SettledText] per word -
 *   one [Text]/[buildAnnotatedString] with a solid [SpanStyle] color sampled per character from
 *   [STORY_TEXT_GRADIENT], not an animated composable. This is what actually fixes the "each
 *   glyph gets its own tiny gradient" bug: sampling a solid color per character *position within
 *   the whole line* (not per word) makes the sweep read as continuous across the whole line even
 *   though it's still physically rendered as several small per-word [Text]s rather than one
 *   [Text] for the entire line - see [SettledText]'s own doc comment for why literal one-Text-
 *   per-line wasn't used.
 * - **Still animating** (the last [ANIMATING_TAIL_CHARS] revealed characters): individual
 *   [AnimatedChar] composables with their own rise/blur/fade. The moment a character ages past
 *   the animating window, it simply stops being emitted at its `key(localIndex)` position and
 *   the growing [SettledText] absorbs it instead - no visual jump, since by then its own
 *   animation has already finished.
 *
 * The reveal is gated on [isActivePage], not just [Story.id]: `charsRevealed` only ticks while
 * this page is actually the pager's current settled page, and is reset to 0 every time
 * [isActivePage] transitions to `true` (see the `LaunchedEffect(isActivePage, story.id)` below) -
 * so returning to a story always replays it from scratch, never resumes mid-way or shows an
 * already-completed result. This matters because [VerticalStoriesPager]'s
 * `beyondViewportPageCount = 1` keeps an immediate neighbor page's composition (and therefore a
 * plain `remember(story.id)`-scoped counter) alive without disposing it while off-screen; keying
 * only off `story.id` let that counter keep ticking in the background whether or not the page
 * was ever actually being looked at. Gating on `isActivePage` instead means the reveal coroutine
 * is genuinely suspended (not just visually ignored) the moment this page stops being the
 * settled page, and cleanly restarts the moment it becomes the settled page again - mirroring
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager]'s own
 * `pagerState.settledPage`-driven play/pause check, the existing "is this the active page"
 * mechanism this codebase already uses for the video feed, rather than a new one.
 *
 * [key]\(story.id\) still wraps the reveal subtree too, forcing every remembered animation state
 * beneath it to be discarded and rebuilt if a differently-shaped story ever lands in the same
 * pager slot - a defensive belt-and-suspenders alongside the `isActivePage`-driven reset, not a
 * replacement for it.
 *
 * Double-tap-to-like is wired here (mirroring [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer]'s
 * gesture) since it's the only gesture a static text page can meaningfully support; the
 * tap-to-pause/long-press-speed gestures [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer]
 * also has don't apply here.
 */
@Composable
private fun StoryTextContent(
    story: Story,
    isActivePage: Boolean,
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Accessibility "Remove animations" - system-wide animator duration scale of 0. No existing
    // codebase pattern for this (checked - nothing else reads ANIMATOR_DURATION_SCALE), so this
    // is a new, one-off check scoped to this animation only.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val lines = remember(story.id) { story.lines }
    // Cumulative character offset (not counting the line breaks between entries) where each
    // line starts in the single combined charsRevealed count.
    val lineOffsets = remember(story.id) {
        var running = 0
        lines.map { line -> val start = running; running += line.length; start }
    }
    val totalChars = remember(story.id) { lines.sumOf { it.length } }

    // Each line split into word chunks - "word plus its own trailing whitespace" - paired with
    // the chunk's starting character offset *within its line*, so AnimatedChar's per-character
    // reveal timing is completely unaffected: this only changes how already-revealed characters
    // are grouped for layout/coloring, not when any character becomes visible.
    val wordChunksPerLine = remember(story.id) {
        lines.map { line -> Regex("\\S+\\s*").findAll(line).map { match -> match.value to match.range.first }.toList() }
    }

    var charsRevealed by remember(story.id) { mutableIntStateOf(0) }

    // Ticks charsRevealed only while this page is actually the one being looked at, and resets
    // to 0 every time isActivePage flips to true - see this function's doc comment. Cancelling/
    // relaunching on isActivePage's own transitions (not just story.id) is what actually stops
    // the ticking while off-screen, rather than merely ignoring its result.
    LaunchedEffect(isActivePage, story.id) {
        if (!isActivePage) return@LaunchedEffect

        charsRevealed = 0
        if (reduceMotion) {
            charsRevealed = totalChars
            return@LaunchedEffect
        }
        while (charsRevealed < totalChars) {
            delay(CHAR_TICK_MS)
            charsRevealed++
        }
    }

    // 0 under reduced motion: charsRevealed jumps straight to totalChars above, so there is
    // never a "still animating" tail to render - every visible character is immediately settled.
    val animatingTailCount = if (reduceMotion) 0 else ANIMATING_TAIL_CHARS

    Box(
        modifier = modifier
            .drawWithCache {
                // CSS radial-gradient(130% 90% at 70% 100%, ...): center at 70% across / 100%
                // down (bottom edge), resolved against the ACTUAL draw size here rather than a
                // guessed fixed dp offset. Brush.radialGradient is circular only - Compose has no
                // elliptical radial gradient - so the CSS's independent 130%/90% width/height
                // radii are approximated with one circular radius sized to the larger of the two
                // dimensions, so the glow's reach doesn't fall short of either CSS axis.
                val center = Offset(size.width * 0.7f, size.height * 1f)
                val radius = maxOf(size.width, size.height) * 1.1f
                val brush = Brush.radialGradient(
                    colorStops = EMBER_BACKGROUND_STOPS,
                    center = center,
                    radius = radius
                )
                onDrawBehind { drawRect(brush) }
            }
            .pointerInput(story.id) {
                detectTapGestures(onDoubleTap = onDoubleTap)
            },
        contentAlignment = Alignment.Center
    ) {
        // key(story.id): forces this whole subtree - and every remembered animation state
        // beneath it - to be discarded and rebuilt from scratch whenever the story changes, even
        // in the (unlikely but possible) case where two different stories happen to produce the
        // same line/character shape and Compose would otherwise be tempted to reuse the slot
        // positionally instead of restarting it.
        key(story.id) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // The one line still mid-reveal (if any) - null once the whole story is typed,
                // OR whenever this page isn't the active one, which is also what hides the caret
                // (see BlinkingCaret's call site below) on an off-screen neighbor page.
                val activeLineIndex = if (isActivePage) {
                    lineOffsets.indices.firstOrNull { i -> charsRevealed < lineOffsets[i] + lines[i].length }
                } else {
                    null
                }

                // Global (whole-story) cutoff: characters revealed before this point have had
                // enough ticks pass for AnimatedChar's own settle animation to have finished, so
                // they render baked into a SettledText run instead of an individual AnimatedChar
                // - computed once per recomposition, not per line, so a completed earlier line
                // never keeps a stale "still animating" tail once typing has moved past it.
                val globalSettledBoundary = (charsRevealed - animatingTailCount).coerceAtLeast(0)

                lines.forEachIndexed { lineIndex, line ->
                    val visibleCount = (charsRevealed - lineOffsets[lineIndex]).coerceIn(0, line.length)
                    if (visibleCount > 0 || lineIndex == activeLineIndex) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.Center
                        ) {
                            wordChunksPerLine[lineIndex].forEachIndexed { wordIndex, (word, startInLine) ->
                                val charsVisibleInWord = (visibleCount - startInLine).coerceIn(0, word.length)
                                if (charsVisibleInWord > 0) {
                                    val globalWordStart = lineOffsets[lineIndex] + startInLine
                                    val settledCountInWord =
                                        (globalSettledBoundary - globalWordStart).coerceIn(0, charsVisibleInWord)

                                    // One Row per word = one atomic FlowRow child, so wrapping
                                    // only ever happens between words, never inside one.
                                    key(wordIndex) {
                                        Row {
                                            if (settledCountInWord > 0) {
                                                SettledText(
                                                    text = word.substring(0, settledCountInWord),
                                                    startInLine = startInLine,
                                                    totalCharsInLine = line.length
                                                )
                                            }
                                            for (localIndex in settledCountInWord until charsVisibleInWord) {
                                                val fraction = (startInLine + localIndex).toFloat() /
                                                    (line.length - 1).coerceAtLeast(1)
                                                key(localIndex) {
                                                    AnimatedChar(
                                                        char = word[localIndex],
                                                        targetColor = sampleStoryTextGradient(fraction)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (lineIndex == activeLineIndex) {
                                BlinkingCaret()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A settled (done animating) run of characters rendered as ONE [Text] built from a
 * [buildAnnotatedString], with a solid [SpanStyle] color sampled per character from
 * [STORY_TEXT_GRADIENT] - not [TextStyle.brush]/[SpanStyle.brush]. A brush attached to this call
 * (or to [EDITORIAL_TEXT_STYLE] globally) would size its shader to *this composable's own*
 * bounds - i.e. this one word - reproducing the same "each rendering unit gets its own tiny
 * gradient" bug this whole change exists to fix, just at word granularity instead of per-glyph.
 * Sampling a solid color per character based on its position *within the whole line*
 * ([totalCharsInLine], not this word's own length) instead gives a visually continuous sweep
 * across the entire line regardless of how many separate word-[Text]s it's physically split
 * across.
 *
 * True one-[Text]-per-line (letting Compose's own paragraph layout handle wrapping, rather than
 * this per-word [Row]/[FlowRow] grouping) was considered and rejected: the live, still-animating
 * tail of characters would then need to be positioned relative to a paragraph that wraps
 * dynamically as more text is revealed, which only works reliably via
 * [androidx.compose.ui.text.TextLayoutResult]-based pixel positioning (tracking exactly where
 * the last visible glyph landed after each wrap) - meaningfully more fragile than this per-word
 * approach for a case (multi-line stories with 60-100 char lines, per [FakeStoryRepository]'s
 * seed data) where that positioning logic would be exercised constantly. Per-word [Row]s avoid
 * needing that entirely: each word is a small, effectively-never-wraps-internally atomic unit,
 * so its own settled/animating split is just ordinary sibling layout inside one [Row].
 */
@Composable
private fun SettledText(text: String, startInLine: Int, totalCharsInLine: Int, modifier: Modifier = Modifier) {
    val annotated = remember(text, startInLine, totalCharsInLine) {
        buildAnnotatedString {
            text.forEachIndexed { i, ch ->
                val fraction = (startInLine + i).toFloat() / (totalCharsInLine - 1).coerceAtLeast(1)
                withStyle(SpanStyle(color = sampleStoryTextGradient(fraction))) {
                    append(ch)
                }
            }
        }
    }
    Text(text = annotated, style = EDITORIAL_TEXT_STYLE, modifier = modifier)
}

/**
 * One still-animating character of a story's text, blurring into focus the instant it's revealed
 * - the "Typewriter" effect's premium touch. Mirrors [reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute]'s
 * `AnimatedWordmark` (same rise+blur+fade shape, same easing curve) at character granularity.
 *
 * Only ever composed for a character within the trailing [ANIMATING_TAIL_CHARS]-sized window
 * (see [StoryTextContent]'s doc comment) - once a character ages out of that window, its parent
 * simply stops emitting it at its `key(localIndex)` position (absorbing it into [SettledText]
 * instead), which disposes this composable and its `Animatable`s outright. By that point its own
 * `LaunchedEffect(Unit)` settle animation has already finished (the window is sized with a tick
 * of buffer specifically so this is true), so the disposal itself causes no visible jump - unlike
 * the prior version of this function, there's no internal `settled` flag/branch here any more,
 * since the *parent* now owns that transition rather than this composable reporting it upward.
 *
 * [targetColor] is this character's own sampled [STORY_TEXT_GRADIENT] color (by position within
 * the whole line) rather than a shared brush - see [SettledText]'s doc comment for why.
 *
 * Blur requires API 31+ (`Modifier.blur`, same constraint as [reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute]'s
 * `AnimatedWordmark`); this project's minSdk is 24, so below API 31 the blur modifier is
 * skipped entirely (translateY+alpha only) rather than relying on `Modifier.blur`'s own silent
 * no-op on unsupported API levels.
 */
@Composable
private fun AnimatedChar(char: Char, targetColor: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val startOffsetPx = remember(density) { with(density) { CHAR_RISE_DISTANCE.toPx() } }

    val offsetY = remember { Animatable(startOffsetPx) }
    val blurDp = remember { Animatable(CHAR_START_BLUR_DP) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val spec = tween<Float>(durationMillis = CHAR_SETTLE_DURATION_MS, easing = SETTLE_EASING)
        coroutineScope {
            launch { offsetY.animateTo(0f, spec) }
            launch { blurDp.animateTo(0f, spec) }
            launch { alpha.animateTo(1f, spec) }
        }
    }

    Text(
        text = char.toString(),
        color = targetColor,
        style = EDITORIAL_TEXT_STYLE,
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.value
                this.alpha = alpha.value
            }
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurDp.value.dp)
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Blinking vertical bar tracking the current typing position, filled with the same brand
 * gradient ([BrandGradientColors] - unchanged, still correct wherever else it's used, e.g.
 * [reelsdrama.freedrama.videosdrama.presentation.main.components.BrandedBottomNav]'s active-tab
 * pill) as before. Only ever composed for the one line still mid-reveal (see
 * [StoryTextContent]'s `activeLineIndex`) - it simply isn't part of the tree any more once the
 * whole story has finished typing, which is the "stop blinking once fully revealed" behavior for
 * free, with no extra state needed here.
 */
@Composable
private fun BlinkingCaret(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "story_caret_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CARET_BLINK_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "story_caret_alpha"
    )

    Box(
        modifier = modifier
            .padding(start = 2.dp)
            .size(width = CARET_WIDTH, height = CARET_HEIGHT)
            .graphicsLayer { this.alpha = alpha }
            .background(Brush.verticalGradient(BrandGradientColors))
    )
}

/** Linearly interpolates through [STORY_TEXT_GRADIENT]'s 3 stops at [fraction] (clamped 0f..1f). */
private fun sampleStoryTextGradient(fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return if (t <= 0.5f) {
        lerp(STORY_TEXT_GRADIENT[0], STORY_TEXT_GRADIENT[1], t / 0.5f)
    } else {
        lerp(STORY_TEXT_GRADIENT[1], STORY_TEXT_GRADIENT[2], (t - 0.5f) / 0.5f)
    }
}

/**
 * Pink -> hot pink -> a lighter orchid/lavender for the story text's gradient - distinct from
 * [BrandGradientColors] (that constant is untouched and still used as-is elsewhere, e.g.
 * [BlinkingCaret] above). Lifted lighter than the raw brand violet specifically so text stays
 * legible against [EMBER_BACKGROUND_STOPS]'s dark background rather than the violet disappearing
 * into it.
 */
private val STORY_TEXT_GRADIENT = listOf(Color(0xFFFF7A9C), Color(0xFFFF4F94), Color(0xFFC77DFF))

/**
 * The Stories feed's single, fixed background - see [StoryTextContent]'s `drawWithCache` block
 * for how these stops are turned into a [Brush.radialGradient]. Approximates CSS
 * `radial-gradient(130% 90% at 70% 100%, #2B0F14 0%, #170A0C 55%, #0A0506 100%)`: a warm, dark
 * ember glow radiating from the lower-right corner. This replaces the old per-story
 * [Story.thumbnailGradient] entirely - that field and its per-story assignments were removed
 * from [Story]/[FakeStoryRepository], every story now shares this one background.
 */
private val EMBER_BACKGROUND_STOPS = arrayOf(
    0.00f to Color(0xFF2B0F14),
    0.55f to Color(0xFF170A0C),
    1.00f to Color(0xFF0A0506)
)

/**
 * Editorial text style for the story's revealed characters - Playfair Display doesn't actually
 * exist anywhere in this codebase (checked: no Playfair asset in `res/font`, and
 * `presentation/theme/Typography.kt`'s `FreeDramaTypography` is a bare `Typography()` with zero
 * overrides). [reelsdrama.freedrama.videosdrama.presentation.theme.InstrumentSerif] is now a real
 * bundled font, but it's specific to [reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute]'s
 * wordmark - adding a real Playfair Display asset for this file's own use is still out of scope
 * here, so this keeps reusing the generic [FontFamily.Serif] fallback instead of declaring a new
 * font.
 *
 * Deliberately carries no `color`/`brush` of its own - color is supplied per call site instead
 * (a solid, sampled [STORY_TEXT_GRADIENT] color via [AnimatedChar]'s `color` param or
 * [SettledText]'s per-character [SpanStyle]s), so this one shared style can't accidentally paint
 * every character the same flat color the way a single baked-in brush would.
 */
private val EDITORIAL_TEXT_STYLE = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.1).sp
)

/** Same soft, overshoot-free deceleration curve [reelsdrama.freedrama.videosdrama.presentation.splash.SplashRoute]'s
 *  wordmark reveal already uses - matching this codebase's existing "rise + blur + fade" idiom. */
private val SETTLE_EASING = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** How long one character takes to settle (blur/alpha/offsetY) once revealed. */
private const val CHAR_SETTLE_DURATION_MS = 255

/** How far a character travels (translateY) from its starting position down to its resting spot. */
private val CHAR_RISE_DISTANCE = 3.dp
private const val CHAR_START_BLUR_DP = 6f

/** Typing cadence - how long StoryTextContent's `charsRevealed` waits between each new character. */
private const val CHAR_TICK_MS = 35L

/** How many of the most-recently-revealed characters (in typing order) are still within their
 *  settle animation and therefore rendered as individual [AnimatedChar]s rather than baked into
 *  a [SettledText] - derived from how many ticks fit inside one character's settle animation,
 *  plus one tick of buffer so a character is never disposed from [AnimatedChar] before its own
 *  animation has actually finished. */
private val ANIMATING_TAIL_CHARS = ceil(CHAR_SETTLE_DURATION_MS / CHAR_TICK_MS.toFloat()).toInt() + 1

private const val CARET_BLINK_MS = 480
private val CARET_WIDTH = 2.dp
private val CARET_HEIGHT = 18.dp
