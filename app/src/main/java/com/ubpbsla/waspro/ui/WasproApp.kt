@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ubpbsla.waspro.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.stateDescription
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
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
    LaunchedEffect(message, identity?.id) {
        if (identity == null) return@LaunchedEffect
        message?.let { snackbar.showSnackbar(it); model.message.value = null }
    }
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) FieldDarkColors else FieldColors, shapes = FieldShapes, typography = FieldTypography) {
        Surface(Modifier.fillMaxSize()) {
            when {
                starting -> Column(Modifier.fillMaxSize().safeDrawingPadding(), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_waspro), "WASPRO", Modifier.size(54.dp).clip(MaterialTheme.shapes.medium))
                    Text("Menyiapkan ruang kerja…", Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
                identity == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    LoginScreen(busy, Modifier.padding(padding).consumeWindowInsets(padding), message) { email, password ->
                        model.message.value = null
                        model.login(email, password)
                    }
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
                            "guide", "guide-case" -> "Panduan penggunaan"
                            else -> Modules.all.find { it.key == page.module }?.title ?: "WASPRO"
                        }
                    }
                    fun open(target: Page) {
                        if (!busy || target.kind == "editor") {
                            if (target.kind == "approval-menu") backStack.clear()
                            backStack.add(target)
                        }
                    }
                    val wideLayout = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() >= 840.dp }
                    val tabs = listOf("home" to "Beranda", "waste-menu" to "Limbah", "faba-menu" to "FABA") +
                        (if (user.canReview) listOf("approval-menu" to "Tinjau") else emptyList()) + ("more" to "Lainnya")
                    fun goBack() {
                        if (!busy && backStack.size > 1) {
                            if (page.kind == "editor") model.leaveEditor { backStack.removeLastOrNull() }
                            else backStack.removeLastOrNull()
                        }
                    }
                    fun selectTab(kind: String) {
                        if (!busy) { backStack.clear(); backStack.add(Page(kind)) }
                    }
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbar) },
                        topBar = {
                            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
                                if (backStack.size > 1) IconButton(onClick = { goBack() }, enabled = !busy) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                                } else Image(painterResource(R.drawable.ic_waspro), "WASPRO",
                                    Modifier.padding(start = 12.dp, end = 4.dp).size(32.dp).clip(RoundedCornerShape(9.dp)))
                            }, actions = {
                                if (BuildConfig.FLAVOR != "production") Surface(
                                    modifier = Modifier.padding(end = 12.dp), shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) { Text(if (wideLayout) BuildConfig.FLAVOR.uppercase() else BuildConfig.FLAVOR.take(3).uppercase(),
                                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp).semantics { contentDescription = "Lingkungan ${BuildConfig.FLAVOR}" },
                                    style = MaterialTheme.typography.labelSmall) }
                            })
                        },
                        bottomBar = {
                            if (page.kind != "editor" && !wideLayout) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                                tabs.forEach { (kind, text) ->
                                    NavigationBarItem(colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                        selected = (backStack.firstOrNull() as? Page)?.kind == kind, onClick = {
                                        selectTab(kind)
                                    }, enabled = !busy, icon = {
                                        Icon(navigationIcon(kind, (backStack.firstOrNull() as? Page)?.kind == kind), null)
                                    }, label = { Text(if (kind == "approval-menu") "Tinjau" else text, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        },
                    ) { padding ->
                        Row(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
                            if (wideLayout && page.kind != "editor") NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxHeight().width(104.dp).verticalScroll(rememberScrollState())
                            ) {
                                Spacer(Modifier.height(12.dp))
                                tabs.forEach { (kind, text) ->
                                    NavigationRailItem(selected = (backStack.firstOrNull() as? Page)?.kind == kind,
                                        onClick = { selectTab(kind) }, enabled = !busy,
                                        icon = { Icon(navigationIcon(kind, (backStack.firstOrNull() as? Page)?.kind == kind), null) }, label = { Text(text) })
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (user.organization.text("id").isBlank()) {
                                InfoPanel(Icons.Default.Business, "Organisasi belum tersedia", "Hubungi administrator untuk memeriksa penugasan akun Anda.")
                                Button(onClick = model::logout) { Text("Keluar") }
                            } else NavDisplay(
                                backStack = backStack,
                                onBack = { goBack() },
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
}

private fun navigationIcon(kind: String, selected: Boolean = true): ImageVector = when (kind) {
    "home" -> if (selected) Icons.Default.Home else Icons.Outlined.Home
    "waste-menu" -> if (selected) Icons.Default.Inventory2 else Icons.Outlined.Inventory2
    "faba-menu" -> if (selected) Icons.Default.Factory else Icons.Outlined.Factory
    "approval-menu" -> if (selected) Icons.AutoMirrored.Filled.FactCheck else Icons.AutoMirrored.Outlined.FactCheck
    else -> Icons.Default.Menu
}

@Composable
fun LoginScreen(busy: Boolean, modifier: Modifier = Modifier, error: String? = null, submit: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val canSubmit = !busy && email.isNotBlank() && password.isNotBlank()
    fun signIn() {
        if (canSubmit) {
            focus.clearFocus()
            submit(email, password)
        }
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).imePadding()
        .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.widthIn(max = 960.dp).fillMaxWidth()) { LoginMasthead() }
        Column(Modifier.widthIn(max = 500.dp).fillMaxWidth().padding(24.dp)) {
            Text("Masuk ke akun Anda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Masukkan email dan password akun organisasi Anda.", Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress }, label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }), enabled = !busy)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().semantics { contentType = ContentType.Password }, label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                singleLine = true, enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { signIn() }),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = !busy) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Sembunyikan password" else "Tampilkan password") } })
            error?.let { Text(it, Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
            Button({ signIn() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.small, enabled = canSubmit) {
                Text(if (busy) "Sedang masuk…" else "Masuk", Modifier.weight(1f))
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
            }
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Business, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Akses sesuai peran dan organisasi Anda", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (user.canReview) {
                Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small, onClick = { open(Page("approval-menu")) }) {
                    Icon(Icons.AutoMirrored.Filled.FactCheck, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tinjau pengajuan")
                }
            } else if (Modules.visible(Modules.waste, user) && user.can(Modules.waste.createPermission.orEmpty())) {
                Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small, onClick = { model.openEditor(Modules.waste); open(Page("editor", "waste")) }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Catat limbah")
                }
            }
            val drafts by model.drafts.collectAsStateWithLifecycle()
            if (drafts.isNotEmpty()) MenuTile("Lanjutkan draft", "${drafts.size} draft di perangkat", icon = Icons.Default.EditNote) { open(Page("drafts")) }
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
            ModuleTiles(listOf(Modules.production, Modules.utilization, Modules.adjustments, Modules.periods), user, open, "Transaksi dan periode")
            if (user.can("faba_dashboard.view")) MenuTile("Dashboard FABA", "Ringkasan material dan TPS", icon = Icons.Default.Assessment) {
                open(Page("read", path = "faba/dashboard", title = "Dashboard FABA"))
            }
            ModuleTiles(Modules.recaps, user, open, "Rekap dan saldo")
            if (user.can("faba_opening_balance.manage")) listOf(Modules.opening, Modules.capacity).forEach {
                MenuTile(it.title, "Pengaturan operasional", icon = Icons.Default.Tune) { model.openEditor(it); open(Page("editor", it.key)) }
            }
            if (user.can("faba_approvals.view")) MenuTile("Riwayat FABA", "Persetujuan dan 100 aktivitas terbaru", icon = Icons.Default.History) { open(Page("read", Modules.history.key)) }
        }
        "approval-menu" -> ScrollContent {
            LandscapeHeader("PENGAWASAN", "Meja persetujuan", "Pilih jenis pengajuan untuk meninjau catatan dan mengambil tindakan.")
            SectionHeading("Jenis pengajuan")
            if (user.can("waste_records.approve") || user.can("waste_records.reject")) MenuTile("Persetujuan limbah", "Catatan menunggu review", icon = Icons.AutoMirrored.Filled.FactCheck) {
                open(Page("list", "waste", path = "waste-records/pending-approval", title = "Persetujuan limbah"))
            }
            if (user.can("waste_hauling.approve") || user.can("waste_hauling.reject")) MenuTile("Persetujuan pengangkutan", "Pengajuan angkut", icon = Icons.Default.LocalShipping) {
                open(Page("list", "hauling", path = "waste-haulings/pending-approval", title = "Persetujuan angkut"))
            }
            if (user.can("faba_approvals.approve") || user.can("faba_approvals.reject")) {
                MenuTile("Transaksi FABA", "Produksi, pemanfaatan dan penyesuaian", icon = Icons.Default.Factory) { open(Page("list", "production", path = "faba/movements/pending-approval", title = "Persetujuan transaksi FABA")) }
                MenuTile("Periode FABA", "Review rekap bulanan", icon = Icons.Default.CalendarMonth) { open(Page("list", "periods")) }
            }
        }
        "more" -> MoreScreen(model, user, open)
        "guide" -> GuideIndex(user) { open(Page("guide-case", id = it)) }
        "guide-case" -> GuideDetail(page.id)
        "list" -> RecordList(page, module!!, model, user, open)
        "detail" -> RecordDetail(page, module!!, model, user, open, back)
        "read" -> DataPanel(page, page.path.ifBlank { module!!.path }, model, module?.filters.orEmpty(), header = {
            LandscapeHeader("RINGKASAN OPERASIONAL", page.title.ifBlank { module?.title.orEmpty() }, "Periksa periode dan status pembaruan sebelum menggunakan data.")
        }) {
            if (page.path == "faba/dashboard") DashboardSummary(it.data) else FormPanel { JsonDetails(it.data) }
        }
        "editor" -> EditorScreen(model, module!!, back)
        "drafts" -> {
            val drafts by model.drafts.collectAsStateWithLifecycle()
            var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
            drafts.find { it.id == deleteId }?.let { draft ->
                AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Hapus draft lokal?") },
                    text = { Text("Isian ${Modules.find(draft.module).title} yang disimpan di perangkat akan dihapus.") },
                    confirmButton = { TextButton(onClick = { model.removeDraft(draft); deleteId = null }) { Text("Hapus draft") } },
                    dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } })
            }
            ScrollContent {
                LandscapeHeader("PENYIMPANAN LOKAL", "Lanjutkan pekerjaan Anda", "Draft belum dikirim ke server. Buka untuk meninjau dan melanjutkan pengisian.")
                if (drafts.isEmpty()) InfoPanel(Icons.Default.EditNote, "Belum ada draft lokal.", "Formulir yang disimpan di perangkat akan muncul di sini.")
                else Text("${drafts.size} draft · Buka untuk melanjutkan pengisian.", style = MaterialTheme.typography.bodyMedium)
                drafts.forEach { draft ->
                    val spec = Modules.find(draft.module)
                    FormPanel {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium)
                        Text("Terakhir diedit · ${timeText(draft.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { model.openEditor(spec, savedDraft = draft); open(Page("editor", spec.key, id = draft.id)) }) { Text("Lanjutkan") }
                            TextButton(onClick = { deleteId = draft.id }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
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
        MenuTile("Profil", user.user.text("email"), icon = Icons.Default.Person) { open(Page("detail", "profile")) }
        MenuTile("Ganti password", "Kelola keamanan akun", icon = Icons.Default.Lock) { model.openEditor(Modules.password); open(Page("editor", "password")) }
        MenuTile("Draft lokal", "${drafts.size} draft belum terkirim", icon = Icons.Default.EditNote) { open(Page("drafts")) }
        MenuTile("Laporan", "Unduh dan bagikan data", icon = Icons.Default.Assessment) { open(Page("reports")) }
        ModuleTiles(listOf(Modules.categories, Modules.characteristics, Modules.types, Modules.vendors) +
            Modules.all.filter { it.key in listOf("users", "roles") }, user, open, "Master data")
        SectionHeading("Aplikasi")
        MenuTile("Panduan penggunaan", "Langkah per role, kasus, dan screenshot", icon = Icons.AutoMirrored.Filled.MenuBook) { open(Page("guide")) }
        MenuTile("Tentang aplikasi", "Informasi WASPRO Mobile", icon = Icons.Default.Info) { open(Page("about")) }
        OutlinedButton(onClick = { logout = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Keluar akun", color = MaterialTheme.colorScheme.error) }
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
    if (visible.isEmpty() && heading == null) InfoPanel(Icons.Default.Lock, "Belum ada modul tersedia", "Modul mengikuti hak akses akun Anda. Hubungi administrator jika akses perlu diperbarui.")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp && LocalDensity.current.fontScale < 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        visible.chunked(columns).forEach { group ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            group.forEach { module ->
        val (icon, description) = when (module) {
            Modules.waste -> Icons.Default.Inventory2 to "Jenis, jumlah, dan lokasi penyimpanan"
            Modules.hauling -> Icons.Default.LocalShipping to "Sisa limbah dan riwayat pengangkutan"
            Modules.production -> Icons.Default.Factory to "Catatan produksi material"
            Modules.utilization -> Icons.Default.Recycling to "Pemanfaatan internal dan eksternal"
            Modules.adjustments -> Icons.Default.Tune to "Koreksi jumlah material"
            Modules.periods -> Icons.AutoMirrored.Filled.FactCheck to "Status persetujuan per periode"
            Modules.categories -> Icons.Default.Category to "Kelompok pengelolaan limbah"
            Modules.characteristics -> Icons.Default.HealthAndSafety to "Sifat dan klasifikasi bahaya"
            Modules.types -> Icons.Default.Inventory2 to "Jenis, masa simpan, dan biaya angkut"
            Modules.vendors -> Icons.Default.LocalShipping to "Mitra, kontak, dan izin operasional"
            else -> Icons.Default.Folder to when (module.key) {
                "users" -> "Direktori akun organisasi"
                "roles" -> "Peran dan rincian hak akses"
                "recap-monthly" -> "Produksi dan pemanfaatan per bulan"
                "recap-yearly" -> "Ringkasan material sepanjang tahun"
                "recap-vendors" -> "Pemanfaatan melalui mitra"
                "recap-balance" -> "Saldo dan pergerakan material"
                "recap-stock-card" -> "Jejak transaksi stok material"
                else -> ""
            }
        }
        MenuTile(module.title, description, Modifier.weight(1f).fillMaxHeight(), icon) { open(Page("list", module.key)) }
            }
            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        }
    }
}

@Composable
private fun MenuTile(title: String, description: String, modifier: Modifier = Modifier, icon: ImageVector? = null, click: () -> Unit) {
    Card(onClick = click, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(14.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon ?: Icons.Default.Folder)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (description.isNotBlank()) Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScrollContent(modifier: Modifier = Modifier, keyboardInsets: Boolean = true, scrollState: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()
            .then(if (keyboardInsets) Modifier.imePadding() else Modifier).verticalScroll(scrollState)
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
        val summaryPage = page.kind == "home" || page.path == "faba/dashboard"
        val needsAttention = state.loading || state.failure != null || state.cachedAt != null
        if (!summaryPage || needsAttention) StateHeader(state) { model.load(key, path, query) }
        if (state.data != JsonNull) content(state)
        if (summaryPage && !needsAttention) StateHeader(state) { model.load(key, path, query) }
    }
}

@Composable
internal fun StateHeader(state: ScreenData, refresh: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(when { state.cachedAt != null -> Icons.Default.CloudOff; state.failure != null -> Icons.Default.SyncProblem; else -> Icons.Default.Update },
            null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.failure?.let {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            Text(it, Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (!state.loading && state.failure == null && (state.data == JsonNull || state.data == JsonArray(emptyList()))) {
        InfoPanel(Icons.Default.Inbox, "Belum ada data.", "Catatan yang tersedia akan muncul di sini. Coba sesuaikan filter atau muat ulang.")
    }
}

@Composable
private fun FilterFields(keys: List<String>, values: Map<String, String>, options: JsonObject, apply: (Map<String, String>) -> Unit) {
    if (keys.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by remember { mutableStateOf(values) }
    var filterErrors by remember { mutableStateOf(emptyMap<String, String>()) }
    val active = keys.count { !values[it].isNullOrBlank() }
    OutlinedButton(onClick = { draft = values; filterErrors = emptyMap(); expanded = true }, modifier = Modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
        Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (active == 0) "Filter" else "Filter · $active aktif")
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (active > 0) Text(keys.mapNotNull { key ->
        values[key]?.takeIf(String::isNotBlank)?.let { value ->
            val option = options.array(lookupFilters[key].orEmpty()).map { it.objectOrEmpty() }.find { it.text("id") == value }
            "${label(key)} · ${option?.text("name") ?: statuses[value] ?: value}"
        }
    }.joinToString("   /   "), style = MaterialTheme.typography.labelMedium)
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading("Filter data")
            keys.forEach { key ->
                val lookupKey = lookupFilters[key]
                val choices = if (key == "status") JsonObject(mapOf("statuses" to JsonArray(listOf("draft", "pending_review", "approved", "rejected").map(::JsonPrimitive)))) else options
                val field = InputField(key, label(key), kind = when {
                    lookupKey != null || key == "status" -> FieldKind.Choice
                    key in listOf("year", "month") -> FieldKind.Integer
                    else -> FieldKind.Text
                }, lookup = lookupKey ?: if (key == "status") "statuses" else null)
                FormField(field, draft[key].orEmpty(), filterErrors[key], choices, draft, enabled = true) {
                    draft = draft + (key to it); filterErrors = filterErrors - key
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    filterErrors = validatePeriodFilters(draft)
                    if (filterErrors.isEmpty()) { apply(draft.filterValues(String::isNotBlank)); expanded = false }
                }) { Text("Terapkan") }
                TextButton(onClick = { draft = emptyMap(); apply(emptyMap()); expanded = false }) { Text("Reset filter") }
            }
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
    var visited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!visited || state.data == JsonNull) model.load(key, path, query)
        visited = true
        if (module.filters.any { it in lookupFilters }) model.loadOptions()
    }
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gutter = ((maxWidth - 720.dp) / 2).coerceAtLeast(16.dp)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = gutter, end = gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("controls") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LandscapeHeader(if (page.path.isBlank()) "CATATAN OPERASIONAL" else "ANTREAN PENINJAUAN",
                        page.title.ifBlank { module.title },
                        if (page.path.isBlank()) "Pilih catatan untuk melihat rincian dan tindakan yang tersedia." else "Buka pengajuan dan periksa rinciannya sebelum mengambil tindakan.")
                    if (module.createPermission != null && user.can(module.createPermission) && page.path.isBlank() && module != Modules.hauling) {
                        Button({ model.openEditor(module); open(Page("editor", module.key)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tambah ${module.title.lowercase()}") }
                    }
                    FilterFields(if (page.path.isBlank()) module.filters else emptyList(), query, options) {
                        filterJson = jsonObject(it).toString()
                        model.load(key, path, it + query.filterKeys { key -> key == "paginate" })
                    }
                    StateHeader(state) { model.load(key, path, query) }
                }
            }
            val rows = state.data as? JsonArray
            items(rows.orEmpty()) { raw ->
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
                item.text("source"),
                ).filter(String::isNotBlank).joinToString(" · ")
                val statusKey = movement?.approvalStatus.orEmpty().ifBlank { item.text("approval_status").ifBlank { item.text("status") } }
                val statusText = statuses[statusKey] ?: item.text("status_label").ifBlank { statusKey }
                val amount = listOf(quantityText, item.text("unit")).filter(String::isNotBlank).joinToString(" ")
                val domainSummary = if (module == Modules.hauling && page.path.isBlank()) {
                    runCatching { HaulingBacklogDto.from(item) }.getOrNull()?.let {
                        "$summary · Sisa ${it.remaining.toPlainString()} · Reservasi ${it.reserved.toPlainString()}"
                    }
                } else null
                val materialTitle = item.obj("waste_type").text("name").ifBlank { statuses[item.text("material_type")].orEmpty() }
                val cardTitle = materialTitle.ifBlank { title }
                val cardSummary = listOfNotNull(title.takeIf { it != cardTitle }, domainSummary ?: summary).joinToString(" · ")
                RecordCard(cardTitle, cardSummary.trim(' ', '·'), amount, statusKey, statusText, icon = when (module.key) {
                    "hauling", "vendors" -> Icons.Default.LocalShipping
                    "production", "adjustments" -> Icons.Default.Factory
                    "utilization" -> Icons.Default.Recycling
                    "users", "profile" -> Icons.Default.Person
                    "roles" -> Icons.Default.AdminPanelSettings
                    "categories" -> Icons.Default.Category
                    "characteristics" -> Icons.Default.HealthAndSafety
                    "periods" -> Icons.Default.CalendarMonth
                    else -> Icons.Default.Inventory2
                }) {
                    val id = if (module == Modules.periods) "${item.text("year")}/${item.text("month")}" else item.text("id")
                    val detailPath = when {
                        module == Modules.hauling && page.path.isBlank() -> "waste-haulings/history/$id"
                        else -> "${targetModule.path}/$id"
                    }
                    open(Page("detail", targetModule.key, id, detailPath, title))
                }
            }
            item("footer") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (rows == null && state.data != JsonNull) FormPanel { JsonDetails(state.data) }
                    val pagination = state.meta.obj("pagination")
                    val current = pagination.text("current_page").toIntOrNull() ?: 1
                    val last = pagination.text("last_page").toIntOrNull() ?: 1
                    if (last > 1) Text("Halaman $current dari $last", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (current < last) OutlinedButton(onClick = { model.load(key, path, query + ("page" to (current + 1).toString()), append = true) }, enabled = !state.loading) { Text("Muat berikutnya") }
                }
            }
        }
    }
}

@Composable
private fun Disclosure(title: String, data: JsonElement) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize(tween(180))) {
        TextButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = if (expanded) "Terbuka" else "Tertutup" }, onClick = { expanded = !expanded }) {
            Text(title, Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        if (expanded) JsonDetails(data)
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
        val transaction = module in listOf(Modules.waste, Modules.hauling, Modules.production, Modules.utilization, Modules.adjustments)
        if (transaction) TransactionSummary(record, module.title) else FormPanel { JsonDetails(raw) }
        if (module == Modules.history) Text("Riwayat aktivitas menampilkan maksimal 100 entri terbaru.")
        if (state.cachedAt != null) InfoPanel(Icons.Default.CloudOff, "Mode baca dari perangkat", "Muat ulang saat tersambung untuk mendapatkan status terbaru dan melakukan tindakan.")
        if (state.cachedAt == null && !state.loading && state.failure == null) {
            val actions = if (module == Modules.profile) setOf("update") else record.actions()
            val available = actions.filter { it !in listOf("view", "review") }
            if (available.isNotEmpty()) SectionHeading("Tindakan tersedia")
            available.forEach { action ->
                val fill = when (action) {
                    "approve", "submit" -> MaterialTheme.colorScheme.primary
                    "reject", "delete" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                Button(shape = MaterialTheme.shapes.small, colors = ButtonDefaults.buttonColors(containerColor = fill,
                    contentColor = contentColorFor(fill)), onClick = {
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
        if (transaction) {
            val grouped = mapOf("hauling_history" to "Riwayat pengangkutan", "history" to "Riwayat transaksi", "attachments" to "Lampiran")
            grouped.forEach { (key, title) ->
                raw[key]?.takeIf { it is JsonArray && it.isNotEmpty() }?.let { Disclosure(title, it) }
            }
            Disclosure("Informasi tambahan", JsonObject(raw.filterKeys { it !in grouped }))
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
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when (action.action) {
                        "delete" -> "Catatan ini akan dihapus dari server. Pastikan catatan yang dipilih sudah benar."
                        "approve" -> "Persetujuan akan dicatat atas nama akun Anda. Periksa jumlah, dokumen, dan pengaju sebelum melanjutkan."
                        "reject" -> "Tuliskan alasan yang jelas agar pengaju dapat memperbaiki catatan ini."
                        "submit" -> "Catatan akan diajukan untuk ditinjau sesuai alur persetujuan organisasi."
                        else -> "Pastikan data sudah ditinjau sebelum melanjutkan."
                    })
                    if (needsReason || action.action == "approve") OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), enabled = !busy,
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
            }) { Text(if (busy) "Memproses..." else actionLabels[action.action] ?: "Konfirmasi") } },
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
    if (current == null) {
        ScrollContent {
            Text("Menyiapkan formulir…")
            TextButton(onClick = model::retryOpenEditor, enabled = !busy) { Text("Coba kembali") }
        }
        return
    }
    val locked = current.draft?.submittedPayload != null
    val external = current.values["movement_type"] == "utilization_external"
    val groups = remember(module, external) {
        module.fields.filter { field -> field.key != "waste_record_id" && isApplicableField(module, field, current.values) }
            .map { effectiveField(module, it, current.values) }.groupBy { field ->
            when (field.key) {
                "source", "vendor_id", "internal_destination_id", "purpose_id" -> "Asal dan tujuan"
                "document_number", "document_date" -> "Dokumen"
                "description", "note", "notes" -> "Keterangan"
                else -> if (module in listOf(Modules.waste, Modules.hauling, Modules.production,
                    Modules.utilization, Modules.adjustments)) "Material dan jumlah" else "Informasi utama"
            }
        }
    }
    val focus = remember(groups) { groups.values.flatten().associate { it.key to FocusRequester() } }
    val attachmentFocus = remember { FocusRequester() }
    var submitted by remember { mutableStateOf(false) }
    var editorViewportHeight by remember { mutableIntStateOf(0) }
    val editorScroll = rememberScrollState()
    val fieldOffsets = remember { mutableMapOf<String, Float>() }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var focusedField by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusedField, editorViewportHeight) {
        focusedField?.let { key ->
            // Scroll within the actual viewport, including adjustResize on older Android versions.
            delay(100)
            fieldOffsets[key]?.let { editorScroll.animateScrollTo(it.toInt().coerceAtLeast(0)) }
        }
    }
    LaunchedEffect(current.errors, submitted) {
        if (submitted && current.errors.isNotEmpty()) {
            val first = focus.keys.firstOrNull { it in current.errors }
            (first?.let { focus[it] } ?: attachmentFocus.takeIf { "attachment" in current.errors })?.requestFocus()
            submitted = false
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f).onSizeChanged { editorViewportHeight = it.height }
            .onGloballyPositioned { viewportTop = it.positionInRoot().y }) {
            ScrollContent(keyboardInsets = false, scrollState = editorScroll) {
                if (locked) InfoPanel(Icons.Default.SyncProblem, "Status pengiriman belum pasti", "Isian dikunci agar tidak terkirim dua kali. Periksa / kirim kembali untuk memeriksa permintaan yang sama.")
                else if (current.draft != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (current.saved) Icons.Default.CheckCircleOutline else Icons.Default.EditNote, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (current.saved) "Draft tersimpan di perangkat" else "Perubahan akan disimpan di perangkat", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                conflict?.let { latest ->
                    Text("Data server terbaru", style = MaterialTheme.typography.titleMedium)
                    Text("Bandingkan dengan isian Anda sebelum memakai versi terbaru sebagai acuan.")
                    JsonDetails(latest)
                    if ("update" in latest.actions()) OutlinedButton(onClick = model::acceptLatestVersion, enabled = !busy) { Text("Gunakan versi terbaru sebagai acuan") }
                    else Text("Status atau izin server sudah tidak mengizinkan perubahan.", color = MaterialTheme.colorScheme.error)
                    HorizontalDivider()
                }
                Text("Kolom bertanda * wajib diisi.", style = MaterialTheme.typography.bodySmall)
                if (current.errors.isNotEmpty()) Surface(shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Text("Periksa ${current.errors.size} isian sebelum menyimpan.", Modifier.fillMaxWidth().padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }
                groups.entries.forEachIndexed { index, (title, fields) ->
                    FormPanel {
                        FormSectionTitle(index + 1, title)
                        fields.forEach { field ->
                            FormField(field, current.values[field.key].orEmpty(), current.errors[field.key], lookup,
                                current.values, enabled = !busy && !locked, modifier = Modifier.focusRequester(focus.getValue(field.key))
                                    .onGloballyPositioned { fieldOffsets[field.key] = it.positionInRoot().y - viewportTop + editorScroll.value }
                                    .onFocusChanged { if (it.hasFocus) focusedField = field.key else if (focusedField == field.key) focusedField = null }) {
                                model.change(field.key, it)
                            }
                        }
                    }
                }
                if (module == Modules.utilization) {
                    SectionHeading(if (external) "Dokumen pendukung *" else "Dokumen pendukung (opsional)")
                    Text(current.draft?.attachment?.let { "Lampiran dipilih · ${File(it).name}" }
                        ?: if (current.existingAttachment) "Lampiran server tersedia" else "Belum ada lampiran")
                    current.errors["attachment"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(modifier = Modifier.focusRequester(attachmentFocus), onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, enabled = !busy && !locked) { Text("Pilih file") }
                        OutlinedButton(onClick = {
                            val file = context.cacheDir.resolve("camera").apply { mkdirs() }.resolve("${UUID.randomUUID()}.jpg")
                            cameraPath = file.path
                            camera.launch(FileProvider.getUriForFile(context, context.packageName + ".files", file))
                        }, enabled = !busy && !locked) { Text("Foto") }
                    }
                    Text("JPG, PNG, WEBP atau PDF · Maksimal 5 MB", style = MaterialTheme.typography.bodySmall)
                }
                if (current.draft != null) TextButton(onClick = { model.leaveEditor(back) }, enabled = !busy) { Text("Lanjutkan nanti") }
            }
        }
        Surface(shadowElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(onClick = { submitted = true; model.submitEditor { back() } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small, enabled = !busy) {
                    Icon(if (locked) Icons.Default.Sync else Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "Menyimpan..." else if (locked) "Periksa / kirim kembali" else "Simpan ke server")
                }
            }
            }
        }
    }
}

@Composable
private fun FormField(field: InputField, value: String, error: String?, lookup: JsonObject, values: Map<String, String>, enabled: Boolean, modifier: Modifier = Modifier, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val fieldModifier = modifier.semantics { if (error != null) error(error) }
    when (field.kind) {
        FieldKind.Date -> {
            var picking by remember { mutableStateOf(false) }
            OutlinedTextField(value, change, fieldModifier.fillMaxWidth(), enabled = enabled, shape = MaterialTheme.shapes.small,
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
        FieldKind.Boolean -> Row(fieldModifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = value == "true", enabled = enabled, role = Role.Switch, onValueChange = { change(it.toString()) }),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(field.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = value == "true", onCheckedChange = null, enabled = enabled)
        }
        FieldKind.Choice -> {
            val options = lookup.array(field.lookup.orEmpty()).map { option ->
                if (option is JsonPrimitive) option.content to (statuses[option.content] ?: label(option.content))
                else option.objectOrEmpty().let { it.text("id") to it.text("name") }
            }
            ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = !expanded }) {
                OutlinedTextField(options.firstOrNull { it.first == value }?.second ?: value, {}, readOnly = true,
                    label = { Text(field.label + if (field.required) " *" else "") }, isError = error != null,
                    enabled = enabled, shape = MaterialTheme.shapes.small, modifier = fieldModifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
                    if (!field.required) DropdownMenuItem(text = { Text("Tidak dipilih") }, onClick = { change(""); expanded = false })
                    if (options.isEmpty()) DropdownMenuItem(text = { Text("Belum ada pilihan tersedia") }, onClick = {}, enabled = false)
                    options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { change(id); expanded = false }) }
                }
            }
        }
        else -> OutlinedTextField(value, change, fieldModifier.fillMaxWidth(), enabled = enabled, shape = MaterialTheme.shapes.small,
            label = { Text(field.label + if (field.required) " *" else "") }, isError = error != null,
            supportingText = if (field.kind == FieldKind.Date) ({ Text("YYYY-MM-DD") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = when (field.kind) {
                FieldKind.Decimal -> KeyboardType.Decimal
                FieldKind.Integer -> KeyboardType.Number
                FieldKind.Password -> KeyboardType.Password
                else -> when (field.key) { "email" -> KeyboardType.Email; "phone" -> KeyboardType.Phone; else -> KeyboardType.Text }
            }),
            trailingIcon = if (field.kind == FieldKind.Password) ({ IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (passwordVisible) "Sembunyikan password" else "Tampilkan password")
            } }) else null,
            visualTransformation = if (field.kind == FieldKind.Password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = field.key !in listOf("note", "notes", "description", "address"))
    }
    error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
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
    val reports = listOf(
            "monthly" to "Bulanan", "yearly" to "Tahunan", "vendors" to "Vendor", "internal-destinations" to "Tujuan internal",
            "purposes" to "Tujuan penggunaan", "stock-card" to "Kartu stok", "anomalies" to "Anomali", "analysis-matrix" to "Matriks analisis",
        )
    var selected by rememberSaveable { mutableStateOf(reports.first().first) }
    var format by rememberSaveable { mutableStateOf("pdf") }
    val chosen = selected.takeIf { user.can("faba_reports.export") }
    val extras = wireJson.parseToJsonElement(extraFilters).jsonObject.mapValues { it.value.jsonPrimitive.content }
    val filters = (extras + mapOf("year" to year, "month" to month, "material_type" to material)).filterValues(String::isNotBlank)
    val validPeriod = year.toIntOrNull() in 2000..2100 && month.toIntOrNull() in 1..12
    var showFilters by rememberSaveable { mutableStateOf(false) }
    ScrollContent {
        LandscapeHeader("PUSAT LAPORAN", "Data untuk keputusan Anda", "Pilih periode dan material, lalu unduh laporan sesuai hak akses akun.")
        if (chosen == null && listOf(Modules.waste, Modules.production, Modules.utilization).none { Modules.visible(it, user) })
            InfoPanel(Icons.Default.Lock, "Akses unduhan belum tersedia", "Hubungi administrator untuk memeriksa hak akses laporan dan ekspor akun Anda.")
        if (chosen != null) {
            FormPanel {
            FormSectionTitle(1, "Jenis laporan")
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Default.Assessment, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(reports.first { it.first == selected }.second, Modifier.weight(1f), textAlign = TextAlign.Start)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    reports.forEach { (key, title) -> DropdownMenuItem(text = { Text(title) },
                        onClick = { selected = key; expanded = false }) }
                }
            }
        }
        }
        FormPanel {
        FormSectionTitle(if (chosen == null) 1 else 2, "Periode laporan")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(year, { year = it }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, singleLine = true, label = { Text("Tahun") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(month, { month = it }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, singleLine = true, label = { Text("Bulan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(year, { year = it }, Modifier.weight(1f), shape = MaterialTheme.shapes.small, singleLine = true, label = { Text("Tahun") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(month, { month = it }, Modifier.weight(1f), shape = MaterialTheme.shapes.small, singleLine = true, label = { Text("Bulan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Material", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("" to "Semua", "fly_ash" to "Fly Ash", "bottom_ash" to "Bottom Ash").forEach { (key, title) ->
                FilterChip(selected = material == key, onClick = { material = key }, label = { Text(title) },
                    leadingIcon = if (material == key) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null)
            }
        }
        TextButton(onClick = { showFilters = !showFilters }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { stateDescription = if (showFilters) "Terbuka" else "Tertutup" }) {
            Icon(Icons.Default.Tune, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Filter tambahan" + if (extras.values.any(String::isNotBlank)) " · aktif" else "")
        }
        if (showFilters) listOf("vendor_id", "internal_destination_id", "purpose_id", "movement_type").forEach { key ->
            FormField(InputField(key, label(key), kind = FieldKind.Choice, lookup = lookupFilters[key]),
                extras[key].orEmpty(), null, options, extras, enabled = !busy) {
                extraFilters = jsonObject(extras + (key to it)).toString()
            }
        }
        if (!validPeriod) Text("Isi tahun 2000–2100 dan bulan 1–12 untuk mengunduh laporan.", color = MaterialTheme.colorScheme.error)
        }
        if (chosen != null) {
            FormPanel {
            FormSectionTitle(3, "Format unduhan")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("pdf","xlsx").forEach { ext -> FilterChip(selected = format == ext,
                    onClick = { format = ext }, label = { Text(ext.uppercase()) },
                    leadingIcon = { Icon(if (ext == "pdf") Icons.Default.PictureAsPdf else Icons.Default.TableChart, null, Modifier.size(18.dp)) }) }
            }
            Button(onClick = { model.download("faba/reports/$chosen.$format", filters,
                "waspro-$chosen-$year-$month.$format") { shareFile(context, it) } }, enabled = !busy && validPeriod,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unduh ${reports.first { it.first == chosen }.second}")
            }
        }
        }
        val exports = listOf(Modules.waste, Modules.production, Modules.utilization).filter { Modules.visible(it, user) }
        if (exports.isNotEmpty()) SectionHeading("Ekspor data CSV")
        exports.forEach { module ->
            FormPanel {
                Text(module.title, style = MaterialTheme.typography.titleMedium)
                val supported = when (module) {
                    Modules.waste -> emptySet()
                    Modules.production -> setOf("year", "month", "material_type", "movement_type")
                    else -> setOf("year", "month", "material_type", "movement_type", "vendor_id")
                }
                val exportFilters = filters.filterKeys { it in supported }
                Text(when (module) {
                    Modules.waste -> "Semua catatan sesuai hak akses. Filter periode dan material di atas tidak berlaku untuk CSV limbah."
                    Modules.production -> "Mengikuti periode, material, dan tipe transaksi."
                    else -> "Mengikuti periode, material, tipe transaksi, dan vendor."
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { model.download("${module.path}/export/csv", exportFilters,
                    "waspro-${module.key}.csv") { shareFile(context, it) } }, enabled = !busy && (module == Modules.waste || validPeriod),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.Default.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("CSV ${module.title}") }
            }
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
                        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            if (maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.3f) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(label(key), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                    Text(displayValue(key, value), style = MaterialTheme.typography.bodyLarge)
                                }
                            } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(label(key), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Text(displayValue(key, value), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
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
    if (key in setOf("year", "month", "period_year", "period_month")) return text
    if (key == "date" || key.endsWith("_date")) {
        return runCatching { LocalDate.parse(text).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))) }.getOrDefault(text)
    }
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
