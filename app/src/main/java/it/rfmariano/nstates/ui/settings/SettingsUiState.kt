package it.rfmariano.nstates.ui.settings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(
        val nationName: String,
        val accounts: List<String>,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val issueNotificationAccounts: Set<String>
    ) : SettingsUiState
}
