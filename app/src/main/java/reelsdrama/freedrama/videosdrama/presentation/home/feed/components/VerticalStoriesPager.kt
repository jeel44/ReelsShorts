package reelsdrama.freedrama.videosdrama.presentation.home.feed.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import reelsdrama.freedrama.videosdrama.core.constants.PlayerConstants
import reelsdrama.freedrama.videosdrama.domain.model.AdConfig
import reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem
import reelsdrama.freedrama.videosdrama.presentation.home.feed.StoriesEvent
import reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard
import reelsdrama.freedrama.videosdrama.presentation.home.feed.withAdSlots
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story

/**
 * Vertical pager for text stories - the Stories-feed sibling of [VerticalReelsPager]. Reuses the
 * exact same [withAdSlots] ad-slot insertion and the same three full-screen ad pages
 * ([FullScreenNativeAdPage]/[FullScreenBannerAdPage]/[FullScreenMediumRectangleAdPage]) that
 * [VerticalReelsPager] uses, unmodified.
 *
 * Unlike [VerticalReelsPager], there's no [reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager]
 * play/pause orchestration here - a story page is static text ([StoryCard]), not a video, so
 * there's nothing to play per settled page. There is, however, the same *shape* of "is this
 * page actually the one being looked at" check [VerticalReelsPager] uses to decide play vs.
 * pause: each [FeedItem.StoryItem] page passes `page == pagerState.settledPage` into
 * [StoryCard]'s `isActivePage`, so its typewriter reveal only runs while genuinely settled on,
 * not merely composed (this pager's own `beyondViewportPageCount = 1` below keeps one neighbor
 * page composed without it being active). The swipe-away coin-consumption gate is otherwise
 * identical: swiping past an ad slot fires nothing, only swiping away from an actual
 * [FeedItem.StoryItem] fires [StoriesEvent.StorySwiped].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalStoriesPager(
    stories: List<Story>,
    insufficientCoins: Boolean,
    nativeAdUnitId: String,
    bannerAdUnitId: String,
    mediumRectangleAdUnitId: String,
    adConfig: AdConfig,
    onLoadMore: () -> Unit,
    onEvent: (StoriesEvent) -> Unit
) {
    val items = remember(stories, adConfig) { stories.map(FeedItem::StoryItem).withAdSlots(adConfig) }
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Track the KEY (FeedItem.key - story.id/slotId, stable across list mutations) of the page
    // we are CURRENTLY on to detect successful swipes AWAY - NOT its index. See
    // [VerticalReelsPager]'s identical lastSettledKey for the full explanation: [items] is
    // rebuilt one entry shorter every time StoriesViewModel's watched-filter removes a
    // just-watched story, which shifts every later item's index down by one and makes
    // VerticalPager's key-based reflow re-fire this effect a second time for one physical swipe.
    // Comparing by key instead of index makes that reflow a no-op instead of misattributing a
    // second StorySwiped to whatever story now occupies the stale index.
    var lastSettledKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.settledPage) {
        if (items.isNotEmpty()) {
            val currentPage = pagerState.settledPage
            val currentKey = items.getOrNull(currentPage)?.key

            // TEMP DIAGNOSTIC (CoinDebug) - logs every run of this effect, not just when it
            // actually fires StorySwiped, so a run where the key-change guard fails is visible too.
            Log.d(
                "CoinDebug",
                "[Stories] pager effect run: settledPage=$currentPage currentKey=$currentKey " +
                    "lastSettledKey=$lastSettledKey"
            )

            // If we settled on a page with a DIFFERENT key, the user successfully swiped AWAY
            // from the last one - see lastSettledKey's doc comment for why this is keyed rather
            // than indexed. Coins are only consumed for swiping away from an actual story -
            // swiping past an ad page doesn't fire StoriesEvent.StorySwiped at all, and the very
            // first settle (lastSettledKey still null) never charges anything either.
            if (currentKey != null && currentKey != lastSettledKey) {
                val previousKey = lastSettledKey
                if (previousKey != null) {
                    val previousItem = items.firstOrNull { it.key == previousKey }
                    if (previousItem is FeedItem.StoryItem) {
                        onEvent(StoriesEvent.StorySwiped(previousItem.story.id))
                    }
                }
                lastSettledKey = currentKey
            }
        }
    }

    // Pagination
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= items.size - PlayerConstants.RECENT_PAGES_THRESHOLD && items.isNotEmpty()) {
            onLoadMore()
        }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
    } else {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !insufficientCoins,
            key = { index -> items[index].key }
        ) { page ->
            when (val item = items[page]) {
                is FeedItem.StoryItem -> {
                    StoryCard(
                        story = item.story,
                        // Mirrors VerticalReelsPager's own pagerState.settledPage-driven play-vs-
                        // pause check - the "is this the active page" mechanism this codebase
                        // already uses for the video feed - so StoryCard's typewriter reveal only
                        // runs while this page is actually the one being looked at, not merely
                        // composed (beyondViewportPageCount = 1 below keeps a neighbor page
                        // composed without it being active).
                        isActivePage = page == pagerState.settledPage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.NativeAdItem -> {
                    FullScreenNativeAdPage(
                        adUnitId = nativeAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.BannerAdItem -> {
                    FullScreenBannerAdPage(
                        adUnitId = bannerAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.MediumRectangleAdItem -> {
                    FullScreenMediumRectangleAdPage(
                        adUnitId = mediumRectangleAdUnitId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FeedItem.VideoItem -> {
                    // Unreachable: this pager only ever builds FeedItem.StoryItem content via
                    // stories.map(FeedItem::StoryItem).withAdSlots(adConfig) above.
                    error("VerticalStoriesPager received a VideoItem - should be unreachable")
                }
            }
        }
    }
}
