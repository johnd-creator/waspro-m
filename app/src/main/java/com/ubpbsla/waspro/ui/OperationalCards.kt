package com.ubpbsla.waspro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubpbsla.waspro.data.obj
import com.ubpbsla.waspro.data.text
import kotlinx.serialization.json.*

@Composable
internal fun RecordCard(title: String, summary: String, amount: String, statusKey: String, status: String, open: () -> Unit) {
    Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (amount.isNotBlank() || status.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (amount.isNotBlank()) Text(amount, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (status.isNotBlank()) {
                        val colors = MaterialTheme.colorScheme
                        val (fill, ink) = when (statusKey) {
                            "approved" -> colors.primaryContainer to colors.onPrimaryContainer
                            "pending_approval", "submitted" -> Color(0xFFFFEDBC) to Color(0xFF694600)
                            "rejected" -> colors.errorContainer to colors.onErrorContainer
                            else -> colors.surfaceContainer to colors.onSurfaceVariant
                        }
                        Surface(shape = MaterialTheme.shapes.small, color = fill, contentColor = ink) {
                            Text(status, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
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
            if (value.content.toBigDecimalOrNull() != null) add(title to (displayValue(key, value) + unit))
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
        BoxWithConstraints {
            val columns = (maxWidth.value / (110f * LocalDensity.current.fontScale)).toInt().coerceIn(1, 3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.chunked(columns).forEach { group ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { (title, value) ->
                            Surface(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(title, style = MaterialTheme.typography.labelMedium)
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
            FormPanel {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(label(key), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Tutup rincian" else "Lihat rincian")
                }
                if (expanded) JsonDetails(value)
            }
        }
    }
    val scalars = root.filterValues { it is JsonPrimitive }.filterKeys { it != "current_balance" || metrics.isEmpty() }
    if (scalars.isNotEmpty()) FormPanel { JsonDetails(JsonObject(scalars)) }
}
