package reelsdrama.freedrama.videosdrama.domain.repository

import kotlinx.coroutines.flow.Flow
import reelsdrama.freedrama.videosdrama.domain.model.FeedConfig

/**
 * Exposes the Home screen's remotely-controlled [FeedConfig] (Videos/Stories tab visibility),
 * sourced from Firebase Realtime Database (not Remote Config) at `/feedConfig`. Mirrors
 * [AdConfigRepository] exactly - see that interface's doc comment for the emission contract this
 * also follows.
 */
interface FeedConfigRepository {
    /**
     * Emits the current [FeedConfig], starting with [FeedConfig]'s own defaults immediately
     * (before Firebase has delivered anything) and updating live on every server-side change
     * thereafter. Never throws and never completes on a read failure - see the implementation for
     * exactly how failures fall back to the default.
     */
    fun getFeedConfig(): Flow<FeedConfig>
}
