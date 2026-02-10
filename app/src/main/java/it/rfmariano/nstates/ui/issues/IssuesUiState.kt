package it.rfmariano.nstates.ui.issues

import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueResult

/**
 * UI state for the issues list screen.
 */
sealed interface IssuesUiState {
    data object Loading : IssuesUiState
    data class Success(
        val issues: List<Issue>,
        val nextIssueTime: Long
    ) : IssuesUiState
    data class Error(val message: String) : IssuesUiState
}

/**
 * State for the issue action (answering/dismissing) flow.
 */
sealed interface IssueActionState {
    data object Idle : IssueActionState
    data class Confirming(
        val issue: Issue,
        val optionId: Int,
        val optionText: String
    ) : IssueActionState
    data object Submitting : IssueActionState
    data class ResultReady(val result: IssueResult) : IssueActionState
    data class ActionError(val message: String) : IssueActionState
}
