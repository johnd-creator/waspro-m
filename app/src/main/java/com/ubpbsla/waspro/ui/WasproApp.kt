@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ubpbsla.waspro.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.ubpbsla.waspro.BuildConfig
import com.ubpbsla.waspro.R
import com.ubpbsla.waspro.data.*
import com.ubpbsla.waspro.features.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Serializable
data class Page(val kind: String, val module: String = "", val id: String = "", val path: String = "", val title: String = "") : NavKey

@Composable
fun WasproApp(model: WasproViewModel) {
    val identity by model.user.collectAsStateWithLifecycle()
    val starting by model.starting.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val backStack = rememberNavBackStack(Page("home"))
    LaunchedEffect(identity?.id) { backStack.clear(); backStack.add(Page("home")) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); model.message.value = null }
    }
    MaterialTheme(colorScheme = FieldColors, shapes = FieldShapes) {
        Surface(Modifier.fillMaxSize()) {
            when {
                starting -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_waspro), "WASPRO", Modifier.size(54.dp).clip(MaterialTheme.shapes.medium))
                    Text("Menyiapkan ruang kerja…", Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
                identity == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    LoginScreen(busy, Modifier.padding(padding), model::login)
                }
                else -> {
                    val user = identity!!
                    val page = backStack.last() as Page
                    val title = page.title.ifBlank {
                        when (page.kind) {
                            "home" -> "Beranda"
                            "waste-menu" -> "Limbah"
                            "faba-menu" -> "FABA"
                            "approval-menu" -> "Persetujuan"
                            "more" -> "Lainnya"
                            "drafts" -> "Draft lokal"
                            "reports" -> "Laporan"
                            "about" -> "Tentang aplikasi"
                            else -> Modules.all.find { it.key == page.module }?.title ?: "WASPRO"
                        }
                    }
                    fun open(target: Page) {
                        if (!busy || target.kind == "editor") backStack.add(target)
                    }
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbar) },
                        topBar = {
                            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
                                if (backStack.size > 1) IconButton(onClick = { backStack.removeLastOrNull() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                                } else Image(painterResource(R.drawable.ic_waspro), "WASPRO",
                                    Modifier.padding(start = 12.dp, end = 4.dp).size(32.dp).clip(RoundedCornerShape(9.dp)))
                            })
                        },
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                val tabs = listOf("home" to "Beranda", "waste-menu" to "Limbah", "faba-menu" to "FABA") +
                                    (if (user.canReview) listOf("approval-menu" to "Persetujuan") else emptyList()) + ("more" to "Lainnya")
                                tabs.forEach { (kind, text) ->
                                    NavigationBarItem(selected = (backStack.firstOrNull() as? Page)?.kind == kind, onClick = {
                                        if (!busy) { backStack.clear(); backStack.add(Page(kind)) }
                                    }, icon = {
                                        Icon(when (kind) {
                                            "home" -> Icons.Default.Home
                                            "waste-menu" -> Icons.Default.Inventory2
                                            "faba-menu" -> Icons.Default.Factory
                                            "approval-menu" -> Icons.AutoMirrored.Filled.FactCheck
                                            else -> Icons.Default.Menu
                                        }, null)
                                    }, label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        },
                    ) { padding ->
                        Column(Modifier.padding(padding).fillMaxSize()) {
                            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (user.organization.text("id").isBlank()) {
                                InfoPanel(Icons.Default.Business, "Organisasi belum tersedia", "Hubungi administrator untuk memeriksa penugasan akun Anda.")
                                Button(onClick = model::logout) { Text("Keluar") }
                            } else NavDisplay(
                                backStack = backStack,
                                onBack = { if (!busy) backStack.removeLastOrNull() },
                                entryProvider = entryProvider {
                                    entry<Page> { target ->
                                        key(target) {
                                            PageContent(target, model, user, ::open) {
                                                if (backStack.size > 1) backStack.removeLastOrNull()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(busy: Boolean, modifier: Modifier = Modifier, submit: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LandscapeHeader("OPERASIONAL LINGKUNGAN", "WASPRO", "Pengelolaan limbah dan FABA")
            FormPanel {
                Text("Masuk ke akun Anda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Masukkan email dan password akun organisasi Anda.", Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !busy)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true, enabled = !busy,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Sembunyikan password" else "Tampilkan password") } })
                Spacer(Modifier.height(24.dp))
                Button({ submit(email, password) }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy && email.isNotBlank() && password.isNotBlank()) {
                    Text(if (busy) "Sedang masuk…" else "Masuk")
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: Page, model: WasproViewModel, user: UserContext, open: (Page) -> Unit, back: () -> Unit) {
    val module = Modules.all.find { it.key == page.module }
    val allowed = module == null || Modules.visible(module, user)
    if (!allowed) {
        ScrollContent {
            InfoPanel(Icons.Default.Lock, "Akses modul berubah", "Pilih modul lain yang tersedia untuk akun Anda.")
            OutlinedButton(onClick = back) { Text("Kembali") }
        }
        return
    }
    when (page.kind) {
        "home" -> DataPanel(page, "dashboard", model, header = {
            LandscapeHeader("RUANG KERJA", "Halo, ${user.user.text("name")}",
                listOf(user.organization.text("name"), user.raw.obj("role").text("name")).filter(String::isNotBlank).joinToString(" · "))
            val showWaste = Modules.visible(Modules.waste, user)
            if (showWaste || user.can("faba_dashboard.view")) SectionHeading("Akses cepat")
            if (showWaste) MenuTile("Catatan limbah", "Input dan pantau penyimpanan limbah", Icons.Default.Inventory2) { open(Page("list", "waste")) }
            if (user.can("faba_dashboard.view")) MenuTile("Dashboard FABA", "Saldo, produksi dan kapasitas TPS", Icons.Default.Factory) {
                open(Page("read", path = "faba/dashboard", title = "Dashboard FABA"))
            }
            SectionHeading("Ringkasan operasional")
        }) { state ->
            DashboardSummary(state.data)
        }
        "waste-menu" -> ScrollContent {
            LandscapeHeader("OPERASIONAL  /  LIMBAH", "Pengelolaan limbah", "Telusuri catatan dari penyimpanan hingga pengangkutan.")
            SectionHeading("Pencatatan dan pengangkutan")
            ModuleTiles(listOf(Modules.waste, Modules.hauling), user, open)
        }
        "faba-menu" -> ScrollContent {
            LandscapeHeader("OPERASIONAL  /  FABA", "Pusat material FABA", "Pantau produksi, pemanfaatan, dan saldo material.")
            if (user.can("faba_dashboard.view")) MenuTile("Dashboard FABA", "Ringkasan material dan TPS", Icons.Default.Assessment) {
                open(Page("read", path = "faba/dashboard", title = "Dashboard FABA"))
            }
            ModuleTiles(listOf(Modules.production, Modules.utilization, Modules.adjustments, Modules.periods), user, open, "Transaksi dan periode")
            ModuleTiles(Modules.recaps, user, open, "Rekap dan saldo")
            if (user.can("faba_opening_balance.manage")) listOf(Modules.opening, Modules.capacity).forEach {
                MenuTile(it.title, "Pengaturan operasional") { model.openEditor(it); open(Page("editor", it.key)) }
            }
            if (user.can("faba_approvals.view")) MenuTile("Riwayat FABA", "Persetujuan dan 100 aktivitas terbaru") { open(Page("read", Modules.history.key)) }
        }
        "approval-menu" -> ScrollContent {
            LandscapeHeader("PENGAWASAN", "Meja persetujuan", "Pilih jenis pengajuan untuk meninjau catatan dan mengambil tindakan.")
            SectionHeading("Jenis pengajuan")
            if (user.can("waste_records.approve") || user.can("waste_records.reject")) MenuTile("Persetujuan limbah", "Catatan menunggu review") {
                open(Page("list", "waste", path = "waste-records/pending-approval", title = "Persetujuan limbah"))
            }
            if (user.can("waste_hauling.approve") || user.can("waste_hauling.reject")) MenuTile("Persetujuan pengangkutan", "Pengajuan angkut") {
                open(Page("list", "hauling", path = "waste-haulings/pending-approval", title = "Persetujuan angkut"))
            }
            if (user.can("faba_approvals.approve") || user.can("faba_approvals.reject")) {
                MenuTile("Transaksi FABA", "Produksi, pemanfaatan dan penyesuaian") { open(Page("list", "production", path = "faba/movements/pending-approval", title = "Persetujuan transaksi FABA")) }
                MenuTile("Periode FABA", "Review rekap bulanan") { open(Page("list", "periods")) }
            }
        }
        "more" -> MoreScreen(model, user, open)
        "list" -> RecordList(page, module!!, model, user, open)
        "detail" -> RecordDetail(page, module!!, model, user, open, back)
        "read" -> DataPanel(page, page.path.ifBlank { module!!.path }, model, module?.filters.orEmpty()) {
            if (page.path == "faba/dashboard") DashboardSummary(it.data) else FormPanel { JsonDetails(it.data) }
        }
        "editor" -> EditorScreen(model, module!!, back)
        "drafts" -> {
            val drafts by model.drafts.collectAsStateWithLifecycle()
            ScrollContent {
                SectionHeading("Tersimpan di perangkat")
                if (drafts.isEmpty()) InfoPanel(Icons.Default.EditNote, "Belum ada draft lokal.", "Formulir yang disimpan di perangkat akan muncul di sini.")
                else Text("${drafts.size} draft · Buka untuk melanjutkan pengisian.", style = MaterialTheme.typography.bodyMedium)
                drafts.forEach { draft ->
                    val spec = Modules.find(draft.module)
                    FormPanel {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium)
                        Text("Terakhir diedit · ${timeText(draft.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { model.openEditor(spec, savedDraft = draft); open(Page("editor", spec.key, id = draft.id)) }) { Text("Buka") }
                            TextButton(onClick = { model.removeDraft(draft) }) { Text("Hapus") }
                        }
                    }
                }
            }
        }
        "reports" -> ReportsScreen(model, user)
        "about" -> ScrollContent {
            LandscapeHeader("INFORMASI APLIKASI", "WASPRO Mobile", "Versi ${BuildConfig.VERSION_NAME}")
            InfoPanel(Icons.Default.Info, "Bantuan operasional", "Untuk bantuan akun dan operasional, hubungi administrator organisasi Anda.")
        }
    }
}

@Composable
private fun MoreScreen(model: WasproViewModel, user: UserContext, open: (Page) -> Unit) {
    var logout by remember { mutableStateOf(false) }
    val drafts by model.drafts.collectAsStateWithLifecycle()
    ScrollContent {
        LandscapeHeader("AKUN ORGANISASI", user.user.text("name"), user.organization.text("name"))
        SectionHeading("Akun dan penyimpanan")
        MenuTile("Profil", user.user.text("email"), Icons.Default.Person) { open(Page("detail", "profile")) }
        MenuTile("Ganti password", "Kelola keamanan akun", Icons.Default.Lock) { model.openEditor(Modules.password); open(Page("editor", "password")) }
        MenuTile("Draft lokal", "${drafts.size} draft belum terkirim", Icons.Default.EditNote) { open(Page("drafts")) }
        MenuTile("Laporan", "Unduh dan bagikan data", Icons.Default.Assessment) { open(Page("reports")) }
        ModuleTiles(listOf(Modules.categories, Modules.characteristics, Modules.types, Modules.vendors) +
            Modules.all.filter { it.key in listOf("users", "roles") }, user, open, "Master data")
        SectionHeading("Aplikasi")
        MenuTile("Tentang aplikasi", "Informasi WASPRO Mobile") { open(Page("about")) }
        OutlinedButton(onClick = { logout = true }, modifier = Modifier.fillMaxWidth()) { Text("Keluar akun") }
    }
    if (logout) AlertDialog(onDismissRequest = { logout = false }, title = { Text("Keluar akun?") },
        text = { Text(if (drafts.isEmpty()) "Sesi dan data tersimpan akan dibersihkan." else "Ada ${drafts.size} draft belum terkirim. Keluar akan menghapus draft dan lampirannya dari perangkat.") },
        confirmButton = { TextButton({ logout = false; model.logout() }) { Text(if (drafts.isEmpty()) "Keluar" else "Hapus draft dan keluar") } },
        dismissButton = { TextButton({ logout = false }) { Text("Batal") } })
}

@Composable
private fun ModuleTiles(modules: List<ModuleSpec>, user: UserContext, open: (Page) -> Unit, heading: String? = null) {
    val visible = modules.filter { Modules.visible(it, user) }
    if (heading != null && visible.isNotEmpty()) SectionHeading(heading)
    visible.forEach { module ->
        val (icon, description) = when (module) {
            Modules.waste -> Icons.Default.Inventory2 to "Jenis, jumlah, dan lokasi penyimpanan"
            Modules.hauling -> Icons.Default.LocalShipping to "Sisa limbah dan riwayat pengangkutan"
            Modules.production -> Icons.Default.Factory to "Catatan produksi material"
            Modules.utilization -> Icons.Default.Recycling to "Pemanfaatan internal dan eksternal"
            Modules.adjustments -> Icons.Default.Tune to "Koreksi jumlah material"
            Modules.periods -> Icons.AutoMirrored.Filled.FactCheck to "Status persetujuan per periode"
            else -> Icons.Default.Folder to ""
        }
        MenuTile(module.title, description, icon) { open(Page("list", module.key)) }
    }
}

@Composable
private fun MenuTile(title: String, description: String, icon: ImageVector? = null, click: () -> Unit) {
    Card(onClick = click, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon ?: Icons.Default.Folder)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (description.isNotBlank()) Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScrollContent(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun DataPanel(page: Page, path: String, model: WasproViewModel, filters: List<String> = emptyList(),
    header: @Composable ColumnScope.() -> Unit = {}, content: @Composable ColumnScope.(ScreenData) -> Unit) {
    val key = page.toString()
    val screens by model.screens.collectAsStateWithLifecycle()
    val state = screens[key] ?: ScreenData(loading = true)
    val options by model.lookups.collectAsStateWithLifecycle()
    var filterJson by rememberSaveable { mutableStateOf("{}") }
    val query = wireJson.parseToJsonElement(filterJson).jsonObject.mapValues { it.value.jsonPrimitive.content }
    LaunchedEffect(key) {
        model.load(key, path, query)
        if (filters.any { it in lookupFilters }) model.loadOptions()
    }
    ScrollContent {
        header()
        FilterFields(filters, query, options) { filterJson = jsonObject(it).toString(); model.load(key, path, it) }
        StateHeader(state) { model.load(key, path, query) }
        if (state.data != JsonNull) content(state)
    }
}

@Composable
private fun StateHeader(state: ScreenData, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val status = when {
            state.loading -> "Memuat data…"
            state.cachedAt != null -> "Data tersimpan · ${timeText(state.cachedAt)}"
            state.failure != null -> "Pembaruan gagal"
            else -> "Diperbarui · ${timeText(state.loadedAt)}"
        }
        Text(status, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = refresh, enabled = !state.loading) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Muat ulang")
        }
    }
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.failure?.let {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Text(it, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (!state.loading && state.failure == null && (state.data == JsonNull || state.data == JsonArray(emptyList()))) {
        FormPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Inbox, null, tint = MaterialTheme.colorScheme.primary)
                Text("Belum ada data.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FilterFields(keys: List<String>, values: Map<String, String>, options: JsonObject, apply: (Map<String, String>) -> Unit) {
    if (keys.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by remember { mutableStateOf(values) }
    val active = keys.count { !values[it].isNullOrBlank() }
    OutlinedButton(onClick = { expanded = !expanded }) {
        Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (active == 0) "Filter" else "Filter · $active aktif")
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (expanded) {
        keys.forEach { key ->
            val lookupKey = lookupFilters[key]
            val choices = if (key == "status") JsonObject(mapOf("statuses" to JsonArray(listOf("draft", "pending_approval", "approved", "rejected").map(::JsonPrimitive)))) else options
            val field = InputField(key, label(key), kind = when {
                lookupKey != null || key == "status" -> FieldKind.Choice
                key in listOf("year", "month") -> FieldKind.Integer
                else -> FieldKind.Text
            }, lookup = lookupKey ?: if (key == "status") "statuses" else null)
            FormField(field, draft[key].orEmpty(), null, choices, draft, enabled = true) { draft = draft + (key to it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { apply(draft.filterValues(String::isNotBlank)); expanded = false }) { Text("Terapkan") }
            TextButton(onClick = { draft = emptyMap(); apply(emptyMap()); expanded = false }) { Text("Reset filter") }
        }
    }
}

@Composable
private fun RecordList(page: Page, module: ModuleSpec, model: WasproViewModel, user: UserContext, open: (Page) -> Unit) {
    val path = page.path.ifBlank { module.path }
    val key = page.toString()
    val screens by model.screens.collectAsStateWithLifecycle()
    val state = screens[key] ?: ScreenData(loading = true)
    val options by model.lookups.collectAsStateWithLifecycle()
    var filterJson by rememberSaveable { mutableStateOf("{}") }
    val query = wireJson.parseToJsonElement(filterJson).jsonObject.mapValues { it.value.jsonPrimitive.content } +
        if (module == Modules.hauling || path == "faba/movements/pending-approval") mapOf("paginate" to "true") else emptyMap()
    LaunchedEffect(key) {
        model.load(key, path, query)
        if (module.filters.any { it in lookupFilters }) model.loadOptions()
    }
    ScrollContent {
        if (module.createPermission != null && user.can(module.createPermission) && page.path.isBlank() && module != Modules.hauling) {
            Button({ model.openEditor(module); open(Page("editor", module.key)) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Tambah ${module.title.lowercase()}") }
        }
        FilterFields(if (page.path.isBlank()) module.filters else emptyList(), query, options) {
            filterJson = jsonObject(it).toString()
            model.load(key, path, it + query.filterKeys { key -> key == "paginate" })
        }
        StateHeader(state) { model.load(key, path, query) }
        val rows = state.data as? JsonArray
        rows?.forEach { raw ->
            val item = raw.objectOrEmpty()
            val targetModule = if (path == "faba/movements/pending-approval") when (item.text("movement_type")) {
                "utilization_internal", "utilization_external" -> Modules.utilization
                "adjustment_in", "adjustment_out" -> Modules.adjustments
                else -> Modules.production
            } else module
            val title = listOf("record_number", "hauling_number", "display_number", "period_label", "name").firstNotNullOfOrNull { item.text(it).takeIf(String::isNotBlank) }
                ?: statuses[item.text("movement_type")] ?: module.title
            val movement = if (module in listOf(Modules.production, Modules.utilization, Modules.adjustments)) FabaMovementDto.from(item) else null
            val quantityText = runCatching {
                val quantity = if (module == Modules.waste) WasteRecordDto.from(item).quantity else item.text("quantity").toBigDecimal()
                NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply { maximumFractionDigits = 2 }.format(quantity)
            }.getOrDefault("")
            val summary = listOf(item.text("date").ifBlank { item.text("transaction_date") },
                item.obj("waste_type").text("name"), item.text("source"), statuses[movement?.material ?: item.text("material_type")].orEmpty(),
            ).filter(String::isNotBlank).joinToString(" · ")
            val statusKey = movement?.approvalStatus.orEmpty().ifBlank { item.text("approval_status").ifBlank { item.text("status") } }
            val statusText = item.text("status_label").ifBlank { statuses[statusKey] ?: statusKey }
            val amount = listOf(quantityText, item.text("unit")).filter(String::isNotBlank).joinToString(" ")
            val domainSummary = when {
                module == Modules.hauling && page.path.isBlank() -> runCatching { HaulingBacklogDto.from(item) }.getOrNull()?.let {
                    "$summary · Sisa ${it.remaining.toPlainString()} · Reservasi ${it.reserved.toPlainString()}"
                }
                else -> null
            } ?: summary
            RecordCard(title, domainSummary.trim(' ', '·'), amount, statusKey, statusText) {
                val id = if (module == Modules.periods) "${item.text("year")}/${item.text("month")}" else item.text("id")
                val detailPath = when {
                    module == Modules.hauling && page.path.isBlank() -> "waste-haulings/history/$id"
                    else -> "${targetModule.path}/$id"
                }
                open(Page("detail", targetModule.key, id, detailPath, title))
            }
        }
        if (rows == null && state.data != JsonNull) FormPanel { JsonDetails(state.data) }
        val pagination = state.meta.obj("pagination")
        val current = pagination.text("current_page").toIntOrNull() ?: 1
        val last = pagination.text("last_page").toIntOrNull() ?: 1
        if (current < last) OutlinedButton(onClick = { model.load(key, path, query + ("page" to (current + 1).toString()), append = true) }, enabled = !state.loading) { Text("Muat berikutnya") }
    }
}

private data class PendingAction(val action: String, val path: String, val requestKey: String = UUID.randomUUID().toString())

@Composable
private fun RecordDetail(page: Page, module: ModuleSpec, model: WasproViewModel, user: UserContext, open: (Page) -> Unit, back: () -> Unit) {
    val path = page.path.ifBlank { module.path + if (page.id.isNotBlank()) "/${page.id}" else "" }
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    val busy by model.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DataPanel(page, path, model) { state ->
        val raw = state.data.objectOrEmpty()
        val record = when {
            module == Modules.periods -> raw.obj("approval")
            module == Modules.hauling && "record" in raw -> raw.obj("record")
            else -> raw
        }
        SectionHeading(if (module == Modules.profile) "Informasi akun" else "Rincian catatan")
        FormPanel { JsonDetails(raw) }
        if (module == Modules.history) Text("Riwayat aktivitas menampilkan maksimal 100 entri terbaru.")
        if (state.cachedAt == null) {
            val actions = if (module == Modules.profile) setOf("update") else record.actions()
            val available = actions.filter { it !in listOf("view", "review") }
            if (available.isNotEmpty()) SectionHeading("Tindakan tersedia")
            available.forEach { action ->
                OutlinedButton(onClick = {
                    when (action) {
                        "update" -> { model.openEditor(module, record); open(Page("editor", module.key, page.id)) }
                        "create_hauling" -> { model.openEditor(Modules.hauling, initial = mapOf("waste_record_id" to record.text("id"))); open(Page("editor", "hauling")) }
                        else -> {
                            val actionPath = when {
                                module == Modules.periods && action == "submit" -> "faba/approvals/submit"
                                module in listOf(Modules.production, Modules.utilization, Modules.adjustments) && action in listOf("approve", "reject") -> "faba/movements/${page.id}/$action"
                                action == "delete" -> path
                                else -> "$path/${action.replace('_', '-')}"
                            }
                            reason = ""
                            pending = PendingAction(action, actionPath)
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(actionLabels[action] ?: label(action)) }
            }
            if (module == Modules.hauling && "record" in raw) {
                raw.array("haulings").forEach { value ->
                    val hauling = value.objectOrEmpty()
                    MenuTile(hauling.text("hauling_number"), statuses[hauling.text("status")].orEmpty()) {
                        open(Page("detail", "hauling", hauling.text("id"), "waste-haulings/${hauling.text("id")}"))
                    }
                }
            }
        }
        if (module == Modules.utilization && raw.text("attachment_path").isNotBlank()) {
            TextButton(onClick = { model.download("faba/utilization/${page.id}/attachment", emptyMap(), "lampiran-${page.id}.${raw.text("attachment_path").substringAfterLast('.', "pdf")}") { shareFile(context, it) } }) { Text("Buka / bagikan lampiran") }
        }
    }
    pending?.let { action ->
        val needsReason = action.action in listOf("reject", "reopen")
        AlertDialog(onDismissRequest = { if (!busy) pending = null },
            title = { Text("${actionLabels[action.action]}?") },
            text = {
                Column {
                    Text("Pastikan data sudah ditinjau sebelum melanjutkan.")
                    if (needsReason || action.action == "approve") OutlinedTextField(reason, { reason = it },
                        label = { Text(if (needsReason) "Alasan wajib" else "Catatan (opsional)") })
                }
            },
            confirmButton = { TextButton(enabled = !busy && (!needsReason || reason.isNotBlank()), onClick = {
                val fields = when (action.action) {
                    "reject" -> mapOf((if (module in listOf(Modules.waste, Modules.hauling)) "rejection_reason" else "rejection_note") to reason)
                    "reopen" -> mapOf("reopen_note" to reason)
                    "approve" -> mapOf((if (module in listOf(Modules.waste, Modules.hauling)) "approval_notes" else "approval_note") to reason)
                    "submit" -> if (module == Modules.periods) mapOf("year" to page.id.substringBefore('/'), "month" to page.id.substringAfter('/')) else emptyMap()
                    else -> emptyMap()
                }
                model.action(action.path, action.action, fields, action.requestKey) {
                    pending = null
                    if (action.action == "delete") back() else model.load(page.toString(), path)
                }
            }) { Text("Konfirmasi") } },
            dismissButton = { TextButton(onClick = { pending = null }, enabled = !busy) { Text("Batal") } })
    }
}

@Composable
private fun EditorScreen(model: WasproViewModel, module: ModuleSpec, back: () -> Unit) {
    val state by model.editor.collectAsStateWithLifecycle()
    val conflict by model.conflict.collectAsStateWithLifecycle()
    val lookup by model.lookups.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::attach) }
    val context = LocalContext.current
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraPath?.let { model.attach(FileProvider.getUriForFile(context, context.packageName + ".files", File(it))) }
    }
    val current = state?.takeIf { it.module == module.key }
    ScrollContent {
        if (current == null) {
            Text("Menyiapkan formulir…")
            TextButton(onClick = { model.openEditor(module) }) { Text("Coba kembali") }
            return@ScrollContent
        }
        val locked = current.draft?.submittedPayload != null
        if (locked) Text("Hasil pengiriman belum dipastikan. Coba kirim kembali untuk memeriksa permintaan yang sama.", color = MaterialTheme.colorScheme.error)
        if (current.saved) Text("Draft tersimpan di perangkat", color = MaterialTheme.colorScheme.primary)
        conflict?.let { latest ->
            Text("Data server terbaru", style = MaterialTheme.typography.titleMedium)
            Text("Bandingkan dengan isian Anda sebelum memakai versi terbaru sebagai acuan.")
            JsonDetails(latest)
            if ("update" in latest.actions()) OutlinedButton(onClick = model::acceptLatestVersion, enabled = !busy) { Text("Gunakan versi terbaru sebagai acuan") }
            else Text("Status atau izin server sudah tidak mengizinkan perubahan.", color = MaterialTheme.colorScheme.error)
            HorizontalDivider()
        }
        SectionHeading("Isian formulir")
        FormPanel {
            Text("Kolom bertanda * wajib diisi.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                module.fields.forEach { field ->
                    val external = current.values["movement_type"] == "utilization_external"
                    val hidden = module == Modules.utilization && when (field.key) {
                        "vendor_id", "document_number", "document_date" -> !external
                        "internal_destination_id" -> external
                        else -> false
                    }
                    if (!hidden && field.key != "waste_record_id") FormField(field, current.values[field.key].orEmpty(), current.errors[field.key], lookup, current.values, enabled = !busy && !locked) {
                        model.change(field.key, it)
                    }
                }
            }
        }
        if (module == Modules.utilization) {
            SectionHeading("Dokumen pendukung")
            Text(current.draft?.attachment?.let { "Lampiran dipilih · ${File(it).name}" }
                ?: if (current.existingAttachment) "Lampiran server tersedia" else "Belum ada lampiran")
            current.errors["attachment"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, enabled = !busy && !locked) { Text("Pilih file") }
                OutlinedButton(onClick = {
                    val file = context.cacheDir.resolve("camera").apply { mkdirs() }.resolve("${UUID.randomUUID()}.jpg")
                    cameraPath = file.path
                    camera.launch(FileProvider.getUriForFile(context, context.packageName + ".files", file))
                }, enabled = !busy && !locked) { Text("Foto") }
            }
            Text("JPG, PNG, WEBP atau PDF · Maksimal 5 MB", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { model.submitEditor { back() } }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text(if (locked) "Periksa / kirim kembali" else "Simpan ke server") }
        if (current.draft != null) OutlinedButton(onClick = back, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("Lanjutkan nanti") }
    }
}

@Composable
private fun FormField(field: InputField, value: String, error: String?, lookup: JsonObject, values: Map<String, String>, enabled: Boolean, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    when (field.kind) {
        FieldKind.Date -> {
            var picking by remember { mutableStateOf(false) }
            OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled,
                label = { Text(field.label + if (field.required) " *" else "") }, isError = error != null,
                supportingText = { Text("YYYY-MM-DD") }, singleLine = true,
                trailingIcon = { IconButton(onClick = { picking = true }, enabled = enabled) { Icon(Icons.Default.CalendarMonth, "Pilih tanggal") } })
            if (picking) {
                val initial = runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
                val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
                DatePickerDialog(onDismissRequest = { picking = false },
                    confirmButton = { TextButton(onClick = {
                        picker.selectedDateMillis?.let { change(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                        picking = false
                    }) { Text("Pilih") } },
                    dismissButton = { TextButton(onClick = { picking = false }) { Text("Batal") } }) {
                    DatePicker(picker)
                }
            }
        }
        FieldKind.Boolean -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = value == "true", onCheckedChange = { change(it.toString()) }, enabled = enabled)
            Text(field.label, Modifier.padding(start = 12.dp))
        }
        FieldKind.Choice -> {
            val options = lookup.array(field.lookup.orEmpty()).map { option ->
                if (option is JsonPrimitive) option.content to (statuses[option.content] ?: label(option.content))
                else option.objectOrEmpty().let { it.text("id") to it.text("name") }
            }
            ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = !expanded }) {
                OutlinedTextField(options.firstOrNull { it.first == value }?.second ?: value, {}, readOnly = true,
                    label = { Text(field.label + if (field.required) " *" else "") }, isError = error != null,
                    enabled = enabled, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
                    if (!field.required) DropdownMenuItem(text = { Text("Tidak dipilih") }, onClick = { change(""); expanded = false })
                    options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { change(id); expanded = false }) }
                }
            }
        }
        else -> OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled,
            label = { Text(field.label + if (field.required) " *" else "") }, isError = error != null,
            supportingText = if (field.kind == FieldKind.Date) ({ Text("YYYY-MM-DD") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = when (field.kind) {
                FieldKind.Decimal -> KeyboardType.Decimal
                FieldKind.Integer -> KeyboardType.Number
                FieldKind.Password -> KeyboardType.Password
                else -> KeyboardType.Text
            }),
            visualTransformation = if (field.kind == FieldKind.Password) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = field.key !in listOf("note", "notes", "description", "address"))
    }
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun ReportsScreen(model: WasproViewModel, user: UserContext) {
    val context = LocalContext.current
    var year by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    var month by rememberSaveable { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var material by rememberSaveable { mutableStateOf("") }
    var extraFilters by rememberSaveable { mutableStateOf("{}") }
    val options by model.lookups.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadOptions() }
    val busy by model.busy.collectAsStateWithLifecycle()
    ScrollContent {
        SectionHeading("Periode laporan")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(year, { year = it }, Modifier.weight(1f), singleLine = true, label = { Text("Tahun") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(month, { month = it }, Modifier.weight(1f), singleLine = true, label = { Text("Bulan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        SectionHeading("Material")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("" to "Semua", "fly_ash" to "Fly Ash", "bottom_ash" to "Bottom Ash").forEach { (key, title) ->
                FilterChip(selected = material == key, onClick = { material = key }, label = { Text(title) })
            }
        }
        val extras = wireJson.parseToJsonElement(extraFilters).jsonObject.mapValues { it.value.jsonPrimitive.content }
        var showFilters by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(onClick = { showFilters = !showFilters }) {
            Text("Filter tambahan" + if (extras.values.any(String::isNotBlank)) " · aktif" else "")
        }
        if (showFilters) listOf("vendor_id", "internal_destination_id", "purpose_id", "movement_type").forEach { key ->
            FormField(InputField(key, label(key), kind = FieldKind.Choice, lookup = lookupFilters[key]),
                extras[key].orEmpty(), null, options, extras, enabled = !busy) {
                extraFilters = jsonObject(extras + (key to it)).toString()
            }
        }
        val filters = (extras + mapOf("year" to year, "month" to month, "material_type" to material)).filterValues(String::isNotBlank)
        val validPeriod = year.toIntOrNull() in 2000..2100 && month.toIntOrNull() in 1..12
        if (!validPeriod) Text("Isi tahun 2000–2100 dan bulan 1–12 untuk mengunduh laporan.", color = MaterialTheme.colorScheme.error)
        if (user.can("faba_reports.export")) SectionHeading("Unduh laporan FABA")
        if (user.can("faba_reports.export")) listOf(
            "monthly" to "Bulanan", "yearly" to "Tahunan", "vendors" to "Vendor", "internal-destinations" to "Tujuan internal",
            "purposes" to "Tujuan penggunaan", "stock-card" to "Kartu stok", "anomalies" to "Anomali", "analysis-matrix" to "Matriks analisis",
        ).forEach { (report, title) ->
            FormPanel {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pdf", "xlsx").forEach { format ->
                        TextButton(onClick = { model.download("faba/reports/$report.$format", filters, "waspro-$report-$year-$month.$format") { shareFile(context, it) } }, enabled = !busy && validPeriod) { Text(format.uppercase()) }
                    }
                }
            }
        }
        val exports = listOf(Modules.waste, Modules.production, Modules.utilization).filter { Modules.visible(it, user) }
        if (exports.isNotEmpty()) SectionHeading("Ekspor data CSV")
        exports.forEach { module ->
            OutlinedButton(onClick = { model.download("${module.path}/export/csv", filters, "waspro-${module.key}.csv") { shareFile(context, it) } }, enabled = !busy && validPeriod) { Text("CSV ${module.title}") }
        }
    }
}

private val lookupFilters = mapOf(
    "material_type" to "materials", "movement_type" to "movement_types", "vendor_id" to "vendors",
    "internal_destination_id" to "internal_destinations", "purpose_id" to "purposes",
)

@Composable
internal fun JsonDetails(element: JsonElement, depth: Int = 0) {
    when (element) {
        is JsonObject -> {
            val ignored = setOf("id", "resource_version", "allowed_actions", "attachment_path", "password", "token", "remember_token", "is_super_admin", "deleted_at", "can_submit", "can_approve", "can_reject", "can_review", "can_reopen")
            element.filterKeys { it !in ignored && !it.endsWith("_id") && it + "_label" !in element && it != "slug" }.forEach { (key, value) ->
                if (value != JsonNull && value.toString() !in listOf("[]", "{}", "\"\"")) {
                    if (value is JsonPrimitive) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(label(key), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Text(displayValue(key, value), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(label(key), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(12.dp)) { JsonDetails(value, depth + 1) }
                        }
                    }
                }
            }
        }
        is JsonArray -> element.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
            JsonDetails(item, depth + 1)
        }
        is JsonPrimitive -> Text(statuses[element.content] ?: element.content)
        else -> Unit
    }
}

internal fun displayValue(key: String, value: JsonPrimitive): String {
    val text = value.content
    statuses[text]?.let { return it }
    if (key.endsWith("_at")) {
        return runCatching { OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))) }.getOrDefault(text)
    }
    if (!value.isString) text.toBigDecimalOrNull()?.let { return NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply { maximumFractionDigits = 2 }.format(it) }
    return text
}
private fun timeText(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.forLanguageTag("id-ID")))
private fun shareFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val mime = when (file.extension.lowercase()) {
        "pdf" -> "application/pdf"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        "png" -> "image/png"
        else -> "image/jpeg"
    }
    val intent = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Buka / bagikan"))
}
