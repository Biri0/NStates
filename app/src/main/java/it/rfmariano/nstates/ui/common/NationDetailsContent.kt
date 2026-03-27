package it.rfmariano.nstates.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import it.rfmariano.nstates.data.model.NationData
import java.util.Locale

@Composable
fun NationDetailsContent(
    nation: NationData,
    userAgent: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        NationHeader(nation = nation, userAgent = userAgent)

        Spacer(modifier = Modifier.height(16.dp))

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

        InfoCard(title = "Freedoms") {
            InfoRow(label = "Civil Rights", value = nation.freedom.civilRights)
            InfoRow(label = "Economy", value = nation.freedom.economy)
            InfoRow(label = "Political Freedom", value = nation.freedom.politicalFreedom)
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard(title = "Policies") {
            if (nation.policies.isEmpty()) {
                Text(
                    text = "No active policies",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                nation.policies.forEach { policy ->
                    Text(
                        text = "• $policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NationHeader(
    nation: NationData,
    userAgent: String,
    modifier: Modifier = Modifier
) {
    var bannerLoadFailed by remember(nation.bannerCode, nation.flagUrl) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bannerUrl = nationBannerUrl(nation.bannerCode)
        val flagUrl = nationFlagUrl(nation.flagUrl)
        val shouldShowBanner = bannerUrl != null && !bannerLoadFailed
        val bannerHeight = 74.dp

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (shouldShowBanner) {
                    val context = LocalContext.current
                    val imageRequest = ImageRequest.Builder(context)
                        .data(bannerUrl)
                        .httpHeaders(NetworkHeaders.Builder().set("User-Agent", userAgent).build())
                        .build()

                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "${nation.name} banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight),
                        error = {
                            bannerLoadFailed = true
                        },
                        success = {
                            SubcomposeAsyncImageContent(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(bannerHeight)
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (flagUrl != null) {
                        val context = LocalContext.current
                        val imageRequest = ImageRequest.Builder(context)
                            .data(flagUrl)
                            .httpHeaders(NetworkHeaders.Builder().set("User-Agent", userAgent).build())
                            .build()

                        SubcomposeAsyncImage(
                            model = imageRequest,
                            contentDescription = "${nation.name} flag",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .widthIn(max = 190.dp),
                            success = {
                                SubcomposeAsyncImageContent(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f)
                                        .heightIn(max = 96.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = nation.fullName.ifBlank { nation.name },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (nation.type.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nation.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (nation.motto.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${nation.motto}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun nationBannerUrl(rawBannerCode: String): String? {
    val value = rawBannerCode.trim()
    if (value.isBlank()) return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value

    val normalized = value
        .removePrefix("/images/banners/")
        .removeSuffix(".jpg")
        .trim()
    if (normalized.isBlank()) return null

    return "https://www.nationstates.net/images/banners/$normalized.jpg"
}

private fun nationFlagUrl(rawFlagUrl: String): String? {
    val value = rawFlagUrl.trim()
    if (value.isBlank()) return null
    return if (value.startsWith("http://") || value.startsWith("https://")) {
        value
    } else {
        "https://www.nationstates.net$value"
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
