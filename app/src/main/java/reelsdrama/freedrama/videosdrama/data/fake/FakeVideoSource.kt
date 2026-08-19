package reelsdrama.freedrama.videosdrama.data.fake

/**
 * Sample video URLs used to seed [reelsdrama.freedrama.videosdrama.presentation.home.feed.FakeFeedRepository]'s
 * generated feed - cycled via `urls[index % urls.size]` (see that class), so these don't need to
 * correspond 1:1 with any particular reel's generated caption/metadata.
 *
 * Previously pointed at `lathiyainfotech.com` (a personal, unmaintained host) which turned out to
 * be completely unreachable - every request timed out, confirmed directly - causing every video
 * in the feed to fail identically with an [androidx.media3.exoplayer.ExoPlaybackException] that
 * [reelsdrama.freedrama.videosdrama.core.player.VideoPlayer] had no `onPlayerError` handling for
 * at the time, so the whole feed just looked like a permanently stuck black/loading screen (see
 * that class's `onPlayerError` for the fix to the other half of this).
 *
 * Replaced with two purpose-built public sample-video hosts, NOT Google's old
 * `commondatastorage.googleapis.com/gtv-videos-bucket` - the conventional go-to for this and
 * where these same Creative Commons clips (Big Buck Bunny, Sintel, etc.) originally came from,
 * but confirmed (via a direct request just before this change) to now return
 * `403 AccessDenied` for anonymous/public reads; the bucket's objects still exist, public access
 * to them doesn't:
 * - `test-videos.co.uk` - a resource built specifically for hosting standard public-domain test
 *   clips (Big Buck Bunny/Sintel/Jellyfish) at various codecs/resolutions for exactly this use.
 * - `samplelib.com` - same purpose, plain fixed-duration sample MP4s.
 *
 * Every URL below returned HTTP 200 on a direct request immediately before this change - verified
 * individually, not assumed from either site's reputation alone.
 */
object FakeVideoSource {
    val urls = listOf(
        "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
        "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4",
        "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4",
        "https://download.samplelib.com/mp4/sample-5s.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4",
        "https://download.samplelib.com/mp4/sample-15s.mp4",
        "https://download.samplelib.com/mp4/sample-20s.mp4",
        "https://download.samplelib.com/mp4/sample-30s.mp4"
    )
}
