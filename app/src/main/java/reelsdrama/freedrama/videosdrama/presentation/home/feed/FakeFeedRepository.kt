package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import reelsdrama.freedrama.videosdrama.core.constants.NetworkConstants
import reelsdrama.freedrama.videosdrama.data.fake.FakeVideoSource
import reelsdrama.freedrama.videosdrama.presentation.home.model.Video
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake repository implementation for the Reels Feed.
 * Generates mock data for testing and development.
 */
@Singleton
class FakeFeedRepository @Inject constructor() {

    private val gradients = listOf(
        listOf(Color(0xFF1B1B3A), Color(0xFFE94560)),
        listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
        listOf(Color(0xFF42275A), Color(0xFF734B6D)),
        listOf(Color(0xFF141E30), Color(0xFF243B55)),
        listOf(Color(0xFF200122), Color(0xFF6F0000)),
    )

    suspend fun getFollowingVideos(page: Int, pageSize: Int): List<Video> {
        delay(NetworkConstants.NETWORK_DELAY_MS)
        return generateFakeVideos(page, pageSize, "following")
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
                videoUrl = FakeVideoSource.urls[index % FakeVideoSource.urls.size],
                thumbnailGradient = gradients[index % gradients.size],
            )
        }
    }
}
