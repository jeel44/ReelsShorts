package reelsdrama.freedrama.videosdrama.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import reelsdrama.freedrama.videosdrama.di.FeedConfigRef
import reelsdrama.freedrama.videosdrama.domain.model.FeedConfig
import reelsdrama.freedrama.videosdrama.domain.repository.FeedConfigRepository
import javax.inject.Inject

// Matches the tag every other ad component in this app logs under - same rationale
// AdConfigRepositoryImpl documents for its own identical choice: unified so this listener's
// activity shows up in the same logcat filter as everything else config/ad-related.
private const val TAG = "AdDebug"

/**
 * Firebase Realtime Database-backed [FeedConfigRepository] - reads/observes `/feedConfig`
 * ([feedConfigRef], provided by `FirebaseModule`), not Remote Config. Mirrors
 * [AdConfigRepositoryImpl] exactly - same [callbackFlow]/[ValueEventListener] shape, same
 * default-first/live-update/onCancelled-fallback contract, same per-field `?:` fallback
 * philosophy - see that class's own doc comment for the rationale, not re-explained here.
 */
class FeedConfigRepositoryImpl @Inject constructor(
    @FeedConfigRef private val feedConfigRef: DatabaseReference
) : FeedConfigRepository {

    override fun getFeedConfig(): Flow<FeedConfig> = callbackFlow {
        // Emit the default immediately so collectors (FeedTabViewModel) have a usable FeedConfig
        // before Firebase's first callback ever fires - mirrors AdConfigRepositoryImpl.getAdConfig's
        // identical first line.
        trySend(FeedConfig())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val feedConfig = snapshot.toFeedConfig()
                Log.d(TAG, "FeedConfigRepository: onDataChange -> $feedConfig (raw snapshot=${snapshot.value})")
                trySend(feedConfig)
            }

            override fun onCancelled(error: DatabaseError) {
                // Permission-denied, offline with nothing cached, etc. - fall back to the
                // default rather than leaving the tabs on a stale value or killing the flow.
                Log.w(TAG, "FeedConfigRepository: onCancelled (${error.message}) -> falling back to default FeedConfig()")
                trySend(FeedConfig())
            }
        }

        feedConfigRef.addValueEventListener(listener)

        // Removes the listener once the collecting scope is cancelled - see
        // AdConfigRepositoryImpl.getAdConfig's identical awaitClose for the same rationale.
        awaitClose { feedConfigRef.removeEventListener(listener) }
    }

    /**
     * Missing node, or an individual field missing/of the wrong type, falls back to
     * [FeedConfig]'s own default for that field specifically - a node with only `videosEnabled`
     * set still gets [FeedConfig]'s default `storiesEnabled`, rather than the whole snapshot
     * being discarded. Mirrors [AdConfigRepositoryImpl.toAdConfig] exactly.
     */
    private fun DataSnapshot.toFeedConfig(): FeedConfig {
        val default = FeedConfig()
        return FeedConfig(
            videosEnabled = child("videosEnabled").getValue(Boolean::class.java) ?: default.videosEnabled,
            storiesEnabled = child("storiesEnabled").getValue(Boolean::class.java) ?: default.storiesEnabled
        )
    }
}
