package reelsdrama.freedrama.videosdrama.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import reelsdrama.freedrama.videosdrama.core.constants.SettingsConstants
import reelsdrama.freedrama.videosdrama.domain.settings.*
import java.io.File
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val AUTO_NEXT_EPISODE = booleanPreferencesKey("auto_next_episode")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val NOTIF_LIKES = booleanPreferencesKey("notif_likes")
        val NOTIF_COMMENTS = booleanPreferencesKey("notif_comments")
        val THEME = stringPreferencesKey("theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val LANGUAGE = stringPreferencesKey("language")
    }

    private val _cacheSize = MutableStateFlow(calculateCacheSize())

    override fun getUserSettings(): Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            playback = PlaybackSettings(
                autoPlay = preferences[PreferencesKeys.AUTO_PLAY] ?: true,
                autoNextEpisode = preferences[PreferencesKeys.AUTO_NEXT_EPISODE] ?: true,
                videoQuality = preferences[PreferencesKeys.VIDEO_QUALITY] ?: SettingsConstants.DEFAULT_VIDEO_QUALITY
            ),
            storage = StorageSettings(
                dataSaver = preferences[PreferencesKeys.DATA_SAVER] ?: false,
                wifiOnlyDownloads = preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] ?: true,
                downloadQuality = preferences[PreferencesKeys.DOWNLOAD_QUALITY] ?: SettingsConstants.DEFAULT_DOWNLOAD_QUALITY,
                cacheSize = _cacheSize.value
            ),
            notifications = NotificationSettings(
                likes = preferences[PreferencesKeys.NOTIF_LIKES] ?: true,
                comments = preferences[PreferencesKeys.NOTIF_COMMENTS] ?: true
            ),
            appearance = AppearanceSettings(
                theme = preferences[PreferencesKeys.THEME] ?: SettingsConstants.DEFAULT_THEME,
                accentColor = preferences[PreferencesKeys.ACCENT_COLOR] ?: SettingsConstants.DEFAULT_ACCENT_COLOR,
                language = preferences[PreferencesKeys.LANGUAGE] ?: SettingsConstants.DEFAULT_LANGUAGE
            )
        )
    }

    override suspend fun updateAutoPlay(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_PLAY] = enabled }
    }

    override suspend fun updateAutoNextEpisode(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_NEXT_EPISODE] = enabled }
    }

    override suspend fun updateVideoQuality(quality: String) {
        dataStore.edit { it[PreferencesKeys.VIDEO_QUALITY] = quality }
    }

    override suspend fun updateDataSaver(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DATA_SAVER] = enabled }
    }

    override suspend fun updateWifiOnlyDownloads(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    override suspend fun updateDownloadQuality(quality: String) {
        dataStore.edit { it[PreferencesKeys.DOWNLOAD_QUALITY] = quality }
    }

    override suspend fun updateLikesNotification(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIF_LIKES] = enabled }
    }

    override suspend fun updateCommentsNotification(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIF_COMMENTS] = enabled }
    }

    override suspend fun updateTheme(theme: String) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme }
    }

    override suspend fun updateAccentColor(color: String) {
        dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = color }
    }

    override suspend fun updateLanguage(language: String) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language }
    }

    override fun getCacheSize(): Flow<String> = _cacheSize.asStateFlow()

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            deleteDir(context.cacheDir)
            _cacheSize.value = calculateCacheSize()
        }
    }

    private fun calculateCacheSize(): String {
        val sizeInBytes = getDirSize(context.cacheDir)
        return formatSize(sizeInBytes)
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    size += getDirSize(file)
                } else {
                    size += file.length()
                }
            }
        }
        return size
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list() ?: return false
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    private fun formatSize(sizeInBytes: Long): String {
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            else -> "%.2f KB".format(kb)
        }
    }
}
