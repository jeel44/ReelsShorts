package reelsdrama.freedrama.videosdrama.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifies the `/adConfig` [DatabaseReference] (see [FirebaseModule.provideAdConfigRef]).
 * Previously unqualified - Hilt resolved the single [DatabaseReference] binding implicitly by
 * type - but now that [VideoUrlsRef] provides a second same-typed binding, Dagger requires every
 * same-typed binding in a component to be uniquely keyed, so this one is qualified too rather
 * than leaving one implicit and one explicit.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdConfigRef

/** Qualifies the `/videoUrls` [DatabaseReference] - see [FirebaseModule.provideVideoUrlsRef]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VideoUrlsRef

/** Qualifies the `/feedConfig` [DatabaseReference] - see [FirebaseModule.provideFeedConfigRef]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedConfigRef

/**
 * Firebase Realtime Database wiring - not Remote Config - for
 * [reelsdrama.freedrama.videosdrama.data.repository.AdConfigRepositoryImpl],
 * [reelsdrama.freedrama.videosdrama.data.repository.VideoUrlsRepositoryImpl], and
 * [reelsdrama.freedrama.videosdrama.data.repository.FeedConfigRepositoryImpl].
 *
 * Requires `app/google-services.json` plus the `com.google.gms.google-services` plugin applied
 * in app/build.gradle.kts before [FirebaseDatabase.getInstance] actually resolves a real
 * `FirebaseApp` at runtime - neither exists yet (see that file's plugins block), so this module
 * compiles cleanly but isn't exercised until both are added.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /** Path of the remote ad-slot toggle node - see [reelsdrama.freedrama.videosdrama.domain.model.AdConfig]. */
    private const val AD_CONFIG_PATH = "adConfig"

    /** Path of the remote video-URL list node - see [reelsdrama.freedrama.videosdrama.domain.model.VideoUrls]. */
    private const val VIDEO_URLS_PATH = "videoUrls"

    /** Path of the remote Videos/Stories tab-visibility toggle node - see [reelsdrama.freedrama.videosdrama.domain.model.FeedConfig]. */
    private const val FEED_CONFIG_PATH = "feedConfig"

    @AdConfigRef
    @Provides
    @Singleton
    fun provideAdConfigRef(): DatabaseReference =
        FirebaseDatabase.getInstance().getReference(AD_CONFIG_PATH)

    @VideoUrlsRef
    @Provides
    @Singleton
    fun provideVideoUrlsRef(): DatabaseReference =
        FirebaseDatabase.getInstance().getReference(VIDEO_URLS_PATH)

    @FeedConfigRef
    @Provides
    @Singleton
    fun provideFeedConfigRef(): DatabaseReference =
        FirebaseDatabase.getInstance().getReference(FEED_CONFIG_PATH)
}
