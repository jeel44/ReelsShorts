package reelsdrama.freedrama.videosdrama.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import reelsdrama.freedrama.videosdrama.data.repository.DefaultVideoRepository
import reelsdrama.freedrama.videosdrama.domain.repository.VideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVideoRepository(repository: DefaultVideoRepository): VideoRepository
}
