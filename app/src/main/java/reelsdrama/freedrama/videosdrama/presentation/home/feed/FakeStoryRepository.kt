package reelsdrama.freedrama.videosdrama.presentation.home.feed

import kotlinx.coroutines.delay
import reelsdrama.freedrama.videosdrama.core.constants.NetworkConstants
import reelsdrama.freedrama.videosdrama.presentation.home.model.Story
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake repository for the Stories feed - the [Story] sibling of [FakeFeedRepository]. Same
 * pagination/fetch shape (a suspend fetch call, network-delay simulated, paged by
 * `page`/`pageSize`), but backed by a small fixed seed list rather than [FakeFeedRepository]'s
 * unbounded procedural generator.
 *
 * Unlike [FakeFeedRepository]'s pure-function generator, [seedStories] alone is finite (12
 * stories) - so once it's exhausted, [getForYouStories] endlessly reshuffles-and-loops: each new
 * "cycle" is a fresh [List.shuffled] copy of [seedStories] with every id suffixed `_cycle$n` (see
 * [generatedStories]/[cycleCount]), so the feed never actually runs dry, cycle 2+ never shows the
 * same order twice in a row, and every cycle's ids stay globally distinct from cycle 1's and from
 * every other cycle's - required so [androidx.compose.foundation.pager.VerticalPager]'s `key`
 * (see [reelsdrama.freedrama.videosdrama.presentation.home.feed.FeedItem.StoryItem]) never sees a
 * duplicate. Coin cost naturally re-applies to looped content as a side effect of this: cycle 2+
 * ids aren't in [reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository]'s
 * CONSUMED_REELS/WATCHED_REELS ledgers yet, so [reelsdrama.freedrama.videosdrama.data.repository.RewardsRepositoryImpl.consumeCoinForReel]
 * treats them as entirely new content - no separate reset of those ledgers needed or done here.
 *
 * Not synchronized against concurrent callers - [generatedStories]/[cycleCount] are plain mutable
 * state on a Hilt `@Singleton`, same as the rest of this class's existing stateful pattern (it
 * was already a stateful singleton re: [seedStories] itself). Safe in practice because the only
 * caller, [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoriesViewModel]'s
 * `fetchMoreUntilUnseenFound`, only ever has one fetch in flight at a time per ViewModel instance.
 *
 * No per-story background field any more - every [Story] page renders against the same fixed
 * "Ember" background (see [reelsdrama.freedrama.videosdrama.presentation.home.feed.StoryCard]'s
 * `EMBER_BACKGROUND_STOPS`), so the old per-story gradient list this class used to cycle through
 * was removed rather than kept unused.
 */
@Singleton
class FakeStoryRepository @Inject constructor() {

    /**
     * Cycle 1 = [seedStories] itself, untouched ids, seeded lazily on first access - `by lazy`
     * rather than a plain `= seedStories.toMutableList()` initializer specifically so this can be
     * declared above [seedStories] in this file without a forward-reference-in-initializer bug
     * (a function like [getForYouStories] can reference a property declared later in the class
     * safely since it only runs after construction completes, but another *property initializer*
     * referencing [seedStories] before it's assigned would silently see nothing).
     */
    private val generatedStories: MutableList<Story> by lazy { seedStories.toMutableList() }

    /** How many cycles - including the original, un-suffixed cycle 1 - exist in [generatedStories] so far. */
    private var cycleCount = 1

    suspend fun getForYouStories(page: Int, pageSize: Int): List<Story> {
        delay(NetworkConstants.NETWORK_DELAY_MS)
        val start = page * pageSize
        val end = start + pageSize

        // Keep appending freshly reshuffled, cycle-suffixed cycles until there's enough
        // generated content to satisfy this page - unlike the old "return emptyList() past
        // seedStories.size" behavior, this loop always terminates (each iteration adds a fixed
        // 12 items) and the function below never returns empty. A single page can end up
        // spanning the tail of one cycle and the head of the next (pageSize=20 > a 12-story
        // cycle) - that's fine, every id across every cycle is still guaranteed globally unique.
        while (generatedStories.size < end) {
            cycleCount++
            val suffix = "_cycle$cycleCount"
            generatedStories += seedStories.shuffled().map { it.copy(id = it.id + suffix) }
        }

        return generatedStories.subList(start, end)
    }

