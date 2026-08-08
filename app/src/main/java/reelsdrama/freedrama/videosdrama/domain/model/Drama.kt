package reelsdrama.freedrama.videosdrama.domain.model

/**
 * Data model representing a drama series in the discover section.
 */
data class Drama(
    val id: String,
    val title: String,
    val posterUrl: String,
    val episodes: Int,
    val isHd: Boolean = true,
    val isPremium: Boolean = false,
    val genre: String = ""
)
