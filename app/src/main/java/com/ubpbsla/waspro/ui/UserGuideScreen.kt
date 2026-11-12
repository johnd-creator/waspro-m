@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ubpbsla.waspro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ubpbsla.waspro.R
import com.ubpbsla.waspro.data.UserContext
import com.ubpbsla.waspro.data.obj
import com.ubpbsla.waspro.data.text
import com.ubpbsla.waspro.features.GuideCase
import com.ubpbsla.waspro.features.UserGuide

@Composable
internal fun GuideIndex(user: UserContext, openCase: (String) -> Unit) {
    var role by rememberSaveable(user.id) { mutableStateOf(UserGuide.initialRole(user.raw.obj("role").text("slug"), user.raw.obj("role").text("name"))) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(role, query) { UserGuide.filter(role, query) }
    val listState = rememberLazyListState()
    LaunchedEffect(role, query) { listState.scrollToItem(0) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding(), state = listState,
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("intro") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PANDUAN MOBILE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Langkah sesuai pekerjaan Anda", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Pilih role, cari kasus, lalu ikuti langkahnya. Semua panduan tersedia di perangkat; akses tindakan tetap ditentukan server.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Cari kasus atau kendala") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Hapus pencarian") } })
                    Text("Panduan berdasarkan role", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserGuide.roles.forEach { option ->
                            FilterChip(selected = role == option.key, onClick = { role = option.key }, label = { Text(option.title) },
                                leadingIcon = if (role == option.key) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null)
                        }
                    }
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(UserGuide.roles.first { it.key == role }.summary, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${results.size} kasus tersedia", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (results.isEmpty()) item("empty") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kasus tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                        Text("Coba kata lain atau tampilkan semua role.")
                        TextButton(onClick = { query = ""; role = "all" }) { Text("Reset pencarian dan role") }
                    }
                }
            }
            items(results, key = { it.id }) { item ->
                OutlinedCard(onClick = { openCase(item.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(item.group.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, null)
                        }
                        Text(item.route, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.screenshot != null) Text("Dilengkapi screenshot DEMO", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item("limits") {
                Text("Batas mobile: direktori akun/role hanya baca. Pengelolaan organisasi, perubahan izin, rencana angkut dan alat web lain yang belum tersedia dilakukan di web. Nama role tidak menjamin semua menu muncul.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun GuideDetail(id: String) {
    val guide = UserGuide.find(id)
    if (guide == null) {
        Text("Panduan tidak ditemukan. Kembali ke daftar kasus.", Modifier.padding(16.dp))
        return
    }
    var screenshotOpen by rememberSaveable(id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item("heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(guide.group.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(guide.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(guide.route, style = MaterialTheme.typography.bodyMedium)
                    Text("Sebelum mulai", Modifier.padding(top = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                    Text(guide.prerequisite, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(guide.steps.size, key = { "step-$it" }) { index ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(guide.steps[index], style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            guide.screenshot?.let { name -> item("screenshot") {
                OutlinedCard(onClick = { screenshotOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contoh tampilan mobile", style = MaterialTheme.typography.titleMedium)
                        Image(painterResource(guideImage(name)), guideImageDescription(name), Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit)
                        Text("Buka screenshot", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Data sintetis DEMO dari emulator development. Bukan akun atau transaksi produksi. Tampilan/aksi dapat berbeda sesuai izin.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } }
            item("result") { GuideNote("Hasil yang diharapkan", guide.result) }
            item("caution") { GuideNote("Perlu diperhatikan", guide.caution) }
            item("permissions") {
                Text("Panduan tidak menjalankan transaksi. Selalu periksa status terbaru dan dialog konfirmasi pada layar operasional.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (screenshotOpen && guide.screenshot != null) GuideScreenshot(guide) { screenshotOpen = false }
}

@Composable
private fun GuideNote(title: String, body: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GuideScreenshot(guide: GuideCase, dismiss: () -> Unit) {
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Screenshot DEMO", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = dismiss) { Text("Tutup screenshot") }
                }
                Text("Geser ke bawah untuk melihat seluruh gambar. Contoh development, bukan transaksi produksi.", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                    Image(painterResource(guideImage(guide.screenshot!!)), guideImageDescription(guide.screenshot),
                        Modifier.widthIn(max = 720.dp).fillMaxWidth().aspectRatio(1080f / 2400f), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

private fun guideImage(name: String): Int = when (name) {
    "form" -> R.drawable.guide_waste_form
    "records" -> R.drawable.guide_waste_records
    "detail" -> R.drawable.guide_waste_review
    "reports" -> R.drawable.guide_reports
    else -> error("Unknown guide screenshot: $name")
}

private fun guideImageDescription(name: String): String = when (name) {
    "form" -> "Form catatan limbah DEMO: tanggal, jenis, jumlah, satuan, sumber/lokasi, dan Simpan ke server."
    "records" -> "Daftar limbah DEMO dengan Tambah catatan limbah, Filter, Muat ulang dan status catatan."
    "detail" -> "Detail limbah DEMO: jumlah, pengaju, status Menunggu persetujuan dan tombol Setujui/Tolak."
    else -> "Laporan DEMO: jenis laporan, periode, material, filter tambahan dan format PDF/XLSX."
}