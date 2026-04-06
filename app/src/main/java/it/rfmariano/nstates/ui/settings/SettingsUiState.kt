package it.rfmariano.nstates.ui.settings

sealed interface SettingsUiState {
    data class OpenRouterModelOption(
        val id: String,
        val label: String,
        val isFree: Boolean,
        val isRecent: Boolean = false
    )

    enum class OpenRouterPriceFilter {
        ALL,
        FREE,
        PREMIUM
    }

    data object Loading : SettingsUiState
    data class Ready(
        val nationName: String,
        val accounts: List<String>,
        val initialPage: String,
        val issueNotificationsEnabled: Boolean,
        val openRouterApiKey: String,
        val openRouterZdrOnly: Boolean,
        val openRouterSelectedModelId: String,
        val openRouterSelectedModelLabel: String,
        val openRouterModels: List<OpenRouterModelOption>,
        val openRouterModelsLoading: Boolean,
        val openRouterModelsErrorMessage: String?,
        val openRouterPriceFilter: OpenRouterPriceFilter,
        val deepLApiKey: String,
        val deepLUsageCharacterCount: Long?,
        val deepLUsageCharacterLimit: Long?,
        val deepLUsageRefreshing: Boolean,
        val deepLUsageErrorMessage: String?,
        val issueTranslationEnabled: Boolean,
        val issueTranslationAutoEnabled: Boolean,
        val issueTranslationTargetLang: String
    ) : SettingsUiState
}
