package reelsdrama.freedrama.videosdrama.domain.repository

import kotlinx.coroutines.flow.Flow
import reelsdrama.freedrama.videosdrama.domain.model.AdConfig

/**
 * Exposes the reels feed's remotely-controlled ad-slot [AdConfig], sourced from Firebase
 * Realtime Database (not Remote Config) at `/adConfig`.
 */
interface AdConfigRepository {
    /**
     * Emits the current [AdConfig], starting with [AdConfig]'s own defaults immediately (before
     * Firebase has delivered anything) and updating live on every server-side change thereafter.
     * Never throws and never completes on a read failure - see the implementation for exactly
     * how failures fall back to the default.
     */
    fun getAdConfig(): Flow<AdConfig>
}
