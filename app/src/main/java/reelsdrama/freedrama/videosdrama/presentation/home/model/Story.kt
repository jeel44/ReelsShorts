package reelsdrama.freedrama.videosdrama.presentation.home.model

/**
 * Presentation model for a single page in the Stories feed - the text-based sibling of [Video]
 * for [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem.StoryItem]. Mirrors
 * [Video]'s shape field-for-field wherever an equivalent exists, so the shared action-rail/
 * caption composables ([reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoInfoOverlay],
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.components.VideoSideActionBar]) can
 * bind to either type without forking a second set of composables.
 *
 * Unlike [Video], there is no per-story background field - every story's page uses the same
 * fixed "Ember" background (see [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]'s
 * `EMBER_BACKGROUND_STOPS`), so a `thumbnailGradient`-equivalent field was removed rather than
 * kept unused.
 */
data class Story(
    val id: String,
    val username: String,
    val caption: String,
    /** Equivalent of [Video.musicName] - an ambient/background audio label shown in the same
     *  info-overlay row, not necessarily music. */
    val audioLabel: String,
    val hashtags: List<String>,
    val isVerified: Boolean,
    val likeCount: String,
    val commentCount: String,
    val shareCount: String,
    val viewCount: String,
    /**
     * The story's actual text content, one entry per line, revealed character-by-character (see
     * [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]).
     */
    val lines: List<String>,
)
