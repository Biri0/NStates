package it.rfmariano.nstates.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.rfmariano.nstates.data.api.DeepLApiClient
import it.rfmariano.nstates.data.api.DeepLTranslationClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeepLModule {
    @Binds
    @Singleton
    abstract fun bindDeepLTranslationClient(
        impl: DeepLApiClient
    ): DeepLTranslationClient
}
