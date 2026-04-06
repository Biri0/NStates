package it.rfmariano.nstates.ui.issues

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import it.rfmariano.nstates.data.api.OpenRouterApiClient
import it.rfmariano.nstates.data.api.IssueAnswerParseException
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.repository.IssueChatRepository
import it.rfmariano.nstates.data.repository.IssueTranslationRepository
import it.rfmariano.nstates.data.repository.NationRepository
import it.rfmariano.nstates.data.translation.DeepLLanguageSupport
import it.rfmariano.nstates.notifications.NextIssueNotificationScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IssuesViewModel @Inject constructor(
    private val repository: NationRepository,
    private val issueChatRepository: IssueChatRepository,
    private val issueTranslationRepository: IssueTranslationRepository,
    private val settingsDataSource: SettingsDataSource,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<IssuesUiState>(IssuesUiState.Loading)
    val uiState: StateFlow<IssuesUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<IssueActionState>(IssueActionState.Idle)
    val actionState: StateFlow<IssueActionState> = _actionState.asStateFlow()

    /** The currently selected issue for detail view. */
    private val _selectedIssue = MutableStateFlow<Issue?>(null)
    val selectedIssue: StateFlow<Issue?> = _selectedIssue.asStateFlow()
    private var selectedIssueOriginal: Issue? = null

    private val _issueTranslationState = MutableStateFlow(IssueTranslationUiState())
    val issueTranslationState: StateFlow<IssueTranslationUiState> = _issueTranslationState.asStateFlow()

    private val _chatState = MutableStateFlow(IssueChatUiState())
    val chatState: StateFlow<IssueChatUiState> = _chatState.asStateFlow()

    private var chatObserverJob: Job? = null
    private var deepLApiKey: String = ""
    private var issueTranslationEnabled: Boolean = false
    private var issueTranslationAutoEnabled: Boolean = false
    private var issueTranslationTargetLang: String = "EN"

    val userAgent: StateFlow<String> = settingsDataSource.userAgent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val notificationsEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repository.activeNation
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest {
                    loadIssuesInternal()
                }
        }

        viewModelScope.launch {
            settingsDataSource.issueNotificationsEnabled.collectLatest { enabled ->
                notificationsEnabled.value = enabled
                if (!enabled) {
                    NextIssueNotificationScheduler.cancel(appContext)
                }
            }
        }

        viewModelScope.launch {
            settingsDataSource.openRouterApiKey.collectLatest { apiKey ->
                _chatState.value = _chatState.value.copy(
                    isApiKeyConfigured = apiKey.isNotBlank()
                )
            }
        }

        viewModelScope.launch {
            settingsDataSource.openRouterZdrOnly.collectLatest { enabled ->
                _chatState.value = _chatState.value.copy(
                    openRouterZdrOnly = enabled
                )
            }
        }

        viewModelScope.launch {
            settingsDataSource.openRouterModelId.collectLatest { modelId ->
                _chatState.value = _chatState.value.copy(
                    openRouterModelId = modelId
                )
            }
        }

        viewModelScope.launch {
            combine(
                settingsDataSource.deepLApiKey,
                settingsDataSource.issueTranslationEnabled,
                settingsDataSource.issueTranslationAutoEnabled,
                settingsDataSource.issueTranslationTargetLang
            ) { apiKey, enabled, autoEnabled, targetLang ->
                TranslationSettingsSnapshot(
                    apiKey = apiKey,
                    enabled = enabled,
                    autoEnabled = autoEnabled,
                    targetLang = DeepLLanguageSupport.normalizeOrDefault(targetLang)
                )
            }.collectLatest { snapshot ->
                deepLApiKey = snapshot.apiKey
                issueTranslationEnabled = snapshot.enabled
                issueTranslationAutoEnabled = snapshot.autoEnabled
                issueTranslationTargetLang = snapshot.targetLang
                updateIssueTranslationAvailability()
                maybeAutoTranslateSelectedIssue()
            }
        }
    }

    fun loadIssues() {
        viewModelScope.launch {
            loadIssuesInternal()
        }
    }

    private suspend fun loadIssuesInternal() {
        _uiState.value = IssuesUiState.Loading
        repository.fetchIssues()
            .onSuccess { issuesData ->
                _uiState.value = IssuesUiState.Success(
                    issues = issuesData.issues,
                    nextIssueTime = issuesData.nextIssueTime
                )
                if (notificationsEnabled.value) {
                    NextIssueNotificationScheduler.schedule(
                        context = appContext,
                        nextIssueTimeSeconds = issuesData.nextIssueTime
                    )
                } else {
                    NextIssueNotificationScheduler.cancel(appContext)
                }
            }
            .onFailure { error ->
                _uiState.value = IssuesUiState.Error(
                    formatErrorMessage(error)
                )
            }
    }

    fun selectIssue(issue: Issue) {
        selectedIssueOriginal = issue
        _selectedIssue.value = issue
        _issueTranslationState.value = IssueTranslationUiState()
        updateIssueTranslationAvailability()
        maybeAutoTranslateSelectedIssue()
        observeChat(issue.id)
    }

    fun clearSelectedIssue() {
        selectedIssueOriginal = null
        _selectedIssue.value = null
        _issueTranslationState.value = IssueTranslationUiState()
        chatObserverJob?.cancel()
        _chatState.value = _chatState.value.copy(
            messages = emptyList(),
            streamingMessage = "",
            isSending = false,
            attempt = 0,
            errorMessage = null
        )
    }

    fun setIssueTranslationEnabledForSelectedIssue(enabled: Boolean) {
        val original = selectedIssueOriginal ?: return
        if (!canShowIssueTranslationToggle()) return

        if (!enabled) {
            _selectedIssue.value = original
            _issueTranslationState.value = _issueTranslationState.value.copy(
                isTranslated = false,
                isTranslating = false,
                errorMessage = null
            )
            return
        }

        viewModelScope.launch {
            _issueTranslationState.value = _issueTranslationState.value.copy(
                isTranslating = true,
                errorMessage = null
            )
            issueTranslationRepository.translateIssues(
                issues = listOf(original),
                apiKey = deepLApiKey,
                targetLang = issueTranslationTargetLang
            ).fold(
                onSuccess = { translatedIssues ->
                    val translatedIssue = translatedIssues.single()
                    _selectedIssue.value = translatedIssue
                    _issueTranslationState.value = _issueTranslationState.value.copy(
                        isTranslated = true,
                        isTranslating = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _selectedIssue.value = original
                    _issueTranslationState.value = _issueTranslationState.value.copy(
                        isTranslated = false,
                        isTranslating = false,
                        errorMessage = "Issue translation failed: ${error.message ?: "unknown error"}"
                    )
                }
            )
        }
    }

    /**
     * Request confirmation before answering an issue.
     *
     * @param issue The issue being answered
     * @param optionId The option ID (0-based), or -1 for dismiss
     */
    fun requestAnswer(issue: Issue, optionId: Int) {
        val optionText = if (optionId == -1) {
            "Dismiss this issue"
        } else {
            issue.options.find { it.id == optionId }?.text ?: "Option $optionId"
        }
        _actionState.value = IssueActionState.Confirming(
            issue = issue,
            optionId = optionId,
            optionText = optionText
        )
    }

    /**
     * Confirm and execute the issue answer.
     */
    fun confirmAnswer() {
        val confirming = _actionState.value as? IssueActionState.Confirming ?: return

        viewModelScope.launch {
            _actionState.value = IssueActionState.Submitting
            repository.answerIssue(confirming.issue.id, confirming.optionId)
                .onSuccess { result ->
                    _actionState.value = IssueActionState.ResultReady(result)
                    // Remove the answered issue from the list
                    removeIssueFromList(confirming.issue.id)
                    // Clear selected issue since it's been answered
                    _selectedIssue.value = null
                }
                .onFailure { error ->
                    _actionState.value = if (error is IssueAnswerParseException) {
                        IssueActionState.ActionError(
                            message = "Failed to parse the server response after answering this issue.",
                            copyPayload = error.rawResponse
                        )
                    } else {
                        IssueActionState.ActionError(
                            message = formatErrorMessage(error)
                        )
                    }
                }
        }
    }

    /**
     * Cancel the confirmation dialog.
     */
    fun cancelAction() {
        _actionState.value = IssueActionState.Idle
    }

    /**
     * Dismiss the result or error dialog.
     */
    fun dismissResult() {
        _actionState.value = IssueActionState.Idle
    }

    private fun removeIssueFromList(issueId: Int) {
        val current = _uiState.value as? IssuesUiState.Success ?: return
        _uiState.value = current.copy(
            issues = current.issues.filter { it.id != issueId }
        )
    }

    private fun updateIssueTranslationAvailability() {
        val toggleVisible = canShowIssueTranslationToggle()
        if (!toggleVisible) {
            selectedIssueOriginal?.let { original ->
                _selectedIssue.value = original
            }
        }
        _issueTranslationState.value = if (toggleVisible) {
            _issueTranslationState.value.copy(
                isToggleVisible = true,
                targetLanguageCode = issueTranslationTargetLang
            )
        } else {
            IssueTranslationUiState()
        }
    }

    private fun maybeAutoTranslateSelectedIssue() {
        if (!issueTranslationAutoEnabled) return
        val translationState = _issueTranslationState.value
        if (!translationState.isToggleVisible || translationState.isTranslated || translationState.isTranslating) {
            return
        }
        setIssueTranslationEnabledForSelectedIssue(enabled = true)
    }

    private fun canShowIssueTranslationToggle(): Boolean {
        return selectedIssueOriginal != null &&
            deepLApiKey.isNotBlank() &&
            issueTranslationEnabled &&
            DeepLLanguageSupport.isTranslatableTarget(issueTranslationTargetLang)
    }

    fun clearChatConversation() {
        val issue = _selectedIssue.value ?: return
        viewModelScope.launch {
            issueChatRepository.clearConversation(issue.id)
            _chatState.value = _chatState.value.copy(
                streamingMessage = "",
                isSending = false,
                attempt = 0,
                errorMessage = null
            )
        }
    }

    fun sendChatMessage(message: String) {
        val issue = _selectedIssue.value ?: return
        val userMessage = message.trim()
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            val apiKey = settingsDataSource.openRouterApiKey.first()
            if (apiKey.isBlank()) {
                _chatState.value = _chatState.value.copy(
                    errorMessage = "Set your OpenRouter API key in Settings to use issue chat."
                )
                return@launch
            }

            issueChatRepository.addUserMessage(issue.id, userMessage)
            _chatState.value = _chatState.value.copy(
                isSending = true,
                attempt = 0,
                errorMessage = null,
                streamingMessage = ""
            )

            val maxAttempts = _chatState.value.maxAttempts
            var lastError: Throwable? = null

            for (attempt in 1..maxAttempts) {
                _chatState.value = _chatState.value.copy(attempt = attempt, streamingMessage = "")
                val collected = StringBuilder()
                val result = runCatching {
                    val conversation = issueChatRepository.getMessages(issue.id)
                    issueChatRepository.streamAssistantReply(
                        issue = issue,
                        messages = conversation,
                        apiKey = apiKey,
                        modelId = _chatState.value.openRouterModelId.ifBlank { OpenRouterApiClient.DEFAULT_MODEL },
                        openRouterZdrOnly = _chatState.value.openRouterZdrOnly
                    ).collect { token ->
                        collected.append(token)
                        _chatState.value = _chatState.value.copy(
                            streamingMessage = collected.toString()
                        )
                    }
                }

                if (result.isSuccess && collected.isNotBlank()) {
                    issueChatRepository.addAssistantMessage(issue.id, collected.toString())
                    _chatState.value = _chatState.value.copy(
                        isSending = false,
                        attempt = 0,
                        streamingMessage = "",
                        errorMessage = null
                    )
                    return@launch
                }

                lastError = result.exceptionOrNull()
                    ?: IllegalStateException("Empty response from OpenRouter.")
                if (attempt < maxAttempts) {
                    delay(500L * (1 shl (attempt - 1)))
                }
            }

            _chatState.value = _chatState.value.copy(
                isSending = false,
                streamingMessage = "",
                errorMessage = lastError?.message ?: "Failed to get AI response."
            )
        }
    }

    private fun observeChat(issueId: Int) {
        chatObserverJob?.cancel()
        chatObserverJob = viewModelScope.launch {
            issueChatRepository.observeMessages(issueId).collectLatest { messages ->
                _chatState.value = _chatState.value.copy(messages = messages)
            }
        }
    }

    private fun formatErrorMessage(error: Throwable): String {
        return when {
            error is it.rfmariano.nstates.data.api.ApiException -> {
                when (error.statusCode) {
                    400 -> "Bad request. The issue may have already been answered."
                    403 -> "Session expired. Please log in again."
                    404 -> "Nation not found."
                    409 -> "Conflict. Please wait a moment and try again."
                    429 -> "Rate limit exceeded. Please wait before trying again."
                    else -> "API error (${error.statusCode}): ${error.message}"
                }
            }
            error.message != null -> error.message!!
            else -> "An unknown error occurred"
        }
    }

    private data class TranslationSettingsSnapshot(
        val apiKey: String,
        val enabled: Boolean,
        val autoEnabled: Boolean,
        val targetLang: String
    )
}
