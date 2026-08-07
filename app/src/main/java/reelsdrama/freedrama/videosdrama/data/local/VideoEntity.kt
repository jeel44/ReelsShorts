package reelsdrama.freedrama.videosdrama.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
)
