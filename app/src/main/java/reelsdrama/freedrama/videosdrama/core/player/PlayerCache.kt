package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object PlayerCache {
    private const val CACHE_FOLDER = "video_media3_cache"
    private const val DEFAULT_CACHE_SIZE_BYTES = 256L * 1024L * 1024L

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    fun get(context: Context, maxBytes: Long = DEFAULT_CACHE_SIZE_BYTES): Cache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(context.cacheDir, CACHE_FOLDER),
                LeastRecentlyUsedCacheEvictor(maxBytes),
                databaseProvider ?: StandaloneDatabaseProvider(context).also { databaseProvider = it },
            ).also { simpleCache = it }
        }
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun release() {
        synchronized(this) {
            simpleCache?.release()
            simpleCache = null
            databaseProvider = null
        }
    }
}
