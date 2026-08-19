package reelsdrama.freedrama.videosdrama.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import reelsdrama.freedrama.videosdrama.di.VideoUrlsRef
import reelsdrama.freedrama.videosdrama.domain.model.VideoUrls
import reelsdrama.freedrama.videosdrama.domain.repository.VideoUrlsRepository
import javax.inject.Inject

// Own tag, not "AdDebug" - AdConfigRepositoryImpl deliberately reuses that tag because /adConfig
// is thematically ad-related; /videoUrls isn't, so it gets its own "<Feature>Debug" tag matching
// this codebase's existing convention (CoinDebug, OneSignalDebug, PlayerDebug).
private const val TAG = "VideoUrlsDebug"

/**
 * Firebase Realtime Database-backed [VideoUrlsRepository] - reads/observes `/videoUrls`
 * ([videoUrlsRef], provided by `FirebaseModule`), not Remote Config. Mirrors
 * [AdConfigRepositoryImpl] exactly - same [callbackFlow]/[ValueEventListener] shape, same
 * default-first/live-update/onCancelled-fallback contract - see that class's own doc comment for
 * the rationale, not re-explained here.
 */
class VideoUrlsRepositoryImpl @Inject constructor(
    @VideoUrlsRef private val videoUrlsRef: DatabaseReference
) : VideoUrlsRepository {

    override fun getVideoUrls(): Flow<VideoUrls> = callbackFlow {
        // Emit the default immediately so collectors (FakeFeedRepository) have a usable
        // VideoUrls before Firebase's first callback ever fires - mirrors
        // AdConfigRepositoryImpl.getAdConfig's identical first line.
        trySend(VideoUrls())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val videoUrls = snapshot.toVideoUrls()
                Log.d(TAG, "VideoUrlsRepository: onDataChange -> ${videoUrls.urls.size} url(s) (raw snapshot=${snapshot.value})")
                trySend(videoUrls)
            }

            override fun onCancelled(error: DatabaseError) {
                // Permission-denied, offline with nothing cached, etc. - fall back to the
                // default rather than leaving the feed on a stale value or killing the flow.
                Log.w(TAG, "VideoUrlsRepository: onCancelled (${error.message}) -> falling back to default VideoUrls()")
                trySend(VideoUrls())
            }
        }

        videoUrlsRef.addValueEventListener(listener)

        // Removes the listener once the collecting scope is cancelled - see
        // AdConfigRepositoryImpl.getAdConfig's identical awaitClose for the same rationale.
        awaitClose { videoUrlsRef.removeEventListener(listener) }
    }

    /**
     * `/videoUrls` is expected to be a plain JSON array of URL strings (e.g.
     * `["https://.../a.mp4", "https://.../b.mp4"]`), so each indexed child is read individually
     * via [DataSnapshot.getValue] and [mapNotNull] rather than reading the snapshot's whole
     * value at once - a malformed/non-string entry is skipped instead of discarding the entire
     * list, the per-element equivalent of [AdConfigRepositoryImpl.toAdConfig]'s per-field
     * fallback. Falls back to [VideoUrls]'s own non-empty default if the node is missing or
     * parses to nothing - an empty list here is exactly the black-screen regression that
     * default exists to prevent (see [VideoUrls]'s own doc comment).
     */
    private fun DataSnapshot.toVideoUrls(): VideoUrls {
        val parsed = children.mapNotNull { it.getValue(String::class.java) }
        return if (parsed.isNotEmpty()) VideoUrls(parsed) else VideoUrls()
    }
}
