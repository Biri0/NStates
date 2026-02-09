package it.rfmariano.nstates.ui.login

import it.rfmariano.nstates.data.model.NationData

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val nation: NationData) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
