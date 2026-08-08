package reelsdrama.freedrama.videosdrama.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import reelsdrama.freedrama.videosdrama.data.repository.DefaultVideoRepository
import reelsdrama.freedrama.videosdrama.data.repository.RewardsRepositoryImpl
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import reelsdrama.freedrama.videosdrama.domain.repository.VideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVideoRepository(repository: DefaultVideoRepository): VideoRepository

    @Binds
    @Singleton
    abstract fun bindRewardsRepository(repository: RewardsRepositoryImpl): RewardsRepository
}
