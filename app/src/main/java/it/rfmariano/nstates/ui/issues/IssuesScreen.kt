package it.rfmariano.nstates.ui.issues

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import it.rfmariano.nstates.data.api.CensusScales
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueChatRole
import it.rfmariano.nstates.data.model.IssueOption
import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.PolicyDetails
import it.rfmariano.nstates.data.model.RankingChange
import it.rfmariano.nstates.ui.common.NStatesCenteredLoading
import it.rfmariano.nstates.ui.common.NStatesInlineLoading
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(
    modifier: Modifier = Modifier,
    viewModel: IssuesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val selectedIssue by viewModel.selectedIssue.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val issueTranslationState by viewModel.issueTranslationState.collectAsStateWithLifecycle()
    val userAgent by viewModel.userAgent.collectAsStateWithLifecycle()

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
                copyPayload = action.copyPayload,
                onDismiss = viewModel::dismissResult
            )
        }
        is IssueActionState.Idle -> { /* no dialog */ }
    }

    val currentIssue = selectedIssue
    if (currentIssue != null) {
        BackHandler(onBack = viewModel::clearSelectedIssue)
        IssueDetailContent(
            issue = currentIssue,
            userAgent = userAgent,
            onBack = viewModel::clearSelectedIssue,
            onSelectOption = { optionId ->
                viewModel.requestAnswer(currentIssue, optionId)
            },
            onDismissIssue = {
                viewModel.requestAnswer(currentIssue, -1)
            },
            chatState = chatState,
            issueTranslationState = issueTranslationState,
            onToggleIssueTranslation = viewModel::setIssueTranslationEnabledForSelectedIssue,
            onSendChatMessage = viewModel::sendChatMessage,
            onClearConversation = viewModel::clearChatConversation,
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
                NStatesCenteredLoading(
                    modifier = Modifier
                        .padding(innerPadding)
                )
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
                text = italicizeHtmlItalics(issue.text),
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
    userAgent: String,
    onBack: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onDismissIssue: () -> Unit,
    chatState: IssueChatUiState,
    issueTranslationState: IssueTranslationUiState,
    onToggleIssueTranslation: (Boolean) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chatInput by rememberSaveable(issue.id) { mutableStateOf("") }

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
                val imageUrl = "https://www.nationstates.net/images/banners/${issue.pic1}.jpg"
                val context = LocalContext.current
                val imageRequest = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .httpHeaders(NetworkHeaders.Builder().set("User-Agent", userAgent).build())
                    .build()

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = issue.title,
                    contentScale = ContentScale.FillWidth,
                    success = {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = issue.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (issueTranslationState.isToggleVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show translated text",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Target language: ${issueTranslationState.targetLanguageCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = issueTranslationState.isTranslated,
                        onCheckedChange = onToggleIssueTranslation,
                        enabled = !issueTranslationState.isTranslating
                    )
                }
                if (issueTranslationState.isTranslating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    NStatesInlineLoading(size = 20.dp)
                }
                issueTranslationState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description text
            Text(
                text = italicizeHtmlItalics(issue.text),
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

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Issue AI Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClearConversation,
                    enabled = !chatState.isSending
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear chat conversation"
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (!chatState.isApiKeyConfigured) {
                Text(
                    text = "Add your OpenRouter API key in Settings to use this chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (chatState.messages.isEmpty() && chatState.streamingMessage.isBlank()) {
                    Text(
                        text = "Ask anything about this issue and its options.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    chatState.messages.forEach { message ->
                        val isUser = message.role == IssueChatRole.USER
                        ChatMessageBubble(
                            roleLabel = if (isUser) "You" else "AI",
                            isUser = isUser,
                            content = if (isUser) {
                                AnnotatedString(message.content)
                            } else {
                                boldMarkdownToAnnotatedString(message.content)
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (chatState.streamingMessage.isNotBlank()) {
                        ChatMessageBubble(
                            roleLabel = "AI",
                            isUser = false,
                            content = boldMarkdownToAnnotatedString(chatState.streamingMessage),
                            footer = "Writing..."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (chatState.isSending) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NStatesInlineLoading(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Requesting... attempt ${chatState.attempt}/${chatState.maxAttempts}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                chatState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val canSend = chatInput.isNotBlank() && !chatState.isSending
                fun sendCurrentMessage() {
                    if (!canSend) return
                    val text = chatInput
                    chatInput = ""
                    onSendChatMessage(text)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        label = { Text("Message") },
                        modifier = Modifier.weight(1f),
                        enabled = !chatState.isSending,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { sendCurrentMessage() }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendCurrentMessage() },
                        enabled = canSend
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
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
                text = italicizeHtmlItalics(option.text),
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
                        text = italicizeHtmlItalics(optionText),
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
private fun ChatMessageBubble(
    roleLabel: String,
    isUser: Boolean,
    content: AnnotatedString,
    footer: String? = null
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("Chat message", content.text))
                            )
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                footer?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.offset(x = 1.dp)
                    )
                }
            }
        }
    }
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
                NStatesInlineLoading(size = 28.dp)
            }
        },
        confirmButton = { /* no buttons while submitting */ }
    )
}

@Composable
private fun ErrorDialog(
    message: String,
    copyPayload: String?,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val textToCopy = copyPayload ?: message
    val copyLabel = if (copyPayload != null) "Copy response" else "Copy"

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
                            ClipEntry(ClipData.newPlainText("Error", textToCopy))
                        )
                    }
                }
            ) {
                Text(copyLabel)
            }
        }
    )
}

