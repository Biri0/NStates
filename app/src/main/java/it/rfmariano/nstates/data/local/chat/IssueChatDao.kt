package it.rfmariano.nstates.data.local.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueChatDao {
    @Query(
        """
        SELECT * FROM issue_chat_messages
        WHERE nationName = :nationName AND issueId = :issueId
        ORDER BY id ASC
        """
    )
    fun observeMessages(nationName: String, issueId: Int): Flow<List<IssueChatMessageEntity>>

    @Query(
        """
        SELECT * FROM issue_chat_messages
        WHERE nationName = :nationName AND issueId = :issueId
        ORDER BY id ASC
        """
    )
    suspend fun getMessages(nationName: String, issueId: Int): List<IssueChatMessageEntity>

    @Insert
    suspend fun insertMessage(message: IssueChatMessageEntity)

    @Query(
        """
        DELETE FROM issue_chat_messages
        WHERE nationName = :nationName AND issueId = :issueId
        """
    )
    suspend fun clearConversation(nationName: String, issueId: Int)
}
