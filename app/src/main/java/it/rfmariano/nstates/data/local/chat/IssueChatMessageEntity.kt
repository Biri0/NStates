package it.rfmariano.nstates.data.local.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "issue_chat_messages")
data class IssueChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nationName: String,
    val issueId: Int,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
