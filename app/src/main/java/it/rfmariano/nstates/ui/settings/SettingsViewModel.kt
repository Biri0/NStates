package it.rfmariano.nstates.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import it.rfmariano.nstates.data.api.DeepLTranslationClient
import it.rfmariano.nstates.data.api.OpenRouterApiClient
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.repository.NationRepository
import it.rfmariano.nstates.notifications.NextIssueNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NationRepository,
    private val deepLClient: DeepLTranslationClient,
    private val openRouterApiClient: OpenRouterApiClient,
    private val settingsDataSource: SettingsDataSource,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var pendingOpenRouterApiKey: String? = null
    private val deepLUsageRefreshing = MutableStateFlow(false)
    private val deepLUsageErrorMessage = MutableStateFlow<String?>(null)
    private val openRouterModelCatalog = MutableStateFlow<List<OpenRouterApiClient.ModelInfo>>(emptyList())
    private val recentOpenRouterModelId = MutableStateFlow<String?>(null)
    private val openRouterModelsLoading = MutableStateFlow(false)
    private val openRouterModelsErrorMessage = MutableStateFlow<String?>(null)
    private val openRouterPriceFilter = MutableStateFlow(SettingsUiState.OpenRouterPriceFilter.ALL)

    init {
        viewModelScope.launch {
            val openRouterPrimarySettings = combine(
                settingsDataSource.openRouterApiKey,
                settingsDataSource.openRouterZdrOnly,
                settingsDataSource.openRouterModelId
            ) { openRouterApiKey, openRouterZdrOnly, openRouterModelId ->
                OpenRouterPrimarySettingsSnapshot(
                    openRouterApiKey = openRouterApiKey,
                    openRouterZdrOnly = openRouterZdrOnly,
                    openRouterModelId = openRouterModelId
                )
            }

            val primarySettings = combine(
                repository.activeNation,
                settingsDataSource.initialPage,
                settingsDataSource.issueNotificationsEnabled,
                openRouterPrimarySettings
            ) { activeNation, initialPage, issueNotificationsEnabled, openRouter ->
                PrimarySettingsSnapshot(
                    activeNation = activeNation,
                    initialPage = initialPage,
                    issueNotificationsEnabled = issueNotificationsEnabled,
                    openRouterApiKey = openRouter.openRouterApiKey,
                    openRouterZdrOnly = openRouter.openRouterZdrOnly,
                    openRouterModelId = openRouter.openRouterModelId
                )
            }

            val deepLUsageState = combine(
                settingsDataSource.deepLUsageCharacterCount,
                settingsDataSource.deepLUsageCharacterLimit,
                deepLUsageRefreshing,
                deepLUsageErrorMessage
            ) { characterCount, characterLimit, refreshing, errorMessage ->
                DeepLUsageSnapshot(
                    characterCount = characterCount,
                    characterLimit = characterLimit,
                    refreshing = refreshing,
                    errorMessage = errorMessage
                )
            }

            val openRouterModelsState = combine(
                openRouterModelCatalog,
                recentOpenRouterModelId,
                openRouterModelsLoading,
                openRouterModelsErrorMessage,
                openRouterPriceFilter
            ) { openRouterModels, recentModelId, modelsLoading, modelsErrorMessage, priceFilter ->
                OpenRouterModelsSnapshot(
                    openRouterModels = openRouterModels,
                    recentModelId = recentModelId,
                    openRouterModelsLoading = modelsLoading,
                    openRouterModelsErrorMessage = modelsErrorMessage,
                    openRouterPriceFilter = priceFilter
                )
            }

            val translationSettings = combine(
                settingsDataSource.deepLApiKey,
                deepLUsageState,
                settingsDataSource.issueTranslationEnabled,
                settingsDataSource.issueTranslationAutoEnabled
            ) { deepLApiKey, usageState, issueTranslationEnabled, issueTranslationAutoEnabled ->
                TranslationSettingsWithoutTargetLanguageSnapshot(
                    deepLApiKey = deepLApiKey,
                    deepLUsageCharacterCount = usageState.characterCount,
                    deepLUsageCharacterLimit = usageState.characterLimit,
                    deepLUsageRefreshing = usageState.refreshing,
                    deepLUsageErrorMessage = usageState.errorMessage,
                    issueTranslationEnabled = issueTranslationEnabled,
                    issueTranslationAutoEnabled = issueTranslationAutoEnabled
                )
            }

            val settingsWithoutTargetLanguage = combine(
                primarySettings,
                translationSettings,
                openRouterModelsState
            ) { primary, translation, openRouterState ->
                val selectedModel = openRouterState.openRouterModels
                    .firstOrNull { it.id == primary.openRouterModelId }
                val selectedModelLabel = selectedModel?.name
                    ?: if (primary.openRouterModelId == OpenRouterApiClient.DEFAULT_MODEL) {
                        "OpenRouter default"
                    } else {
                        primary.openRouterModelId
                    }
                val filteredModels = when (openRouterState.openRouterPriceFilter) {
                    SettingsUiState.OpenRouterPriceFilter.ALL -> openRouterState.openRouterModels
                    SettingsUiState.OpenRouterPriceFilter.FREE -> openRouterState.openRouterModels.filter { it.isFree }
                    SettingsUiState.OpenRouterPriceFilter.PREMIUM -> openRouterState.openRouterModels.filter { !it.isFree }
                }.map { model ->
                    val isRecent = openRouterState.recentModelId?.equals(model.id, ignoreCase = true) == true
                    SettingsUiState.OpenRouterModelOption(
                        id = model.id,
                        label = model.name,
                        isFree = model.isFree,
                        isRecent = isRecent
                    )
                }
                SettingsSnapshotWithoutTargetLanguage(
                    activeNation = primary.activeNation,
                    initialPage = primary.initialPage,
                    issueNotificationsEnabled = primary.issueNotificationsEnabled,
                    openRouterApiKey = primary.openRouterApiKey,
                    openRouterZdrOnly = primary.openRouterZdrOnly,
                    openRouterSelectedModelId = primary.openRouterModelId,
                    openRouterSelectedModelLabel = selectedModelLabel,
                    openRouterModels = filteredModels,
                    openRouterModelsLoading = openRouterState.openRouterModelsLoading,
                    openRouterModelsErrorMessage = openRouterState.openRouterModelsErrorMessage,
                    openRouterPriceFilter = openRouterState.openRouterPriceFilter,
                    deepLApiKey = translation.deepLApiKey,
                    deepLUsageCharacterCount = translation.deepLUsageCharacterCount,
                    deepLUsageCharacterLimit = translation.deepLUsageCharacterLimit,
                    deepLUsageRefreshing = translation.deepLUsageRefreshing,
                    deepLUsageErrorMessage = translation.deepLUsageErrorMessage,
                    issueTranslationEnabled = translation.issueTranslationEnabled,
                    issueTranslationAutoEnabled = translation.issueTranslationAutoEnabled
                )
            }

            combine(
                settingsWithoutTargetLanguage,
                settingsDataSource.issueTranslationTargetLang
            ) { snapshot, issueTranslationTargetLang ->
                SettingsSnapshot(
                    activeNation = snapshot.activeNation,
                    initialPage = snapshot.initialPage,
                    issueNotificationsEnabled = snapshot.issueNotificationsEnabled,
                    openRouterApiKey = snapshot.openRouterApiKey,
                    openRouterZdrOnly = snapshot.openRouterZdrOnly,
                    openRouterSelectedModelId = snapshot.openRouterSelectedModelId,
                    openRouterSelectedModelLabel = snapshot.openRouterSelectedModelLabel,
                    openRouterModels = snapshot.openRouterModels,
                    openRouterModelsLoading = snapshot.openRouterModelsLoading,
                    openRouterModelsErrorMessage = snapshot.openRouterModelsErrorMessage,
                    openRouterPriceFilter = snapshot.openRouterPriceFilter,
                    deepLApiKey = snapshot.deepLApiKey,
                    deepLUsageCharacterCount = snapshot.deepLUsageCharacterCount,
                    deepLUsageCharacterLimit = snapshot.deepLUsageCharacterLimit,
                    deepLUsageRefreshing = snapshot.deepLUsageRefreshing,
                    deepLUsageErrorMessage = snapshot.deepLUsageErrorMessage,
                    issueTranslationEnabled = snapshot.issueTranslationEnabled,
                    issueTranslationAutoEnabled = snapshot.issueTranslationAutoEnabled,
                    issueTranslationTargetLang = issueTranslationTargetLang
                )
            }.collectLatest { snapshot ->
                updateState(snapshot)
            }
        }
        refreshOpenRouterModels()
    }

    fun setInitialPage(route: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(initialPage = route)
            viewModelScope.launch {
                settingsDataSource.setInitialPage(route)
            }
        }
    }

    fun setIssueNotificationsEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(issueNotificationsEnabled = enabled)
            viewModelScope.launch {
                settingsDataSource.setIssueNotificationsEnabled(enabled)
            }
            if (!enabled) {
                NextIssueNotificationScheduler.cancel(appContext)
            }
        }
    }

    fun setOpenRouterApiKey(apiKey: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            pendingOpenRouterApiKey = apiKey
            _uiState.value = current.copy(openRouterApiKey = apiKey)
            viewModelScope.launch {
                settingsDataSource.setOpenRouterApiKey(apiKey)
                refreshOpenRouterModels()
            }
        }
    }

    fun setOpenRouterZdrOnly(enabled: Boolean) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(openRouterZdrOnly = enabled)
            viewModelScope.launch {
                settingsDataSource.setOpenRouterZdrOnly(enabled)
            }
        }
    }

    fun setOpenRouterModelId(modelId: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            val selected = current.openRouterModels.firstOrNull { it.id == modelId }
            val selectedLabel = selected?.label ?: modelId
            _uiState.value = current.copy(
                openRouterSelectedModelId = modelId,
                openRouterSelectedModelLabel = selectedLabel
            )
            recentOpenRouterModelId.value = modelId
            viewModelScope.launch {
                settingsDataSource.setOpenRouterModelId(modelId)
            }
        }
    }

    fun setOpenRouterPriceFilter(filter: SettingsUiState.OpenRouterPriceFilter) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(openRouterPriceFilter = filter)
            openRouterPriceFilter.value = filter
        }
    }

    fun refreshOpenRouterModels() {
        if (openRouterModelsLoading.value) return
        viewModelScope.launch {
            openRouterModelsLoading.value = true
            openRouterModelsErrorMessage.value = null
            val apiKey = (_uiState.value as? SettingsUiState.Ready)
                ?.openRouterApiKey
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            runCatching {
                openRouterApiClient.fetchModels(apiKey = apiKey)
            }.onSuccess { models ->
                openRouterModelCatalog.value = models
                openRouterModelsErrorMessage.value = null
            }.onFailure { error ->
                openRouterModelsErrorMessage.value =
                    error.message ?: "Failed to fetch OpenRouter models."
            }
            openRouterModelsLoading.value = false
        }
    }

    fun setDeepLApiKey(apiKey: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(
                deepLApiKey = apiKey,
                deepLUsageErrorMessage = null
            )
            viewModelScope.launch {
                settingsDataSource.setDeepLApiKey(apiKey)
                if (apiKey.isBlank()) {
                    settingsDataSource.clearDeepLUsage()
                    deepLUsageErrorMessage.value = null
                }
            }
        }
    }

    fun refreshDeepLUsage() {
        val current = _uiState.value
        if (current !is SettingsUiState.Ready) return
        val apiKey = current.deepLApiKey.trim()
        if (apiKey.isBlank()) {
            deepLUsageRefreshing.value = false
            deepLUsageErrorMessage.value = "Add a DeepL API key to load usage."
            return
        }
        if (deepLUsageRefreshing.value) return
        viewModelScope.launch {
            deepLUsageRefreshing.value = true
            deepLUsageErrorMessage.value = null
            runCatching {
                deepLClient.fetchUsage(apiKey)
            }.onSuccess { usage ->
                settingsDataSource.setDeepLUsage(
                    characterCount = usage.characterCount,
                    characterLimit = usage.characterLimit
                )
                deepLUsageErrorMessage.value = null
            }.onFailure { error ->
                deepLUsageErrorMessage.value = error.message ?: "Failed to refresh DeepL usage."
            }
            deepLUsageRefreshing.value = false
        }
    }

    fun setIssueTranslationEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(issueTranslationEnabled = enabled)
            viewModelScope.launch {
                settingsDataSource.setIssueTranslationEnabled(enabled)
            }
        }
    }

    fun setIssueTranslationTargetLang(languageCode: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(issueTranslationTargetLang = languageCode)
            viewModelScope.launch {
                settingsDataSource.setIssueTranslationTargetLang(languageCode)
            }
        }
    }

    fun setIssueTranslationAutoEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(issueTranslationAutoEnabled = enabled)
            viewModelScope.launch {
                settingsDataSource.setIssueTranslationAutoEnabled(enabled)
            }
        }
    }

    fun switchAccount(nationName: String) {
        repository.switchAccount(nationName)
    }

    fun removeAccount(nationName: String): Int {
        val remaining = repository.removeAccount(nationName)
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            updateState(
                SettingsSnapshot(
                    activeNation = repository.getCurrentNationName(),
                    initialPage = current.initialPage,
                    issueNotificationsEnabled = current.issueNotificationsEnabled,
                    openRouterApiKey = current.openRouterApiKey,
                    openRouterZdrOnly = current.openRouterZdrOnly,
                    openRouterSelectedModelId = current.openRouterSelectedModelId,
                    openRouterSelectedModelLabel = current.openRouterSelectedModelLabel,
                    openRouterModels = current.openRouterModels,
                    openRouterModelsLoading = current.openRouterModelsLoading,
                    openRouterModelsErrorMessage = current.openRouterModelsErrorMessage,
                    openRouterPriceFilter = current.openRouterPriceFilter,
                    deepLApiKey = current.deepLApiKey,
                    deepLUsageCharacterCount = current.deepLUsageCharacterCount,
                    deepLUsageCharacterLimit = current.deepLUsageCharacterLimit,
                    deepLUsageRefreshing = current.deepLUsageRefreshing,
                    deepLUsageErrorMessage = current.deepLUsageErrorMessage,
                    issueTranslationEnabled = current.issueTranslationEnabled,
                    issueTranslationAutoEnabled = current.issueTranslationAutoEnabled,
                    issueTranslationTargetLang = current.issueTranslationTargetLang
                )
            )
        }
        return remaining
    }

    private fun updateState(snapshot: SettingsSnapshot) {
        val accounts = repository.getAccounts()
            .map { it.nationName }
            .sortedBy { it.lowercase() }
        val resolvedOpenRouterApiKey = pendingOpenRouterApiKey?.let { pending ->
            if (pending.trim() == snapshot.openRouterApiKey) {
                pendingOpenRouterApiKey = null
                snapshot.openRouterApiKey
            } else {
                pending
            }
        } ?: snapshot.openRouterApiKey
        _uiState.value = SettingsUiState.Ready(
            nationName = snapshot.activeNation ?: "",
            accounts = accounts,
            initialPage = snapshot.initialPage,
            issueNotificationsEnabled = snapshot.issueNotificationsEnabled,
            openRouterApiKey = resolvedOpenRouterApiKey,
            openRouterZdrOnly = snapshot.openRouterZdrOnly,
            openRouterSelectedModelId = snapshot.openRouterSelectedModelId,
            openRouterSelectedModelLabel = snapshot.openRouterSelectedModelLabel,
            openRouterModels = snapshot.openRouterModels,
            openRouterModelsLoading = snapshot.openRouterModelsLoading,
            openRouterModelsErrorMessage = snapshot.openRouterModelsErrorMessage,
            openRouterPriceFilter = snapshot.openRouterPriceFilter,
            deepLApiKey = snapshot.deepLApiKey,
            deepLUsageCharacterCount = snapshot.deepLUsageCharacterCount,
            deepLUsageCharacterLimit = snapshot.deepLUsageCharacterLimit,
            deepLUsageRefreshing = snapshot.deepLUsageRefreshing,
            deepLUsageErrorMessage = snapshot.deepLUsageErrorMessage,
            issueTranslationEnabled = snapshot.issueTranslationEnabled,
            issueTranslationAutoEnabled = snapshot.issueTranslationAutoEnabled,
            issueTranslationTargetLang = snapshot.issueTranslationTargetLang
        )
    }

    private data class SettingsSnapshot(
        val activeNation: String?,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean,
        val openRouterSelectedModelId: String,
        val openRouterSelectedModelLabel: String,
        val openRouterModels: List<SettingsUiState.OpenRouterModelOption>,
        val openRouterModelsLoading: Boolean,
        val openRouterModelsErrorMessage: String?,
        val openRouterPriceFilter: SettingsUiState.OpenRouterPriceFilter,
        val deepLApiKey: String,
        val deepLUsageCharacterCount: Long?,
        val deepLUsageCharacterLimit: Long?,
        val deepLUsageRefreshing: Boolean,
        val deepLUsageErrorMessage: String?,
        val issueTranslationEnabled: Boolean,
        val issueTranslationAutoEnabled: Boolean,
        val issueTranslationTargetLang: String
    )

    private data class SettingsSnapshotWithoutTargetLanguage(
        val activeNation: String?,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean,
        val openRouterSelectedModelId: String,
        val openRouterSelectedModelLabel: String,
        val openRouterModels: List<SettingsUiState.OpenRouterModelOption>,
        val openRouterModelsLoading: Boolean,
        val openRouterModelsErrorMessage: String?,
        val openRouterPriceFilter: SettingsUiState.OpenRouterPriceFilter,
        val deepLApiKey: String,
        val deepLUsageCharacterCount: Long?,
        val deepLUsageCharacterLimit: Long?,
        val deepLUsageRefreshing: Boolean,
        val deepLUsageErrorMessage: String?,
        val issueTranslationEnabled: Boolean,
        val issueTranslationAutoEnabled: Boolean
    )

    private data class PrimarySettingsSnapshot(
        val activeNation: String?,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean,
        val openRouterModelId: String
    )

    private data class OpenRouterPrimarySettingsSnapshot(
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean,
        val openRouterModelId: String
    )

    private data class OpenRouterModelsSnapshot(
        val openRouterModels: List<OpenRouterApiClient.ModelInfo>,
        val recentModelId: String?,
        val openRouterModelsLoading: Boolean,
        val openRouterModelsErrorMessage: String?,
        val openRouterPriceFilter: SettingsUiState.OpenRouterPriceFilter
    )

    private data class TranslationSettingsWithoutTargetLanguageSnapshot(
        val deepLApiKey: String,
        val deepLUsageCharacterCount: Long?,
        val deepLUsageCharacterLimit: Long?,
        val deepLUsageRefreshing: Boolean,
        val deepLUsageErrorMessage: String?,
        val issueTranslationEnabled: Boolean,
        val issueTranslationAutoEnabled: Boolean
    )

    private data class DeepLUsageSnapshot(
        val characterCount: Long?,
        val characterLimit: Long?,
        val refreshing: Boolean,
        val errorMessage: String?
    )
}
