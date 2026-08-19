package reelsdrama.freedrama.videosdrama.presentation.home.feed

import reelsdrama.freedrama.videosdrama.domain.model.AdConfig
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * A single page in [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager]
 * or [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalStoriesPager]:
 * either real feed [Content] (a video reel or a text story) or a full-screen ad slot.
 *
 * Replaces the old interstitial - a full-screen overlay shown on TOP of a reel, pausing it -
 * with an ad that is itself a swipeable pager page the user scrolls past like any other reel.
 * See [withAdSlots] for where these are inserted and which ad type each slot gets.
 */
sealed interface FeedItem {
    /** Stable identity for this page, used as [androidx.compose.foundation.pager.VerticalPager]'s `key`. */
    val key: String

    /**
     * Real feed content - as opposed to an ad slot - that [withAdSlots] threads through
     * unchanged while inserting ad pages around it. [VideoItem] and [StoryItem] are its only
     * two implementations, one per feed.
     */
    sealed interface Content : FeedItem

    data class VideoItem(val video: Video) : Content {
        override val key: String get() = video.id
    }

    data class StoryItem(val story: Story) : Content {
        override val key: String get() = story.id
    }

    data class NativeAdItem(val slotId: String) : FeedItem {
        override val key: String get() = slotId
    }

    data class BannerAdItem(val slotId: String) : FeedItem {
        override val key: String get() = slotId
    }

    /**
     * A centered, fixed 300x250dp MEDIUM_RECTANGLE ad card - see
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.FullScreenMediumRectangleAdPage].
     * Independent of [AdConfig]'s bannerEnabled/nativeEnabled toggle - always-on, on its own
     * [MEDIUM_RECTANGLE_AD_EVERY_N_REELS] cadence, unlike [BannerAdItem]/[NativeAdItem].
     */
    data class MediumRectangleAdItem(val slotId: String) : FeedItem {
        override val key: String get() = slotId
    }
}

/**
 * Builds a pager's page list from [this] list of feed [FeedItem.Content] (video reels or text
 * stories), inserting an ad page after every [every]th item (see [NATIVE_AD_EVERY_N_REELS] for
 * the current interval). Each [FeedItem.Content] element is passed through unchanged - this
 * function only decides what gets inserted around it, never constructs the content items
 * themselves (callers build those, e.g. `videos.map(FeedItem::VideoItem)` or
 * `stories.map(FeedItem::StoryItem)`, before calling this). Expressed structurally as a page in
 * the list rather than a stateful overlay trigger (the old interstitial's approach), since an ad
 * slot has to be an actual swipeable page rather than something shown on top of one.
 *
 * Which ad type each slot gets is decided by [adConfig] (Firebase Realtime Database's
 * `/adConfig`, see [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository]):
 * - `bannerEnabled=true, nativeEnabled=false` -> every slot is a [FeedItem.BannerAdItem].
 * - `bannerEnabled=false, nativeEnabled=true` -> every slot is a [FeedItem.NativeAdItem] (the
 *   original, pre-Firebase behavior).
 * - both true -> slots alternate in swipe order: 1st = banner, 2nd = native, 3rd = banner, etc.,
 *   tracked by an incrementing slot index rather than the content's own index, so the alternation
 *   is unaffected by [every].
 * - both false -> no ad slot is inserted at all; the feed plays with no every-[every] interruption.
 *
 * Independently of [adConfig], every [MEDIUM_RECTANGLE_AD_EVERY_N_REELS]th item also gets a
 * [FeedItem.MediumRectangleAdItem] slot - a separate, always-on placement not gated by the
 * Firebase toggle at all. If a position is a multiple of both [every] and
 * [MEDIUM_RECTANGLE_AD_EVERY_N_REELS], the [every]-driven slot above wins and the MREC slot is
 * skipped that round, so two ad pages are never stacked back to back for the same position.
 */
fun List<FeedItem.Content>.withAdSlots(
    adConfig: AdConfig,
    every: Int = NATIVE_AD_EVERY_N_REELS,
    mediumRectangleEvery: Int = MEDIUM_RECTANGLE_AD_EVERY_N_REELS
): List<FeedItem> = buildList {
    var adSlotIndex = 0
    this@withAdSlots.forEachIndexed { index, content ->
        add(content)
        val position = index + 1

        val slot = when {
            position % every == 0 -> when {
                adConfig.bannerEnabled && adConfig.nativeEnabled -> {
                    // Alternate by the running ad-slot count, not by content position - the 1st
                    // slot inserted is always a banner regardless of `every`.
                    val item = if (adSlotIndex % 2 == 0) {
                        FeedItem.BannerAdItem(slotId = "banner_ad_after_${content.key}")
                    } else {
                        FeedItem.NativeAdItem(slotId = "native_ad_after_${content.key}")
                    }
                    adSlotIndex++
                    item
                }
                adConfig.bannerEnabled -> FeedItem.BannerAdItem(slotId = "banner_ad_after_${content.key}")
                adConfig.nativeEnabled -> FeedItem.NativeAdItem(slotId = "native_ad_after_${content.key}")
                else -> null // Both disabled - no slot inserted for this position.
            }
            position % mediumRectangleEvery == 0 -> FeedItem.MediumRectangleAdItem(slotId = "mrec_ad_after_${content.key}")
            else -> null
        }
        slot?.let(::add)
    }
}

/** Insert a full-screen ad page every Nth reel. */
const val NATIVE_AD_EVERY_N_REELS = 3

/**
 * Insert a centered MEDIUM_RECTANGLE ad-card page every Nth reel - independent of, and less
 * frequent than, [NATIVE_AD_EVERY_N_REELS]. Picked as a placeholder cadence (not tied to any
 * product spec) so this new placement doesn't crowd the feed on top of the existing every-3
 * native/banner slot; tune freely.
 */
const val MEDIUM_RECTANGLE_AD_EVERY_N_REELS = 8
