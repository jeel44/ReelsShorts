package reelsdrama.freedrama.videosdrama.presentation.home.feed

import reelsdrama.freedrama.videosdrama.presentation.home.model.Video

/**
 * A single page in [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VerticalReelsPager]:
 * either a real reel or a full-screen native ad slot.
 *
 * Replaces the old interstitial - a full-screen overlay shown on TOP of a reel, pausing it -
 * with a native ad that is itself a swipeable pager page the user scrolls past like any other
 * reel. See [withNativeAdSlots] for where these are inserted.
 */
sealed interface FeedItem {
    /** Stable identity for this page, used as [androidx.compose.foundation.pager.VerticalPager]'s `key`. */
    val key: String

    data class VideoItem(val video: Video) : FeedItem {
        override val key: String get() = video.id
    }

    data class NativeAdItem(val slotId: String) : FeedItem {
        override val key: String get() = slotId
    }
}

/**
 * Builds the pager's page list from [this] video list, inserting a [FeedItem.NativeAdItem] page
 * after every [every]th video (see [NATIVE_AD_EVERY_N_REELS] for the current interval).
 * Expressed structurally as a page in the list rather than a stateful overlay trigger (the old
 * interstitial's approach), since the native ad has to be an actual swipeable page rather than
 * something shown on top of one.
 */
fun List<Video>.withNativeAdSlots(every: Int = NATIVE_AD_EVERY_N_REELS): List<FeedItem> = buildList {
    this@withNativeAdSlots.forEachIndexed { index, video ->
        add(FeedItem.VideoItem(video))
        val position = index + 1
        if (position % every == 0) {
            add(FeedItem.NativeAdItem(slotId = "native_ad_after_${video.id}"))
        }
    }
}

/** Insert a full-screen native ad page every Nth reel. */
const val NATIVE_AD_EVERY_N_REELS = 3
