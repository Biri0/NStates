package it.rfmariano.nstates.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.rfmariano.nstates.data.api.ApiException
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.repository.NationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: NationRepository,
    private val settingsDataSource: SettingsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settingsDataSource.userAgent, settingsDataSource.pinnedNations) { userAgent, pinned ->
                Pair(userAgent, pinned)
            }.collectLatest { (userAgent, pinned) ->
                _uiState.update { current ->
                    current.copy(
                        userAgent = userAgent,
                        pinnedNations = pinned
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun search() {
        val query = uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a nation name.") }
            return
        }
        searchNation(query)
    }

    fun searchPinnedNation(name: String) {
        _uiState.update { it.copy(query = name) }
        searchNation(name)
    }

    fun toggleCurrentNationPinned() {
        val nationName = uiState.value.nation?.name?.trim() ?: return
        viewModelScope.launch {
            if (uiState.value.isCurrentNationPinned) {
                settingsDataSource.removePinnedNation(nationName)
            } else {
                settingsDataSource.addPinnedNation(nationName)
            }
        }
    }

    private fun searchNation(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    nation = null
                )
            }

            repository.fetchNationByName(normalizeNationName(name))
                .onSuccess { nation ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nation = nation
                        )
                    }
                }
                .onFailure { error ->
                    val message = when ((error as? ApiException)?.statusCode) {
                        404 -> "Nation not found."
                        else -> error.message ?: "Failed to load nation data."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message
                        )
                    }
                }
        }
    }

    private fun normalizeNationName(name: String): String {
        return name.trim().replace(' ', '_')
    }
}
