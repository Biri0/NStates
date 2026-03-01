package it.rfmariano.nstates.data.model

data class IssueChatMessage(
    val id: Long,
    val role: IssueChatRole,
    val content: String
)

enum class IssueChatRole {
    USER,
    ASSISTANT
}
