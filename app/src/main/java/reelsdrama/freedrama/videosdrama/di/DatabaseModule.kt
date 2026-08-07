package reelsdrama.freedrama.videosdrama.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import reelsdrama.freedrama.videosdrama.core.constants.AppConstants
import reelsdrama.freedrama.videosdrama.core.database.FreeDramaDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FreeDramaDatabase =
        Room.databaseBuilder(context, FreeDramaDatabase::class.java, AppConstants.DATABASE_NAME).build()

    @Provides
    fun provideVideoDao(database: FreeDramaDatabase) = database.videoDao()
}
