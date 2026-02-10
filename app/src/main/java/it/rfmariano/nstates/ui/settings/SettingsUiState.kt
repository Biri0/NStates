package it.rfmariano.nstates.ui.settings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val nationName: String) : SettingsUiState
}
