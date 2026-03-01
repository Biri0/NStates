package it.rfmariano.nstates.ui.search

import it.rfmariano.nstates.data.model.NationData

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val nation: NationData? = null,
    val userAgent: String = "",
    val pinnedNations: List<String> = emptyList()
) {
    val isCurrentNationPinned: Boolean
        get() = nation?.let { current ->
            pinnedNations.any { it.equals(current.name, ignoreCase = true) }
        } ?: false
}
