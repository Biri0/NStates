package it.rfmariano.nstates.data.local.chat

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [IssueChatMessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NStatesDatabase : RoomDatabase() {
    abstract fun issueChatDao(): IssueChatDao
}
