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
import kotlinx.coroutines.flow.first
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

    init {
        viewModelScope.launch {
            combine(
                repository.activeNation,
                settingsDataSource.initialPage,
                settingsDataSource.issueNotificationsEnabled
            ) { activeNation, initialPage, issueNotificationsEnabled ->
                SettingsSnapshot(
                    activeNation = activeNation,
                    initialPage = initialPage,
                    issueNotificationsEnabled = issueNotificationsEnabled
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

    fun logout() {
        repository.logout()
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
                    issueNotificationsEnabled = current.issueNotificationsEnabled
                )
            )
        }
        return remaining
    }

    private fun updateState(snapshot: SettingsSnapshot) {
        val accounts = repository.getAccounts()
            .map { it.nationName }
            .sortedBy { it.lowercase() }
        _uiState.value = SettingsUiState.Ready(
            nationName = snapshot.activeNation ?: "",
            accounts = accounts,
            initialPage = snapshot.initialPage,
            issueNotificationsEnabled = snapshot.issueNotificationsEnabled
        )
    }

    private data class SettingsSnapshot(
        val activeNation: String?,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean
    )
}
