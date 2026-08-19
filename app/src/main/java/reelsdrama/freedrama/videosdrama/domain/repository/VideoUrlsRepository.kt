package reelsdrama.freedrama.videosdrama.domain.repository

import kotlinx.coroutines.flow.Flow
import reelsdrama.freedrama.videosdrama.domain.model.VideoUrls

/**
 * Exposes the reels feed's remotely-controlled [VideoUrls] list, sourced from Firebase Realtime
 * Database (not Remote Config) at `/videoUrls`. Mirrors [AdConfigRepository] exactly - see that
 * interface's doc comment for the emission contract this also follows.
 */
interface VideoUrlsRepository {
    /**
     * Emits the current [VideoUrls], starting with [VideoUrls]'s own default immediately (before
     * Firebase has delivered anything) and updating live on every server-side change thereafter.
     * Never throws and never completes on a read failure - see the implementation for exactly
     * how failures fall back to the default.
     */
    fun getVideoUrls(): Flow<VideoUrls>
}
