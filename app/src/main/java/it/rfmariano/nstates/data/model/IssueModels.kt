package it.rfmariano.nstates.data.model

/**
 * Represents an issue presented to a nation.
 *
 * Issues are fetched via the private "issues" shard and contain
 * a title, description text, a banner image, and a list of options
 * the nation can choose from (or dismiss).
 */
data class Issue(
    val id: Int,
    val title: String,
    val text: String,
    val author: String = "",
    val editor: String = "",
    val pic1: String = "",
    val pic2: String = "",
    val options: List<IssueOption> = emptyList()
)

/**
 * A single option the player can choose when answering an issue.
 * Option IDs start at 0.
 */
data class IssueOption(
    val id: Int,
    val text: String
)

/**
 * Result returned by the API after answering an issue.
 * Contains the outcome description, census ranking changes,
 * reclassifications, and policy changes.
 */
data class IssueResult(
    val ok: Boolean = false,
    val error: String = "",
    val description: String = "",
    val rankings: List<RankingChange> = emptyList(),
    val reclassifications: List<Reclassification> = emptyList(),
    val newPolicies: List<String> = emptyList(),
    val removedPolicies: List<String> = emptyList(),
    val unlocks: List<String> = emptyList()
)

/**
 * A change in a census ranking after answering an issue.
 */
data class RankingChange(
    val id: Int,
    val name: String = "",
    val scoreBefore: Double = 0.0,
    val scoreAfter: Double = 0.0,
    val percentageChange: Double = 0.0
)

/**
 * A reclassification of the nation (e.g., category change).
 */
data class Reclassification(
    val type: String = "",
    val from: String = "",
    val to: String = ""
)

/**
 * Container for the list of current issues and the next issue timestamp.
 */
data class IssuesData(
    val issues: List<Issue> = emptyList(),
    val nextIssueTime: Long = 0L
)
