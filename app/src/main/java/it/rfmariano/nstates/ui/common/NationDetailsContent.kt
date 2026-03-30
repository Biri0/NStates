package it.rfmariano.nstates.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import it.rfmariano.nstates.data.model.Government
import it.rfmariano.nstates.data.model.NationData
import java.util.Locale
import kotlin.math.abs

@Composable
fun NationDetailsContent(
    nation: NationData,
    userAgent: String,
    modifier: Modifier = Modifier
) {
    val sections = nationDetailSections(nation)
    val sectionSpacing = 12.dp
    val minCardWidth = 320.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        NationHeader(nation = nation, userAgent = userAgent)
        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = (maxWidth / minCardWidth).toInt().coerceAtLeast(1)
            MasonryGrid(
                columns = columns,
                horizontalSpacing = sectionSpacing,
                verticalSpacing = sectionSpacing
            ) {
                sections.forEach { section ->
                    InfoCard(
                        title = section.title,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        section.content()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MasonryGrid(
    columns: Int,
    horizontalSpacing: androidx.compose.ui.unit.Dp,
    verticalSpacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = content
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.minWidth, 0) {}
        }

        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()
        val safeColumns = columns.coerceAtLeast(1)
        val totalSpacing = hSpacingPx * (safeColumns - 1)
        val columnWidth = ((constraints.maxWidth - totalSpacing) / safeColumns).coerceAtLeast(0)
        val itemConstraints = constraints.copy(
            minWidth = columnWidth,
            maxWidth = columnWidth
        )

        val columnHeights = IntArray(safeColumns)
        val placeables = ArrayList<androidx.compose.ui.layout.Placeable>(measurables.size)
        val xPositions = IntArray(measurables.size)
        val yPositions = IntArray(measurables.size)

        measurables.forEachIndexed { index, measurable ->
            val placeable = measurable.measure(itemConstraints)
            val targetColumn = columnHeights.indices.minByOrNull { columnHeights[it] } ?: 0
            val x = targetColumn * (columnWidth + hSpacingPx)
            val y = columnHeights[targetColumn]

            placeables.add(placeable)
            xPositions[index] = x
            yPositions[index] = y

            columnHeights[targetColumn] = y + placeable.height + vSpacingPx
        }

        val height = (columnHeights.maxOrNull() ?: 0).let { h ->
            if (h == 0) 0 else h - vSpacingPx
        }.coerceAtLeast(0)

        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(xPositions[index], yPositions[index])
            }
        }
    }
}

private data class NationDetailSection(
    val title: String,
    val content: @Composable () -> Unit
)

private fun nationDetailSections(nation: NationData): List<NationDetailSection> = listOf(
    NationDetailSection(title = "Overview") {
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
    },
    NationDetailSection(title = "Freedoms") {
        InfoRow(label = "Civil Rights", value = nation.freedom.civilRights)
        InfoRow(label = "Economy", value = nation.freedom.economy)
        InfoRow(label = "Political Freedom", value = nation.freedom.politicalFreedom)
    },
    NationDetailSection(title = "Economy") {
        InfoRow(label = "GDP", value = formatCurrency(nation.gdp))
        InfoRow(label = "Income", value = formatCurrency(nation.income))
        InfoRow(label = "Poorest", value = formatCurrency(nation.poorest))
        InfoRow(label = "Richest", value = formatCurrency(nation.richest))
        InfoRow(label = "Tax Rate", value = "${nation.tax}%")
        InfoRow(label = "Currency", value = nation.currency)
        InfoRow(label = "Major Industry", value = nation.majorIndustry)
    },
    NationDetailSection(title = "Leading Causes of Death") {
        PieChartSection(
            slices = nation.deaths.causes.map { cause ->
                PieSliceData(
                    label = cause.type,
                    value = cause.percentage
                )
            },
            emptyMessage = "No death causes available"
        )
    },
    NationDetailSection(title = "Economy Breakdown") {
        val sectors = nation.sectors
        PieChartSection(
            slices = listOf(
                PieSliceData("Government", sectors.government),
                PieSliceData("Industry", sectors.industry),
                PieSliceData("Public Sector", sectors.publicSector),
                PieSliceData("Black Market", sectors.blackMarket)
            ),
            emptyMessage = "No economy sector data available"
        )
    },
    NationDetailSection(title = "Government Spending") {
        val govt = nation.government
        PieChartSection(
            slices = governmentSpendingSlices(govt),
            emptyMessage = "No government spending data available"
        )
    },
    NationDetailSection(title = "Details") {
        if (nation.leader.isNotBlank()) {
            InfoRow(label = "Leader", value = nation.leader)
        }
        if (nation.capital.isNotBlank()) {
            InfoRow(label = "Capital", value = nation.capital)
        }
        InfoRow(label = "Animal", value = nation.animal)
        InfoRow(label = "Sensibilities", value = nation.sensibilities)
        InfoRow(label = "Crime", value = nation.crime)
    },
    NationDetailSection(title = "Policies") {
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
)

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

private fun governmentSpendingSlices(govt: Government): List<PieSliceData> = listOf(
    PieSliceData("Administration", govt.administration),
    PieSliceData("Defence", govt.defence),
    PieSliceData("Education", govt.education),
    PieSliceData("Environment", govt.environment),
    PieSliceData("Healthcare", govt.healthcare),
    PieSliceData("Commerce", govt.commerce),
    PieSliceData("International Aid", govt.internationalAid),
    PieSliceData("Law & Order", govt.lawAndOrder),
    PieSliceData("Public Transport", govt.publicTransport),
    PieSliceData("Social Equality", govt.socialEquality),
    PieSliceData("Spirituality", govt.spirituality),
    PieSliceData("Welfare", govt.welfare)
)

private data class PieSliceData(
    val label: String,
    val value: Double
)

@Composable
private fun PieChartSection(
    slices: List<PieSliceData>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    val positiveSlices = slices
        .filter { it.value > 0.0 }
        .sortedByDescending { it.value }
    if (positiveSlices.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val colors = chartColors()
    val total = positiveSlices.sumOf { it.value }
    val normalizedSlices = if (total > 0.0) {
        positiveSlices.map { it.copy(value = (it.value / total) * 100.0) }
    } else {
        positiveSlices
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PieChart(
            slices = normalizedSlices,
            colors = colors,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(12.dp))
        normalizedSlices.forEachIndexed { index, slice ->
            PieLegendRow(
                color = colors[index % colors.size],
                label = slice.label,
                percentage = formatPercent(slice.value)
            )
        }

        val totalDelta = abs(normalizedSlices.sumOf { it.value } - 100.0)
        if (totalDelta > 0.01) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: ${formatPercent(normalizedSlices.sumOf { it.value })}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PieChart(
    slices: List<PieSliceData>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value }.toFloat().coerceAtLeast(0.0001f)
    val ringColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.size(180.dp)) {
        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweepAngle = ((slice.value.toFloat() / total) * 360f).coerceAtLeast(0f)
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }

        drawCircle(
            color = ringColor,
            radius = size.minDimension * 0.28f
        )
    }
}

@Composable
private fun PieLegendRow(
    color: Color,
    label: String,
    percentage: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            content = {}
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = percentage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun chartColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.error,
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF009688),
    Color(0xFF3F51B5),
    Color(0xFFFFC107),
    Color(0xFF795548),
    Color(0xFF607D8B)
)

private fun formatPercent(value: Double): String = String.format(Locale.getDefault(), "%.2f%%", value)
