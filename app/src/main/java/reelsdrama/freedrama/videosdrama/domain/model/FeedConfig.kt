package reelsdrama.freedrama.videosdrama.domain.model

/**
 * Remotely-controlled visibility toggles for the Home screen's Videos/Stories tabs - sourced
 * from Firebase Realtime Database's `/feedConfig` node (see
 * [reelsdrama.freedrama.videosdrama.domain.repository.FeedConfigRepository]), not Remote Config.
 * Mirrors [AdConfig]'s shape/rationale exactly - see that class's doc comment for why a data
 * class with default values (rather than a separate constant) doubles as both the
 * pre-first-emission value and the onCancelled/missing-node fallback.
 *
 * The two flags are independent, not mutually exclusive - either, both, or (a misconfiguration)
 * neither can be true at once; see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedTabViewModel] for how each
 * combination is actually handled in the UI.
 *
 * Both default `true` - matching this app's behavior before this remote toggle existed (both
 * feeds always visible) - so adding this node is a no-op until someone actually edits
 * `/feedConfig` in Firebase, same rationale [AdConfig]'s own defaults document.
 */
data class FeedConfig(
    val videosEnabled: Boolean = true,
    val storiesEnabled: Boolean = true
)