private fun italicizeHtmlItalics(text: String): AnnotatedString {
    if (!text.contains("<i>", ignoreCase = true)) return AnnotatedString(text)

    val regex = Regex("<i>(.*?)</i>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    return buildAnnotatedString {
        var lastIndex = 0
        regex.findAll(text).forEach { match ->
            val start = match.range.first
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.groupValues[1])
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

private fun boldMarkdownToAnnotatedString(text: String): AnnotatedString {
    if (!text.contains("**")) return AnnotatedString(text)

    val regex = Regex("\\*\\*(.*?)\\*\\*", setOf(RegexOption.DOT_MATCHES_ALL))
    return buildAnnotatedString {
        var lastIndex = 0
        regex.findAll(text).forEach { match ->
            val start = match.range.first
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
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
                text = italicizeHtmlItalics(result.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error
        if (result.error.isNotBlank()) {
            Text(
                text = italicizeHtmlItalics(result.error),
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
        if (result.newPolicyDetails.isNotEmpty() || result.newPolicies.isNotEmpty()) {
            Text(
                text = "New Policies",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (result.newPolicyDetails.isNotEmpty()) {
                result.newPolicyDetails.forEach { policy ->
                    PolicyDetailsBlock(
                        policy = policy,
                        prefix = "+",
                        titleColor = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                result.newPolicies.forEach { policy ->
                    Text(
                        text = "+ $policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Removed policies
        if (result.removedPolicyDetails.isNotEmpty() || result.removedPolicies.isNotEmpty()) {
            Text(
                text = "Removed Policies",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (result.removedPolicyDetails.isNotEmpty()) {
                result.removedPolicyDetails.forEach { policy ->
                    PolicyDetailsBlock(
                        policy = policy,
                        prefix = "-",
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                result.removedPolicies.forEach { policy ->
                    Text(
                        text = "- $policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unlocked banners
        if (result.unlockedBanners.isNotEmpty()) {
            Text(
                text = "Unlocked Banners",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.unlockedBanners.forEach { banner ->
                Text(
                    text = banner.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (banner.validity.isNotBlank()) {
                    Text(
                        text = banner.validity,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Headlines
        if (result.headlines.isNotEmpty()) {
            Text(
                text = "Headlines",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            result.headlines.forEach { headline ->
                Text(
                    text = "\u2022 $headline",
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
private fun PolicyDetailsBlock(
    policy: PolicyDetails,
    prefix: String,
    titleColor: androidx.compose.ui.graphics.Color
) {
    if (policy.name.isNotBlank()) {
        Text(
            text = "$prefix ${policy.name}",
            style = MaterialTheme.typography.bodySmall,
            color = titleColor,
            fontWeight = FontWeight.SemiBold
        )
    }
    if (policy.category.isNotBlank() || policy.pic.isNotBlank()) {
        val meta = listOf(policy.category, policy.pic).filter { it.isNotBlank() }.joinToString(" • ")
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (policy.description.isNotBlank()) {
        Text(
            text = italicizeHtmlItalics(policy.description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
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
