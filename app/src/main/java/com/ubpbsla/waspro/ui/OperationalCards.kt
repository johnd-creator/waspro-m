@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ubpbsla.waspro.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubpbsla.waspro.data.obj
import com.ubpbsla.waspro.data.text
import kotlinx.serialization.json.*

@Composable
internal fun RecordCard(title: String, summary: String, amount: String, statusKey: String, status: String, icon: ImageVector = Icons.Default.Inventory2, open: () -> Unit) {
    Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Icon(icon, null, Modifier.padding(9.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (amount.isNotBlank() || status.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    if (amount.isNotBlank()) Text(amount, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
                    if (status.isNotBlank()) {
                        StatusBadge(statusKey, status)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(statusKey: String, status: String) {
    val colors = MaterialTheme.colorScheme
    val (fill, ink) = when (statusKey) {
        "approved" -> colors.primaryContainer to colors.onPrimaryContainer
        "pending_review", "pending_approval", "submitted" -> colors.tertiaryContainer to colors.onTertiaryContainer
        "rejected" -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceContainer to colors.onSurfaceVariant
    }
    Surface(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, shape = MaterialTheme.shapes.small, color = fill, contentColor = ink) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (statusKey) {
                "approved" -> Icons.Default.CheckCircle
                "pending_review", "pending_approval", "submitted" -> Icons.Default.Schedule
                "rejected" -> Icons.Default.Cancel
                else -> Icons.Default.Description
            }
            Icon(icon, null, Modifier.size(14.dp))
            Text(status, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun DashboardSummary(element: JsonElement) {
    val root = element as? JsonObject ?: run { FormPanel { JsonDetails(element) }; return }
    val waste = root.obj("waste")
    val faba = root.obj("faba")
    val period = root.obj("resolved_period")
    val periodText = listOf(period.text("month"), period.text("year")).filter(String::isNotBlank).joinToString("/")
    val metrics = buildList<Pair<String, String>> {
        fun addMetric(data: JsonObject, key: String, title: String, unit: String = "") {
            val value = data[key] as? JsonPrimitive ?: return
            formatMetric(value.content)?.let { add(title to (it + unit)) }
        }
        addMetric(waste, "total_records", "Catatan limbah")
        addMetric(waste, "pending_records", "Menunggu persetujuan")
        addMetric(faba, "current_balance", "Saldo FABA", " ton")
        addMetric(root, "current_balance", "Saldo FABA terkini", " ton")
        addMetric(root.obj("recap"), "total_production", "Produksi periode ini", " ton")
        addMetric(root.obj("recap"), "total_utilization", "Pemanfaatan periode ini", " ton")
    }
    if (periodText.isNotBlank()) SectionHeading("Periode $periodText")
    if (metrics.isNotEmpty()) {
        val lead = metrics.first()
        MetricHero(lead.first, lead.second, if (lead.first.contains("FABA")) Icons.Default.Layers else Icons.Default.Inventory2)
        BoxWithConstraints {
            val columns = ((maxWidth.value + 10f) / (154f * LocalDensity.current.fontScale + 10f)).toInt().coerceIn(1, minOf(3, (metrics.size - 1).coerceAtLeast(1)))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metrics.drop(1).chunked(columns).forEach { group ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.forEach { (title, value) ->
                            Surface(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(if (title == "Menunggu persetujuan") Icons.Default.Schedule else Icons.Default.Layers,
                                        null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    QuantityLabel(value, prominent = false)
                                }
                            }
                        }
                        repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    root.forEach { (key, value) ->
        if (value is JsonObject || value is JsonArray) {
            var expanded by rememberSaveable(key) { mutableStateOf(false) }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().animateContentSize(tween(180))) {
                    TextButton(onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                            stateDescription = if (expanded) "Terbuka" else "Tertutup"
                        }) {
                        Text(label(key), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expanded) "Tutup rincian" else "Lihat rincian")
                    }
                    if (expanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.padding(16.dp)) { JsonDetails(value) }
                    }
                }
            }
        }
    }
    val scalars = root.filterValues { it is JsonPrimitive }.filterKeys { it != "current_balance" || metrics.isEmpty() }
    if (scalars.isNotEmpty()) FormPanel { JsonDetails(JsonObject(scalars)) }
}

@Composable
internal fun TransactionSummary(record: JsonObject, fallback: String) {
    val material = record.obj("waste_type").text("name").ifBlank {
        statuses[record.text("material_type")] ?: fallback
    }
    val number = listOf("record_number", "hauling_number", "display_number", "document_number")
        .firstNotNullOfOrNull { record.text(it).takeIf(String::isNotBlank) }
    val statusKey = record.text("approval_status").ifBlank { record.text("status") }
    val status = statuses[statusKey] ?: record.text("status_label").ifBlank { statusKey }
    FormPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Default.Inventory2)
            Text(material, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
        }
        val quantity = record.text("quantity").toBigDecimalOrNull()
        if (quantity != null) {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID")).format(quantity)
            QuantityLabel(listOf(formatted, record.text("unit")).filter(String::isNotBlank).joinToString(" "),
                prominent = true, unit = record.text("unit"))
        }
        if (status.isNotBlank()) AnimatedContent(status, transitionSpec = {
            fadeIn(tween(180)) togetherWith fadeOut(tween(160))
        }, label = "Status") { StatusBadge(statusKey, it) }
        val context = listOf(record.text("date").ifBlank { record.text("transaction_date") },
            record.text("source"), record.obj("internal_destination").text("name")).filter(String::isNotBlank)
        if (context.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(context.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val requester = listOf("submitted_by", "submitted_by_user", "submitter", "created_by", "created_by_user", "creator", "user").firstNotNullOfOrNull {
            record.obj(it).text("name").takeIf(String::isNotBlank)
        }
        if (requester != null) Text("Pengaju / pembuat · $requester", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (number != null) SelectionContainer { Text(number, Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Keep the full quantity in one text node for selection and assistive technology. */
@Composable
private fun QuantityLabel(value: String, prominent: Boolean, unit: String = if (value.endsWith(" ton")) "ton" else "") {
    val amountStyle = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
    Text(buildAnnotatedString {
        if (unit.isNotBlank() && value.endsWith(" $unit")) {
            append(value.removeSuffix(" $unit"))
            withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                append(" $unit")
            }
        } else append(value)
    }, style = amountStyle.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
}
