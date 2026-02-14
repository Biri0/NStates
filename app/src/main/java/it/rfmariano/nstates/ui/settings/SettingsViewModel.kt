package it.rfmariano.nstates.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.repository.NationRepository
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
    private val settingsDataSource: SettingsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.activeNation,
                settingsDataSource.initialPage
            ) { activeNation, initialPage ->
                SettingsSnapshot(
                    activeNation = activeNation,
                    initialPage = initialPage
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
                    initialPage = current.initialPage
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
            initialPage = snapshot.initialPage
        )
    }

    private data class SettingsSnapshot(
        val activeNation: String?,
        val initialPage: String
    )
}
