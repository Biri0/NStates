package it.rfmariano.nstates.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.repository.NationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            val nationName = repository.getCurrentNationName() ?: ""
            val initialPage = settingsDataSource.initialPage.first()
            _uiState.value = SettingsUiState.Ready(
                nationName = nationName,
                initialPage = initialPage
            )
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
}
