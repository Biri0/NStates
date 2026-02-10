package it.rfmariano.nstates.ui.nation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import it.rfmariano.nstates.data.model.NationData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NationScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("NStates") },
                windowInsets = WindowInsets(0),
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is NationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NationUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::loadNation) {
                            Text("Retry")
                        }
                    }
                }
            }
            is NationUiState.Success -> {
                NationContent(
                    nation = state.nation,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun NationContent(
    nation: NationData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header: Flag + Name + Motto
        NationHeader(nation = nation)

        Spacer(modifier = Modifier.height(16.dp))

        // Overview card
        InfoCard(title = "Overview") {
            InfoRow(label = "Full Name", value = nation.fullName)
            InfoRow(label = "Category", value = nation.category)
            InfoRow(label = "Region", value = nation.region)
            InfoRow(label = "Population", value = formatPopulation(nation.population))
            InfoRow(label = "WA Status", value = nation.waStatus)
            if (nation.influence.isNotBlank()) {
                InfoRow(label = "Influence", value = nation.influence)
            }
            if (nation.founded.isNotBlank()) {
                InfoRow(label = "Founded", value = nation.founded)
            }
            InfoRow(label = "Last Activity", value = nation.lastActivity)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Civil Rights / Economy / Political Freedom
        InfoCard(title = "Freedoms") {
            InfoRow(label = "Civil Rights", value = nation.freedom.civilRights)
            InfoRow(label = "Economy", value = nation.freedom.economy)
            InfoRow(label = "Political Freedom", value = nation.freedom.politicalFreedom)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Economy details
        InfoCard(title = "Economy") {
            InfoRow(label = "GDP", value = formatCurrency(nation.gdp))
            InfoRow(label = "Income", value = formatCurrency(nation.income))
            InfoRow(label = "Poorest", value = formatCurrency(nation.poorest))
            InfoRow(label = "Richest", value = formatCurrency(nation.richest))
            InfoRow(label = "Tax Rate", value = "${nation.tax}%")
            InfoRow(label = "Currency", value = nation.currency)
            InfoRow(label = "Major Industry", value = nation.majorIndustry)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Government spending
        InfoCard(title = "Government Spending") {
            val govt = nation.government
            InfoRow(label = "Administration", value = "${govt.administration}%")
            InfoRow(label = "Defence", value = "${govt.defence}%")
            InfoRow(label = "Education", value = "${govt.education}%")
            InfoRow(label = "Environment", value = "${govt.environment}%")
            InfoRow(label = "Healthcare", value = "${govt.healthcare}%")
            InfoRow(label = "Commerce", value = "${govt.commerce}%")
            InfoRow(label = "International Aid", value = "${govt.internationalAid}%")
            InfoRow(label = "Law & Order", value = "${govt.lawAndOrder}%")
            InfoRow(label = "Public Transport", value = "${govt.publicTransport}%")
            InfoRow(label = "Social Equality", value = "${govt.socialEquality}%")
            InfoRow(label = "Spirituality", value = "${govt.spirituality}%")
            InfoRow(label = "Welfare", value = "${govt.welfare}%")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Details
        InfoCard(title = "Details") {
            if (nation.leader.isNotBlank()) {
                InfoRow(label = "Leader", value = nation.leader)
            }
            if (nation.capital.isNotBlank()) {
                InfoRow(label = "Capital", value = nation.capital)
            }
            InfoRow(label = "Animal", value = nation.animal)
            InfoRow(label = "Sensibilities", value = nation.sensibilities)
            InfoRow(label = "Crime", value = nation.crime)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NationHeader(
    nation: NationData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (nation.flagUrl.isNotBlank()) {
            AsyncImage(
                model = nation.flagUrl,
                contentDescription = "${nation.name} flag",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = nation.fullName.ifBlank { nation.name },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (nation.motto.isNotBlank()) {
            Text(
                text = "\"${nation.motto}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (nation.type.isNotBlank()) {
            Text(
                text = nation.type,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f)
        )
    }
}

private fun formatPopulation(millions: Long): String {
    return when {
        millions >= 1000 -> String.format(Locale.getDefault(), "%.2f billion", millions / 1000.0)
        else -> "$millions million"
    }
}

private fun formatCurrency(amount: Long): String {
    return when {
        amount >= 1_000_000_000_000 -> String.format(Locale.getDefault(), "%.2f trillion", amount / 1_000_000_000_000.0)
        amount >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.2f billion", amount / 1_000_000_000.0)
        amount >= 1_000_000 -> String.format(Locale.getDefault(), "%.2f million", amount / 1_000_000.0)
        else -> amount.toString()
    }
}
