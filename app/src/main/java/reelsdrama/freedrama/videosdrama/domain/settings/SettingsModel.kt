package reelsdrama.freedrama.videosdrama.domain.settings

data class UserSettings(
    val playback: PlaybackSettings = PlaybackSettings(),
    val storage: StorageSettings = StorageSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val appearance: AppearanceSettings = AppearanceSettings()
)

data class PlaybackSettings(
    val autoPlay: Boolean = true,
    val autoNextEpisode: Boolean = true,
    val videoQuality: String = "Auto"
)

data class StorageSettings(
    val dataSaver: Boolean = false,
    val wifiOnlyDownloads: Boolean = true,
    val downloadQuality: String = "HD",
    val cacheSize: String = "512 MB"
)

data class NotificationSettings(
    val likes: Boolean = true,
    val comments: Boolean = true
)

data class AppearanceSettings(
    val theme: String = "System", // Light, Dark, System
    val accentColor: String = "Default",
    val language: String = "English"
)
