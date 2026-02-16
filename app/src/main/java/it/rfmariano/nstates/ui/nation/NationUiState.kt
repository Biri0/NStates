package it.rfmariano.nstates.ui.nation

import it.rfmariano.nstates.data.model.NationData

sealed interface NationUiState {
    data object Loading : NationUiState
    data class Success(val nation: NationData, val userAgent: String) : NationUiState
    data class Error(val message: String) : NationUiState
}
