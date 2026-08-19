package reelsdrama.freedrama.videosdrama.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import reelsdrama.freedrama.videosdrama.di.AdConfigRef
import reelsdrama.freedrama.videosdrama.domain.model.AdConfig
import reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository
import javax.inject.Inject

// Matches the tag every other ad component in this app logs under (AdInitializer,
// NativeAdManager's callers, AdBannerView, FullScreenNativeAdPage) - unified here so this
// listener's activity shows up in the same logcat filter as everything else ad-related.
private const val TAG = "AdDebug"

/**
 * Firebase Realtime Database-backed [AdConfigRepository] - reads/observes `/adConfig`
 * ([adConfigRef], provided by `FirebaseModule`), not Remote Config.
 *
 * Wraps a [ValueEventListener] in [callbackFlow] rather than pulling in the separate
 * `firebase-database-ktx` `DatabaseReference.snapshots` extension, so the default-fallback
 * behavior on a missing node/field and on [onCancelled] is explicit and co-located here rather
 * than split across a generic extension.
 */
class AdConfigRepositoryImpl @Inject constructor(
    @AdConfigRef private val adConfigRef: DatabaseReference
) : AdConfigRepository {

    override fun getAdConfig(): Flow<AdConfig> = callbackFlow {
        // Emit the default immediately so collectors (FeedViewModel) have a usable AdConfig
        // before Firebase's first callback ever fires - the ad-slot decision logic must never
        // block on a network round trip, and this is overwritten the instant a real value (or
        // onCancelled's own fallback) arrives.
        trySend(AdConfig())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val adConfig = snapshot.toAdConfig()
                Log.d(TAG, "AdConfigRepository: onDataChange -> $adConfig (raw snapshot=${snapshot.value})")
                trySend(adConfig)
            }

            override fun onCancelled(error: DatabaseError) {
                // Permission-denied, offline with nothing cached, etc. - fall back to the
                // default rather than leaving the feed on a stale value or killing the flow.
                Log.w(TAG, "AdConfigRepository: onCancelled (${error.message}) -> falling back to default AdConfig()")
                trySend(AdConfig())
            }
        }

        adConfigRef.addValueEventListener(listener)

        // Removes the listener once the collecting scope (FeedViewModel.viewModelScope) is
        // cancelled - the Flow equivalent of ViewModel.onCleared() cleanup, so this never leaks
        // a live Firebase listener past the screen that started it.
        awaitClose { adConfigRef.removeEventListener(listener) }
    }

    /**
     * Missing node, or an individual field missing/of the wrong type, falls back to [AdConfig]'s
     * own default for that field specifically - a node with only `bannerEnabled` set still gets
     * [AdConfig]'s default `nativeEnabled`, rather than the whole snapshot being discarded.
     */
    private fun DataSnapshot.toAdConfig(): AdConfig {
        val default = AdConfig()
        return AdConfig(
            bannerEnabled = child("bannerEnabled").getValue(Boolean::class.java) ?: default.bannerEnabled,
            nativeEnabled = child("nativeEnabled").getValue(Boolean::class.java) ?: default.nativeEnabled
        )
    }
}
