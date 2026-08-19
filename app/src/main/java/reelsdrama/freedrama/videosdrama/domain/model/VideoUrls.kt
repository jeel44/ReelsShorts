package reelsdrama.freedrama.videosdrama.domain.model

/**
 * Remotely-controlled list of video URLs the feed cycles through - sourced from Firebase
 * Realtime Database's `/videoUrls` node (see
 * [reelsdrama.freedrama.videosdrama.domain.repository.VideoUrlsRepository]), not Remote Config.
 * Mirrors [AdConfig]'s shape/rationale exactly - see that class's doc comment for why a data
 * class with a default value (rather than a separate constant) doubles as both the
 * pre-first-emission value and the onCancelled/missing-node fallback.
 *
 * Defaults to a small, known-reachable subset of
 * [reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource.urls] - deliberately duplicated as
 * a literal here rather than referencing that object directly, since this is a `domain` model and
 * [reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource] lives in `data` (this project's
 * other `domain` models, e.g. [AdConfig], don't reference `data` either - the dependency only
 * ever points the other way). A non-empty default matters here specifically: an empty [urls] list
 * would mean every generated [reelsdrama.freedrama.videosdrama.presentation.home.model.Video]
 * gets an empty `videoUrl` - i.e. the exact silent-failure/black-screen bug this app already hit
 * once with a single dead host (see [reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource]'s
 * own doc comment) - so Firebase being unreachable, or `/videoUrls` not yet populated, still
 * leaves the feed with something playable rather than nothing.
 */
data class VideoUrls(
    val urls: List<String> = listOf(
        "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4"
    )
)
