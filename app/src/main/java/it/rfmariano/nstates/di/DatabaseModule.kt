package it.rfmariano.nstates.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.rfmariano.nstates.data.local.chat.IssueChatDao
import it.rfmariano.nstates.data.local.chat.NStatesDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NStatesDatabase {
        return Room.databaseBuilder(
            context,
            NStatesDatabase::class.java,
            "nstates.db"
        ).build()
    }

    @Provides
    fun provideIssueChatDao(database: NStatesDatabase): IssueChatDao = database.issueChatDao()
}
