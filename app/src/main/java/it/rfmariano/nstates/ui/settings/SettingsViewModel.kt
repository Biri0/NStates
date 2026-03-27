package it.rfmariano.nstates.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val settingsDataSource: SettingsDataSource,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var pendingOpenRouterApiKey: String? = null

    init {
        viewModelScope.launch {
            val primarySettings = combine(
                repository.activeNation,
                settingsDataSource.initialPage,
                settingsDataSource.issueNotificationsEnabled,
                settingsDataSource.openRouterApiKey,
                settingsDataSource.openRouterZdrOnly
            ) { activeNation, initialPage, issueNotificationsEnabled, openRouterApiKey, openRouterZdrOnly ->
                PrimarySettingsSnapshot(
                    activeNation = activeNation,
                    initialPage = initialPage,
                    issueNotificationsEnabled = issueNotificationsEnabled,
                    openRouterApiKey = openRouterApiKey,
                    openRouterZdrOnly = openRouterZdrOnly
                )
            }

            combine(
                primarySettings,
                settingsDataSource.deepLApiKey,
                settingsDataSource.issueTranslationEnabled,
                settingsDataSource.issueTranslationAutoEnabled,
                settingsDataSource.issueTranslationTargetLang
            ) { primary, deepLApiKey, issueTranslationEnabled, issueTranslationAutoEnabled, issueTranslationTargetLang ->
                SettingsSnapshot(
                    activeNation = primary.activeNation,
                    initialPage = primary.initialPage,
                    issueNotificationsEnabled = primary.issueNotificationsEnabled,
                    openRouterApiKey = primary.openRouterApiKey,
                    openRouterZdrOnly = primary.openRouterZdrOnly,
                    deepLApiKey = deepLApiKey,
                    issueTranslationEnabled = issueTranslationEnabled,
                    issueTranslationAutoEnabled = issueTranslationAutoEnabled,
                    issueTranslationTargetLang = issueTranslationTargetLang
                )
            }.collectLatest { snapshot ->
                updateState(snapshot)
            }
        }
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

    fun setDeepLApiKey(apiKey: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Ready) {
            _uiState.value = current.copy(deepLApiKey = apiKey)
            viewModelScope.launch {
                settingsDataSource.setDeepLApiKey(apiKey)
            }
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
                    deepLApiKey = current.deepLApiKey,
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
            deepLApiKey = snapshot.deepLApiKey,
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
        val deepLApiKey: String,
        val issueTranslationEnabled: Boolean,
        val issueTranslationAutoEnabled: Boolean,
        val issueTranslationTargetLang: String
    )

    private data class PrimarySettingsSnapshot(
        val activeNation: String?,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean
    )
}
