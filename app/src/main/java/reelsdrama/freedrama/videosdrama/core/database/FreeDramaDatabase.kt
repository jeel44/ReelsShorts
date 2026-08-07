package reelsdrama.freedrama.videosdrama.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import reelsdrama.freedrama.videosdrama.data.local.VideoDao
import reelsdrama.freedrama.videosdrama.data.local.VideoEntity

@Database(entities = [VideoEntity::class], version = 1, exportSchema = true)
abstract class FreeDramaDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
