package it.rfmariano.nstates.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.rfmariano.nstates.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onAddNation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var lastNationName by remember { mutableStateOf<String?>(null) }
    var pendingNotificationEnable by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setIssueNotificationsEnabled(true)
        } else {
            if (pendingNotificationEnable) {
                viewModel.setIssueNotificationsEnabled(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Notification permission denied")
                }
            }
        }
        pendingNotificationEnable = false
    }

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is SettingsUiState.Ready -> {
                if (lastNationName == null) {
                    lastNationName = state.nationName
                } else if (!lastNationName.equals(state.nationName, ignoreCase = true)) {
                    Toast
                        .makeText(context, "Switched to ${state.nationName}", Toast.LENGTH_SHORT)
                        .show()
                    lastNationName = state.nationName
                }
                SettingsContent(
                    nationName = state.nationName,
                    accounts = state.accounts,
                    initialPage = state.initialPage,
                    issueNotificationsEnabled = state.issueNotificationsEnabled,
                    onInitialPageChange = { viewModel.setInitialPage(it) },
                    onNotificationsToggle = { enabled ->
                        if (enabled) {
                            if (hasNotificationPermission) {
                                viewModel.setIssueNotificationsEnabled(true)
                            } else if (!pendingNotificationEnable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingNotificationEnable = true
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    // Should be covered by hasNotificationPermission=true for < 33,
                                    // but just in case logic falls through here.
                                    viewModel.setIssueNotificationsEnabled(true)
                                }
                            }
                        } else {
                            viewModel.setIssueNotificationsEnabled(false)
                        }
                    },
                    onAccountSelected = {
                        viewModel.switchAccount(it)
                    },
                    onAddNation = onAddNation,
                    onRemoveAccount = { accountName ->
                        val remaining = viewModel.removeAccount(accountName)
                        if (remaining == 0) {
                            onLogout()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Removed $accountName")
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }


}

/**
 * Maps route constants to user-friendly display labels.
 */
private val initialPageOptions = listOf(
    Routes.NATION to "Nation",
    Routes.ISSUES to "Issues"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    nationName: String,
    accounts: List<String>,
    initialPage: String,
    issueNotificationsEnabled: Boolean,
    onInitialPageChange: (String) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onAccountSelected: (String) -> Unit,
    onAddNation: () -> Unit,
    onRemoveAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Account card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Logged in as",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                var accountExpanded by remember { mutableStateOf(false) }
                var showRemoveConfirm by remember { mutableStateOf(false) }
                val selectedAccount = accounts.firstOrNull { it.equals(nationName, ignoreCase = true) }
                    ?: nationName
                val canRemoveAccount = accounts.size > 1 && selectedAccount.isNotBlank()
                val accountLabel = selectedAccount.ifBlank { "Select account" }

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.sortedBy { it.lowercase() }.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account) },
                                onClick = {
                                    accountExpanded = false
                                    onAccountSelected(account)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onAddNation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add nation")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canRemoveAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove account")
                }

                if (showRemoveConfirm && canRemoveAccount) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRemoveConfirm = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRemoveConfirm = false
                                    onRemoveAccount(selectedAccount)
                                }
                            ) {
                                Text("Remove")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showRemoveConfirm = false }
                            ) {
                                Text("Cancel")
                            }
                        },
                        title = { Text("Remove account") },
                        text = { Text("Remove $selectedAccount from this device?") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notifications card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alerts when new issues are available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Issue notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = issueNotificationsEnabled,
                        onCheckedChange = onNotificationsToggle
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Initial page card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Initial Page",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The page shown after logging in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = initialPageOptions
                    .firstOrNull { it.first == initialPage }?.second ?: "Nation"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        initialPageOptions.forEach { (route, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onInitialPageChange(route)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // About card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NStates",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "An Android client for NationStates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
