package it.rfmariano.nstates.data.repository

import it.rfmariano.nstates.data.api.OpenRouterApiClient
import it.rfmariano.nstates.data.local.AuthLocalDataSource
import it.rfmariano.nstates.data.local.chat.IssueChatDao
import it.rfmariano.nstates.data.local.chat.IssueChatMessageEntity
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueChatMessage
import it.rfmariano.nstates.data.model.IssueChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IssueChatRepository @Inject constructor(
    private val issueChatDao: IssueChatDao,
    private val authLocalDataSource: AuthLocalDataSource,
    private val openRouterApiClient: OpenRouterApiClient
) {
    fun observeMessages(issueId: Int): Flow<List<IssueChatMessage>> {
        val nationName = requireNation()
        return issueChatDao.observeMessages(nationName, issueId)
            .map { messages -> messages.map { it.toModel() } }
    }

    suspend fun getMessages(issueId: Int): List<IssueChatMessage> {
        val nationName = requireNation()
        return issueChatDao.getMessages(nationName, issueId).map { it.toModel() }
    }

    suspend fun addUserMessage(issueId: Int, content: String) {
        insertMessage(issueId, IssueChatRole.USER, content)
    }

    suspend fun addAssistantMessage(issueId: Int, content: String) {
        insertMessage(issueId, IssueChatRole.ASSISTANT, content)
    }

    suspend fun clearConversation(issueId: Int) {
        val nationName = requireNation()
        issueChatDao.clearConversation(nationName, issueId)
    }

    fun streamAssistantReply(
        issue: Issue,
        messages: List<IssueChatMessage>,
        apiKey: String,
        openRouterZdrOnly: Boolean
    ): Flow<String> {
        val contextPrompt = buildContextPrompt(issue)
        val payload = buildList {
            add(
                OpenRouterApiClient.ChatMessage(
                    role = "system",
                    content = contextPrompt
                )
            )
            messages.forEach { message ->
                add(
                    OpenRouterApiClient.ChatMessage(
                        role = if (message.role == IssueChatRole.USER) "user" else "assistant",
                        content = message.content
                    )
                )
            }
        }
        return openRouterApiClient.streamChat(
            apiKey = apiKey,
            model = OpenRouterApiClient.DEFAULT_MODEL,
            messages = payload,
            openRouterZdrOnly = openRouterZdrOnly
        )
    }

    private suspend fun insertMessage(issueId: Int, role: IssueChatRole, content: String) {
        val nationName = requireNation()
        issueChatDao.insertMessage(
            IssueChatMessageEntity(
                nationName = nationName,
                issueId = issueId,
                role = role.name,
                content = content
            )
        )
    }

    private fun requireNation(): String {
        return authLocalDataSource.nationName
            ?: throw IllegalStateException("Not logged in")
    }

    private fun buildContextPrompt(issue: Issue): String {
        val options = issue.options.joinToString("\n") { option ->
            "${option.id + 1}. ${option.text}"
        }
        return """
            You are helping a NationStates player evaluate an issue.
            Explain options clearly, including tradeoffs and possible outcomes.
            
            Issue #${issue.id}: ${issue.title}
            Description:
            ${issue.text}
            
            Available options:
            $options
        """.trimIndent()
    }

    private fun IssueChatMessageEntity.toModel(): IssueChatMessage {
        val role = runCatching { IssueChatRole.valueOf(role) }
            .getOrDefault(IssueChatRole.ASSISTANT)
        return IssueChatMessage(
            id = id,
            role = role,
            content = content
        )
    }
}
