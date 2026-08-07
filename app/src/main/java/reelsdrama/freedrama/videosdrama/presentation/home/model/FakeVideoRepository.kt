package reelsdrama.freedrama.videosdrama.presentation.home.model

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Local fake repository for Home UI development. No backend or API calls are made here.
 */
class FakeVideoRepository @Inject constructor() {
    fun observeFollowingVideos(): Flow<List<Video>> = flowOf(fakeVideos.filterIndexed { index, _ -> index % 2 == 0 })

    fun observeForYouVideos(): Flow<List<Video>> = flowOf(fakeVideos)

    private companion object {
        val fakeVideos = List(20) { index ->
            val number = index + 1
            Video(
                id = "video_$number",
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
                giftCount = "${80 + index * 11}",
                coinCount = "${2 + index % 6}.${index % 7}K",
                thumbnailGradient = gradients[index % gradients.size],
            )
        }

        val gradients = listOf(
            listOf(Color(0xFF1B1B3A), Color(0xFFE94560)),
            listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
            listOf(Color(0xFF42275A), Color(0xFF734B6D)),
            listOf(Color(0xFF141E30), Color(0xFF243B55)),
            listOf(Color(0xFF200122), Color(0xFF6F0000)),
        )
    }
}
