package it.rfmariano.nstates.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.rfmariano.nstates.data.api.ApiException
import it.rfmariano.nstates.data.repository.NationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: NationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _nationName = MutableStateFlow("")
    val nationName: StateFlow<String> = _nationName.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    init {
        // Pre-fill nation name if the user has logged in before
        repository.getCurrentNationName()?.let { _nationName.value = it }
        tryResumeSession()
    }

    fun onNationNameChanged(name: String) {
        _nationName.value = name
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
    }

    fun login() {
        val name = _nationName.value.trim()
        val pass = _password.value

        if (name.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Nation name and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.login(name, pass)
                .onSuccess { nation ->
                    _password.value = "" // Clear password from memory
                    _uiState.value = LoginUiState.Success(nation)
                }
                .onFailure { error ->
                    _uiState.value = LoginUiState.Error(formatError(error))
                }
        }
    }

    private fun tryResumeSession() {
        if (!repository.isLoggedIn()) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.resumeSession()
                .onSuccess { nation ->
                    _nationName.value = repository.getCurrentNationName() ?: ""
                    _uiState.value = LoginUiState.Success(nation)
                }
                .onFailure {
                    // Session expired, user needs to re-login
                    _nationName.value = repository.getCurrentNationName() ?: ""
                    _uiState.value = LoginUiState.Idle
                }
        }
    }

    private fun formatError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                403 -> "Invalid credentials or missing User-Agent"
                404 -> "Nation not found"
                409 -> "Too many login attempts. Wait a few seconds and try again."
                429 -> "Rate limited. Please wait before trying again."
                else -> "API error: ${error.message}"
            }
            else -> error.message ?: "Unknown error occurred"
        }
    }
}
