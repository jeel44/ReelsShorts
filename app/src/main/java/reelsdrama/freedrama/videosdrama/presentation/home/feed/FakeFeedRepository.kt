package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reelsdrama.freedrama.videosdrama.core.constants.NetworkConstants
import reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource
import reelsdrama.freedrama.videosdrama.domain.repository.VideoUrlsRepository
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake repository implementation for the Reels Feed.
 * Generates mock data for testing and development.
 *
 * [generateFakeVideos] is a plain synchronous function (not `suspend`, doesn't collect a `Flow`
 * itself) - so [VideoUrlsRepository]'s live `/videoUrls` [kotlinx.coroutines.flow.Flow] can't be
 * read from it directly. Instead, [currentVideoUrls] is a plain field this class keeps
 * continuously up to date via its own one-time collector (see [init]), and [generateFakeVideos]
 * just reads that field synchronously - the same "collect once into a plain field, plain code
 * reads the field" shape [reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdManager] and others
 * already use for state that has to be readable outside a suspend context.
 */
@Singleton
class FakeFeedRepository @Inject constructor(
    private val videoUrlsRepository: VideoUrlsRepository
) {

    private val gradients = listOf(
        listOf(Color(0xFF1B1B3A), Color(0xFFE94560)),
        listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
        listOf(Color(0xFF42275A), Color(0xFF734B6D)),
        listOf(Color(0xFF141E30), Color(0xFF243B55)),
        listOf(Color(0xFF200122), Color(0xFF6F0000)),
    )

    // App-process-lifetime scope, same idiom as
    // [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer.initScope]/[reelsdrama.freedrama.videosdrama.App.appScope] -
    // this class is a Hilt singleton with no natural owner to cancel a collection against, and it
    // needs to keep observing /videoUrls for the whole process lifetime so a live Firebase push
    // actually reaches the next [getForYouVideos]/[getCategoryVideos] call, not just the next app
    // launch - matching [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository]'s
    // own live-update contract (see that interface's doc comment).
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The actual value [generateFakeVideos] reads. Kept a plain field, not exposed as a
    // StateFlow/etc. - nothing outside this class needs to react to it changing, it exists purely
    // as this singleton's own cached "latest known" snapshot of
    // [VideoUrlsRepository.getVideoUrls], continuously kept current by the collector in [init].
    // @Volatile: [generateFakeVideos] can run on whatever dispatcher its caller
    // ([getForYouVideos]/[getCategoryVideos]) happens to be on, while the collector below writes
    // to it from [repoScope]'s own [Dispatchers.Default] thread - needs to be visible across
    // threads without a full lock for a simple read/replace of an immutable [List] reference.
    @Volatile
    private var currentVideoUrls: List<String> = emptyList()

    init {
        videoUrlsRepository.getVideoUrls()
            .onEach { videoUrls -> currentVideoUrls = videoUrls.urls }
            .launchIn(repoScope)
    }

    suspend fun getForYouVideos(page: Int, pageSize: Int): List<Video> {
        delay(NetworkConstants.NETWORK_DELAY_MS)
        return generateFakeVideos(page, pageSize, "foryou")
    }

    suspend fun getCategoryVideos(categoryId: String, page: Int, pageSize: Int): List<Video> {
        delay(NetworkConstants.NETWORK_DELAY_MS)
        return generateFakeVideos(page, pageSize, categoryId)
    }

    private fun generateFakeVideos(page: Int, pageSize: Int, type: String): List<Video> {
        val start = page * pageSize
        // Snapshotted once per call (not re-read from the @Volatile field per index below) so a
        // single page's URLs are internally consistent even if a Firebase push lands mid-call.
        // Falls back to FakeVideoSource.urls - kept around as exactly this safety net, and for
        // local testing without Firebase - if currentVideoUrls hasn't been populated yet (the
        // collector's first emission, VideoUrlsRepository's own pre-Firebase default, hasn't
        // landed) or somehow ended up empty.
        val urls = currentVideoUrls.ifEmpty { FakeVideoSource.urls }
        return List(pageSize) { i ->
            val index = start + i
            val number = index + 1
            Video(
                id = "${type}_video_$number",
                username = listOf("maya.dramas", "reelking", "shorts_avenue", "luna.stories", "daily.clips")[index % 5],
                caption = listOf(
                    "A secret promise changes everything in episode $number.",
                    "Wait for the final look — nobody saw this twist coming.",
                    "When love, ambition, and revenge collide in one night.",
                    "She thought it was over, but the message said otherwise.",
                )[index % 4],
                musicName = listOf(
                    "Original Sound - FreeDrama",
                    "Midnight City Beats",
                    "Lo-fi Drama Theme",
                    "Cinematic Pulse Audio",
                )[index % 4],
                hashtags = listOf("#drama", "#shorts", "#episode$number", if (index % 2 == 0) "#romance" else "#mystery"),
                isVerified = index % 3 != 1,
                likeCount = "${12 + index * 7}.${index % 9}K",
                commentCount = "${420 + index * 38}",
                shareCount = "${1 + index % 8}.${index % 10}K",
                viewCount = "${100 + index * 15}K",
                videoUrl = urls[index % urls.size],
                thumbnailGradient = gradients[index % gradients.size],
            )
        }
    }
}