    private val seedStories: List<Story> = listOf(
        Story(
            id = "story_1",
            username = "maya.dramas",
            caption = "The letter she was never supposed to find.",
            audioLabel = "Whispers in the Rain",
            hashtags = listOf("#romance", "#drama", "#storytime"),
            isVerified = true,
            likeCount = "18.2K",
            commentCount = "612",
            shareCount = "2.4K",
            viewCount = "142K",
            lines = listOf(
                "Three years of marriage, and she'd never opened his desk drawer.",
                "Tonight, looking for a pen, she found the letter instead.",
                "It wasn't addressed to her.",
                "And it was dated last week.",
            )
        ),
        Story(
            id = "story_2",
            username = "reelking",
            caption = "He waited eleven years for this apology.",
            audioLabel = "Lo-fi Heartbeat",
            hashtags = listOf("#secondchance", "#drama"),
            isVerified = false,
            likeCount = "9.7K",
            commentCount = "301",
            shareCount = "1.1K",
            viewCount = "88K",
            lines = listOf(
                "\"I'm sorry\" isn't a sentence you rehearse for a decade.",
                "But he had.",
                "In the mirror, in the car, in every silence since she left.",
                "Now she was standing at his door, and the words wouldn't come.",
                "So he just opened it wider, and let her in.",
            )
        ),
        Story(
            id = "story_3",
            username = "luna.stories",
            caption = "She said yes to the wrong brother.",
            audioLabel = "Midnight City Beats",
            hashtags = listOf("#romance", "#twist", "#shorts"),
            isVerified = true,
            likeCount = "24.5K",
            commentCount = "980",
            shareCount = "3.8K",
            viewCount = "210K",
            lines = listOf(
                "The proposal was perfect - candlelight, a string quartet, her favorite song.",
                "She said yes before he even finished the question.",
                "It was only afterward, looking at the ring, that she realized:",
                "he wasn't the brother who'd asked.",
            )
        ),
        Story(
            id = "story_4",
            username = "shorts_avenue",
            caption = "The CEO's assistant knew all his secrets. Including this one.",
            audioLabel = "Cinematic Pulse Audio",
            hashtags = listOf("#office", "#drama", "#secrets"),
            isVerified = false,
            likeCount = "15.9K",
            commentCount = "544",
            shareCount = "2.0K",
            viewCount = "176K",
            lines = listOf(
                "For two years she'd filed his contracts, booked his flights, guarded his calendar.",
                "She'd never once opened the folder marked PRIVATE - until tonight.",
                "Inside was a photo of herself, from before she'd even started working there.",
            )
        ),
        Story(
            id = "story_5",
            username = "daily.clips",
            caption = "Her ex showed up at her wedding. As the officiant.",
            audioLabel = "Original Sound - FreeDrama",
            hashtags = listOf("#wedding", "#drama", "#romance"),
            isVerified = true,
            likeCount = "31.4K",
            commentCount = "1.2K",
            shareCount = "5.6K",
            viewCount = "302K",
            lines = listOf(
                "\"Do you take this man...\"",
                "She recognized the voice before she looked up.",
                "Five years, a broken engagement, and not one phone call since -",
                "and somehow he was the one asking her to say \"I do.\"",
            )
        ),
        Story(
            id = "story_6",
            username = "maya.dramas",
            caption = "The nanny wasn't hired to watch the kids. She was hired to watch him.",
            audioLabel = "Whispers in the Rain",
            hashtags = listOf("#drama", "#twist", "#romance"),
            isVerified = true,
            likeCount = "12.3K",
            commentCount = "402",
            shareCount = "1.5K",
            viewCount = "99K",
            lines = listOf(
                "The agency called it a \"family placement.\"",
                "What they didn't mention: his late wife had hired her, months before she died.",
                "\"Watch him,\" the note said. \"He forgets how to be happy.\"",
                "So far, she was the only one who remembered.",
            )
        ),
        Story(
            id = "story_7",
            username = "reelking",
            caption = "They'd been enemies since grade school. Then the will was read.",
            audioLabel = "Lo-fi Heartbeat",
            hashtags = listOf("#enemies", "#romance", "#shorts"),
            isVerified = false,
            likeCount = "20.1K",
            commentCount = "733",
            shareCount = "2.9K",
            viewCount = "188K",
            lines = listOf(
                "\"To my grandchildren,\" the lawyer read, \"I leave the vineyard - to both of you.\"",
                "\"Jointly.\"",
                "Across the room, her childhood rival's jaw dropped exactly as far as hers.",
                "Neither of them said a word. Neither of them looked away, either.",
            )
        ),
        Story(
            id = "story_8",
            username = "luna.stories",
            caption = "One text, meant for someone else, changed everything.",
            audioLabel = "Midnight City Beats",
            hashtags = listOf("#drama", "#secondchance"),
            isVerified = true,
            likeCount = "27.8K",
            commentCount = "890",
            shareCount = "4.1K",
            viewCount = "254K",
            lines = listOf(
                "\"I still love you. I never stopped.\"",
                "The text wasn't meant for her - it was meant for the woman he'd left her for.",
                "But it landed in her inbox anyway, four years too late.",
                "She stared at it for a long time before she finally typed back: \"I know.\"",
            )
        ),
        Story(
            id = "story_9",
            username = "shorts_avenue",
            caption = "The bride's best friend found the groom's real identity - the night before.",
            audioLabel = "Cinematic Pulse Audio",
            hashtags = listOf("#wedding", "#twist", "#drama"),
            isVerified = false,
            likeCount = "16.6K",
            commentCount = "601",
            shareCount = "2.2K",
            viewCount = "163K",
            lines = listOf(
                "It started as a joke - Googling the groom for the toast.",
                "It stopped being a joke three articles in.",
                "Different name. Different city. A whole different fiancee, still very much alive.",
                "She looked at the clock: 11:58 PM. The wedding was at noon.",
            )
        ),
        Story(
            id = "story_10",
            username = "daily.clips",
            caption = "Her mother arranged the marriage. She didn't arrange the falling in love part.",
            audioLabel = "Original Sound - FreeDrama",
            hashtags = listOf("#arrangedmarriage", "#romance"),
            isVerified = true,
            likeCount = "22.9K",
            commentCount = "815",
            shareCount = "3.3K",
            viewCount = "197K",
            lines = listOf(
                "She'd agreed to the marriage to save the family business, nothing more.",
                "Six months in, she still called it \"the arrangement\" - out loud, anyway.",
                "But she'd started leaving his coffee exactly the way he liked it.",
                "And she'd stopped being able to explain why that scared her.",
            )
        ),
        Story(
            id = "story_11",
            username = "maya.dramas",
            caption = "The rival firm's new lawyer looked exactly like her late husband.",
            audioLabel = "Whispers in the Rain",
            hashtags = listOf("#mystery", "#romance", "#drama"),
            isVerified = true,
            likeCount = "19.4K",
            commentCount = "670",
            shareCount = "2.6K",
            viewCount = "171K",
            lines = listOf(
                "Three years since the funeral. She still checked the obituary sometimes, to make sure it was real.",
                "Then the opposing counsel walked into the deposition room.",
                "Same walk. Same crooked smile. A different name on his card.",
                "\"Have we met?\" he asked, and she couldn't find her voice to answer.",
            )
        ),
        Story(
            id = "story_12",
            username = "reelking",
            caption = "She adopted his dog after the breakup. He never told her why he really left it.",
            audioLabel = "Lo-fi Heartbeat",
            hashtags = listOf("#breakup", "#secondchance", "#romance"),
            isVerified = false,
            likeCount = "14.7K",
            commentCount = "489",
            shareCount = "1.8K",
            viewCount = "132K",
            lines = listOf(
                "\"Keep the dog,\" was the last thing he'd said before he moved out.",
                "She thought it was cruelty. Turns out, it was the only excuse he had left to come back.",
                "Every Sunday, like clockwork, he showed up to \"check on\" a dog that was clearly thriving.",
                "It took her eight Sundays to admit she was starting to look forward to them too.",
            )
        ),
    )
}
