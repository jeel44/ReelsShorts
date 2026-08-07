package reelsdrama.freedrama.videosdrama.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun observeVideos(): Flow<List<VideoEntity>>
}
