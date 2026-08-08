package reelsdrama.freedrama.videosdrama.presentation.interactions.comments

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeCommentRepository @Inject constructor() {

    suspend fun getComments(videoId: String): List<Comment> {
        delay(300) // Simulate slight network delay
        return List(100) { i ->
            Comment(
                id = "comment_$i",
                username = listOf("sarah_jones", "tech_guru", "travel_bug", "foodie_delight", "movie_buff")[i % 5],
                userAvatarUrl = "https://i.pravatar.cc/150?u=$i",
                text = listOf(
                    "This is absolutely amazing! 🔥",
                    "Wow, didn't see that coming!",
                    "Can't wait for the next episode.",
                    "The production quality is top notch.",
                    "Who else is watching this in 2026? 😂",
                    "Great content as always!",
                    "Is there a part 2 for this?",
                    "The ending was so emotional... 😭",
                    "I love the music in this reel.",
                    "Shared this with all my friends!"
                )[i % 10],
                timeAgo = "${(i % 24) + 1}h",
                likeCount = "${(i * 13) % 1000}",
                isVerified = i % 7 == 0
            )
        }
    }
}
