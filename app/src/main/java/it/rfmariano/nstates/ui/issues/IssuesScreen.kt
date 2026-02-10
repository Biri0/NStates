package it.rfmariano.nstates.ui.issues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import it.rfmariano.nstates.data.api.CensusScales
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueOption
import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.RankingChange
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(
    modifier: Modifier = Modifier,
    viewModel: IssuesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val selectedIssue by viewModel.selectedIssue.collectAsStateWithLifecycle()

    // Confirmation dialog
    when (val action = actionState) {
        is IssueActionState.Confirming -> {
            ConfirmAnswerDialog(
                optionText = action.optionText,
                isDismiss = action.optionId == -1,
                onConfirm = viewModel::confirmAnswer,
                onCancel = viewModel::cancelAction
            )
        }
        is IssueActionState.Submitting -> {
            SubmittingDialog()
        }
        is IssueActionState.ResultReady -> {
            ResultBottomSheet(
                result = action.result,
                onDismiss = viewModel::dismissResult
            )
        }
        is IssueActionState.ActionError -> {
            ErrorDialog(
                message = action.message,
                onDismiss = viewModel::dismissResult
            )
        }
        is IssueActionState.Idle -> { /* no dialog */ }
    }

    val currentIssue = selectedIssue
    if (currentIssue != null) {
        IssueDetailContent(
            issue = currentIssue,
            onBack = viewModel::clearSelectedIssue,
            onSelectOption = { optionId ->
                viewModel.requestAnswer(currentIssue, optionId)
            },
            onDismissIssue = {
                viewModel.requestAnswer(currentIssue, -1)
            },
            modifier = modifier
        )
    } else {
        IssuesListContent(
            uiState = uiState,
            onRetry = viewModel::loadIssues,
            onRefresh = viewModel::loadIssues,
            onIssueClick = viewModel::selectIssue,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssuesListContent(
    uiState: IssuesUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onIssueClick: (Issue) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Issues") },
                windowInsets = WindowInsets(0),
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is IssuesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is IssuesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
            is IssuesUiState.Success -> {
                if (uiState.issues.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No issues available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.nextIssueTime > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                NextIssueTimeText(nextIssueTime = uiState.nextIssueTime)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefresh) {
                                Text("Refresh")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.nextIssueTime > 0) {
                            item {
                                NextIssueTimeText(nextIssueTime = uiState.nextIssueTime)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        items(
                            items = uiState.issues,
                            key = { it.id }
                        ) { issue ->
                            IssueCard(
                                issue = issue,
                                onClick = { onIssueClick(issue) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextIssueTimeText(
    nextIssueTime: Long,
    modifier: Modifier = Modifier
) {
    val currentTime = System.currentTimeMillis() / 1000
    val remainingSeconds = nextIssueTime - currentTime

    val timeText = if (remainingSeconds <= 0) {
        "New issues should be available now"
    } else {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        when {
            hours > 0 -> "Next issue in ${hours}h ${minutes}m"
            minutes > 0 -> "Next issue in ${minutes}m"
            else -> "Next issue in less than a minute"
        }
    }

    Text(
        text = timeText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontStyle = FontStyle.Italic,
        modifier = modifier
    )
}

@Composable
private fun IssueCard(
    issue: Issue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "#${issue.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = issue.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${issue.options.size} options",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueDetailContent(
    issue: Issue,
    onBack: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onDismissIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Issue #${issue.id}") },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDismissIssue) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Dismiss issue"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Issue banner image
            if (issue.pic1.isNotBlank()) {
                val imageUrl = "https://www.nationstates.net/images/dilemmas/${issue.pic1}.jpg"
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = issue.title,
                    contentScale = ContentScale.Crop,
                    success = {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                )
            }

            // Title
            Text(
                text = issue.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description text
            Text(
                text = issue.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (issue.author.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Author: ${issue.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Options header
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option cards
            issue.options.forEach { option ->
                OptionCard(
                    option = option,
                    onClick = { onSelectOption(option.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss button
            OutlinedButton(
                onClick = onDismissIssue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Dismiss Issue")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionCard(
    option: IssueOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${option.id + 1}.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConfirmAnswerDialog(
    optionText: String,
    isDismiss: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(if (isDismiss) "Dismiss Issue?" else "Confirm Answer")
        },
        text = {
            Column {
                if (isDismiss) {
                    Text("Are you sure you want to dismiss this issue? This action cannot be undone.")
                } else {
                    Text("Are you sure you want to select this option?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (isDismiss) "Dismiss" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SubmittingDialog() {
    AlertDialog(
        onDismissRequest = { /* non-dismissible while submitting */ },
        title = { Text("Submitting...") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = { /* no buttons while submitting */ }
    )
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("Error", message))
                        )
                    }
                }
            ) {
                Text("Copy")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultBottomSheet(
    result: IssueResult,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ResultContent(
            result = result,
            onDismiss = onDismiss,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ResultContent(
    result: IssueResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Legislation Result",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        if (result.description.isNotBlank()) {
            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error
        if (result.error.isNotBlank()) {
            Text(
                text = result.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Reclassifications (non-freedom ones only, e.g. nation category)
        val otherReclassifications = result.reclassifications
            .filter { it.type !in FREEDOM_RECLASS_TYPES }
        if (otherReclassifications.isNotEmpty()) {
            Text(
                text = "Reclassifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            otherReclassifications.forEach { reclass ->
                Text(
                    text = "${reclass.type}: ${reclass.from} -> ${reclass.to}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Freedom changes (Civil Rights, Economy, Political Freedom)
        val freedomChanges = result.rankings
            .filter { it.id in CensusScales.FREEDOM_IDS }
            .sortedByDescending { it.percentageChange }
        val freedomReclassifications = result.reclassifications
            .filter { it.type in FREEDOM_RECLASS_TYPES }
            .associateBy { it.type }
        if (freedomChanges.isNotEmpty()) {
            Text(
                text = "Freedom Changes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            freedomChanges.forEach { ranking ->
                RankingChangeRow(ranking = ranking)
                val reclass = freedomReclassifications[ranking.id.toString()]
                if (reclass != null) {
                    Text(
                        text = "${reclass.from} -> ${reclass.to}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Census changes (everything else, filtered and sorted)
        val censusChanges = result.rankings
            .filter { it.id !in CensusScales.FREEDOM_IDS && abs(it.percentageChange) >= 1.0 }
            .sortedByDescending { it.percentageChange }
        if (censusChanges.isNotEmpty()) {
            Text(
                text = "Census Changes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            censusChanges.forEach { ranking ->
                RankingChangeRow(ranking = ranking)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // New policies
        if (result.newPolicies.isNotEmpty()) {
            Text(
                text = "New Policies",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.newPolicies.forEach { policy ->
                Text(
                    text = "+ $policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Removed policies
        if (result.removedPolicies.isNotEmpty()) {
            Text(
                text = "Removed Policies",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.removedPolicies.forEach { policy ->
                Text(
                    text = "- $policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unlocks
        if (result.unlocks.isNotEmpty()) {
            Text(
                text = "Unlocked",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.unlocks.forEach { unlock ->
                Text(
                    text = unlock,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dismiss button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun RankingChangeRow(
    ranking: RankingChange,
    modifier: Modifier = Modifier
) {
    val changePrefix = if (ranking.percentageChange >= 0) "+" else ""
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ranking.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$changePrefix${String.format(Locale.getDefault(), "%.2f", ranking.percentageChange)}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (ranking.percentageChange >= 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

/** Reclassification type values that correspond to freedom dimensions. */
private val FREEDOM_RECLASS_TYPES = setOf("0", "1", "2")
