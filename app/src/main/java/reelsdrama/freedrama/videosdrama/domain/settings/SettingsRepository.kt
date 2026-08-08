package reelsdrama.freedrama.videosdrama.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateAutoPlay(enabled: Boolean)
    suspend fun updateAutoNextEpisode(enabled: Boolean)
    suspend fun updateVideoQuality(quality: String)
    suspend fun updateDataSaver(enabled: Boolean)
    suspend fun updateWifiOnlyDownloads(enabled: Boolean)
    suspend fun updateDownloadQuality(quality: String)
    suspend fun updateLikesNotification(enabled: Boolean)
    suspend fun updateCommentsNotification(enabled: Boolean)
    suspend fun updateTheme(theme: String)
    suspend fun updateAccentColor(color: String)
    suspend fun updateLanguage(language: String)
    suspend fun clearCache()
    fun getCacheSize(): Flow<String>
}
