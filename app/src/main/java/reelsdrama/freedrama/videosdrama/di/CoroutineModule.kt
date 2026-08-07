package reelsdrama.freedrama.videosdrama.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import reelsdrama.freedrama.videosdrama.core.util.DefaultDispatcherProvider
import reelsdrama.freedrama.videosdrama.core.util.DispatcherProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoroutineModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(provider: DefaultDispatcherProvider): DispatcherProvider
}
