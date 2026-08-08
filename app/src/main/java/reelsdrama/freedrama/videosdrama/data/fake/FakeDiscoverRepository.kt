package reelsdrama.freedrama.videosdrama.data.fake

import reelsdrama.freedrama.videosdrama.domain.model.Drama
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake repository providing mock drama data for the Discover screen.
 */
@Singleton
class FakeDiscoverRepository @Inject constructor() {

    private val titles = listOf(
        "Eternal Love", "Hidden Vows", "Midnight Secret", "Golden Empire",
        "Destiny's Edge", "Crimson Night", "Summer Whisper", "Silent Echo",
        "Broken Throne", "Velvet Revenge", "Rising Sun", "Fallen Star",
        "Moonlight Waltz", "Iron Heart", "Ocean Dreams", "Lost Legacy"
    )

    private val genres = listOf(
        "🔥 Trending", "🆕 New Releases", "❤️ Romance", "😱 Thriller", "😂 Comedy", 
        "🎬 Action", "🇰🇷 Korean Drama", "🇨🇳 Chinese Drama", "🇯🇵 Japanese Drama", "🇹🇭 Thai Drama"
    )

    fun getDiscoverSections(): Map<String, List<Drama>> {
        return genres.associateWith { genre ->
            List(10) { i ->
                val id = "${genre.lowercase().replace(" ", "_")}_$i"
                Drama(
                    id = id,
                    title = titles.random() + " " + (i + 1),
                    posterUrl = "https://picsum.photos/seed/$id/360/640",
                    episodes = (20..100).random(),
                    isHd = true,
                    isPremium = i % 5 == 0,
                    genre = genre
                )
            }
        }
    }
}
