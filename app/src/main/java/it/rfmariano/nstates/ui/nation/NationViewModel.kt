package it.rfmariano.nstates.ui.nation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.rfmariano.nstates.data.repository.NationRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NationViewModel @Inject constructor(
    private val repository: NationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NationUiState>(NationUiState.Loading)
    val uiState: StateFlow<NationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeNation
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest {
                    loadNationInternal()
                }
        }
    }

    fun loadNation() {
        viewModelScope.launch {
            loadNationInternal()
        }
    }

    private suspend fun loadNationInternal() {
        _uiState.value = NationUiState.Loading
        repository.fetchCurrentNation()
            .onSuccess { nation ->
                _uiState.value = NationUiState.Success(nation)
            }
            .onFailure { error ->
                _uiState.value = NationUiState.Error(
                    error.message ?: "Failed to load nation data"
                )
            }
    }

}
