package com.ubpbsla.waspro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.ubpbsla.waspro.features.WasproViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Opt-in synthetic screen tour. Never submits a valid form or modifies remote data. */
class AllModulesUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) = compose.waitUntil(30_000) {
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tap(text: String, tab: Boolean = false) {
        waitFor(text)
        val matcher = hasText(text) and hasClickAction() and
            (if (tab) isSelectable() else !isSelectable())
        val node = compose.onNode(matcher)
        if (!node.isDisplayed()) {
            if (compose.onAllNodes(matcher and hasAnyAncestor(hasScrollToIndexAction())).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
            } else node.performScrollTo()
        }
        node.assertIsEnabled().performClick()
    }

    private fun back() {
        compose.onNodeWithContentDescription("Kembali").assertIsDisplayed().performClick()
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val variant = InstrumentationRegistry.getArguments().getString("wasproCapture") ?: return
        require(variant.matches(Regex("[a-z0-9-]+")))
        require(name.matches(Regex("[a-z0-9-]+")))
        compose.waitForIdle()
        Thread.sleep(600)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "screencap -p /data/local/tmp/waspro-$variant-tour-$name.png")
        ).use { it.readBytes() }
    }

    @Before fun onlyLoopbackFixture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproVisualFixture") == "true")
        val uri = android.net.Uri.parse(BuildConfig.API_BASE_URL)
        assumeTrue(BuildConfig.FLAVOR == "development" && uri.host == "127.0.0.1" && uri.port == 8798)
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText("Masuk ke akun Anda") or hasText("Halo, Aditya")).fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Masuk ke akun Anda").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Email").performScrollTo().performTextInput("aditya@example.test")
            compose.onNodeWithText("Email").performImeAction()
            compose.onNodeWithText("Password").performTextInput("demo")
            compose.onNodeWithText("Password").performImeAction()
        }
        waitFor("128")
    }

    private fun open(tab: String, module: String) {
        tap(tab, tab = true)
        tap(module)
    }

    private fun form(title: String, firstField: String, lastField: String, shot: String) {
        tap("Tambah ${title.lowercase()}")
        waitFor(firstField)
        compose.onNodeWithText(firstField).performScrollTo().assertIsDisplayed()
        capture("$shot-form-top")
        compose.onNodeWithText(lastField).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Simpan ke server").assertIsDisplayed()
        capture("$shot-form-bottom")
        // No edits: avoid creating persistent local drafts while surveying layouts.
        back()
        waitFor("Tambah ${title.lowercase()}")
    }

    @Test fun fabaTransactionsFormsPeriodsAndRecaps() {
        listOf(
            listOf("Produksi FABA", "production", "Tipe entri *"),
            listOf("Pemanfaatan FABA", "utilization", "Tujuan penggunaan"),
            listOf("Penyesuaian FABA", "adjustments", "Alasan penyesuaian *")
        ).forEach { (title, key, last) ->
            open("FABA", title)
            waitFor("DEMO-${key.uppercase()}-001")
            capture(key)
            tap("Fly Ash")
            waitFor("Rincian catatan")
            capture("$key-detail")
            back()
            form(title, "Tanggal *", last, key)
        }
        open("FABA", "Persetujuan bulanan")
        waitFor("DEMO November 2026")
        capture("periods")
        tap("DEMO November 2026")
        waitFor("Rincian catatan")
        capture("period-detail")
        back()
        listOf("Rekap bulanan", "Rekap tahunan", "Rekap vendor", "Saldo FABA", "Kartu stok", "Riwayat FABA").forEachIndexed { i, title ->
            open("FABA", title)
            waitFor(if (title == "Riwayat FABA") "DEMO Riwayat persetujuan" else "DEMO November 2026")
            capture("recap-$i")
        }
        listOf("Saldo awal" to "Jumlah *", "Kapasitas TPS" to "Ambang critical (%) *").forEachIndexed { i, (title, field) ->
            open("FABA", title)
            waitFor(field)
            compose.onNodeWithText(field).performScrollTo().assertIsDisplayed()
            capture("settings-form-$i")
            back()
        }
    }

    @Test fun masterDataAccountAndHaulingTour() {
        listOf(
            listOf("Kategori limbah", "Kategori", "Deskripsi"),
            listOf("Karakteristik limbah", "Karakteristik", "Berbahaya"),
            listOf("Jenis limbah", "Jenis", "Biaya pengangkutan *"),
            listOf("Vendor", "Vendor", "Berlaku sampai")
        ).forEachIndexed { i, (title, row, last) ->
            open("Lainnya", title)
            waitFor("DEMO $row")
            capture("master-$i")
            tap("DEMO $row")
            waitFor("Rincian catatan")
            capture("master-$i-detail")
            back()
            form(title, "Nama *", last, "master-$i")
        }
        listOf("Pengguna" to "DEMO Operator", "Role dan izin" to "DEMO Supervisor").forEachIndexed { i, (title, row) ->
            open("Lainnya", title)
            waitFor(row)
            capture("access-$i")
            tap(row)
            waitFor("Rincian catatan")
            capture("access-$i-detail")
            back()
        }
        open("Lainnya", "Profil")
        waitFor("Informasi akun")
        capture("profile")
        tap("Edit")
        waitFor("Nama *")
        capture("profile-form")
        back()
        open("Lainnya", "Ganti password")
        waitFor("Password saat ini *")
        compose.onNodeWithText("Konfirmasi password baru *").performScrollTo().assertIsDisplayed()
        capture("password-form")
        back()
        tap("Keluar akun")
        waitFor("Keluar akun?")
        tap("Batal")
        compose.onNodeWithText("Keluar akun?").assertDoesNotExist()
        open("Limbah", "Pengangkutan")
        waitFor("Oli pelumas bekas")
        capture("hauling-backlog")
        tap("Oli pelumas bekas")
        waitFor("DEMO-ANGKUT-001")
        capture("hauling-history")
        tap("Ajukan pengangkutan")
        waitFor("Tanggal angkut *")
        compose.onNodeWithText("Catatan").performScrollTo().assertIsDisplayed()
        capture("hauling-form")
        back()
        tap("DEMO-ANGKUT-001")
        waitFor("Rincian catatan")
        capture("hauling-detail")
    }

    @Test fun approvalQueuesReportsAndAbout() {
        listOf(
            "Persetujuan limbah" to "Oli pelumas bekas",
            "Persetujuan pengangkutan" to "DEMO-ANGKUT-001",
            "Transaksi FABA" to "DEMO-PRODUCTION-001",
            "Periode FABA" to "DEMO November 2026"
        ).forEachIndexed { i, (title, row) ->
            open("Tinjau", title)
            waitFor(row)
            capture("approval-queue-$i")
            back()
        }
        open("Lainnya", "Laporan")
        waitFor("Periode laporan")
        tap("Filter tambahan")
        compose.onNodeWithText("Vendor").performScrollTo().assertIsDisplayed()
        capture("report-filters")
        back()
        tap("Tentang aplikasi")
        capture("about")
        back()
    }

    @Test fun reportTypesFormatsAndPeriodValidation() {
        open("Lainnya", "Laporan")
        waitFor("Jenis laporan")
        var selected = "Bulanan"
        listOf("Bulanan", "Tahunan", "Vendor", "Tujuan internal", "Tujuan penggunaan",
            "Kartu stok", "Anomali", "Matriks analisis").forEach { title ->
            tap(selected)
            // Scope to the popup: the currently selected title also exists on the trigger.
            compose.onNode(hasText(title) and hasClickAction() and hasAnyAncestor(isPopup()))
                .performScrollTo().performClick()
            selected = title
            compose.onNodeWithText("Unduh $title").performScrollTo().assertIsEnabled()
        }
        compose.onNodeWithText("XLSX").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("PDF").assertIsNotSelected()
        capture("reports-xlsx")
        compose.onNodeWithText("PDF").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("XLSX").assertIsNotSelected()
        capture("reports-pdf")
        compose.onNodeWithText("Bulan").performScrollTo().performTextReplacement("13")
        compose.onNodeWithText("Unduh Matriks analisis").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Isi tahun 2000\u20132100 dan bulan 1\u201312 untuk mengunduh laporan.").assertExists()
        capture("reports-invalid-period")
        compose.onNodeWithText("Bulan").performScrollTo().performTextReplacement("11")
        compose.onNodeWithText("Unduh Matriks analisis").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Isi tahun 2000\u20132100 dan bulan 1\u201312 untuk mengunduh laporan.").assertDoesNotExist()
        // Deliberately never invoke download/export: this tour does not share files or fetch reports.
        back()
    }

    @Test fun externalUtilizationFieldsAndSwitchValidation() {
        open("FABA", "Pemanfaatan FABA")
        waitFor("DEMO-UTILIZATION-001")
        tap("Tambah pemanfaatan faba")
        waitFor("Jenis pemanfaatan *")
        val model = compose.runOnIdle { ViewModelProvider(compose.activity)[WasproViewModel::class.java] }
        check(model.repository.scope == "development|ui-demo|ui-demo")
        val ownedDraft = requireNotNull(model.editor.value?.draft)
        try {
            compose.onNodeWithText("Jenis pemanfaatan *").performScrollTo().performClick()
            compose.onNodeWithText("Pemanfaatan eksternal").performClick()
            listOf("Vendor *", "Nomor dokumen *", "Tanggal dokumen *", "Dokumen pendukung *").forEach {
                compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText("Simpan ke server").assertIsDisplayed()
            capture("utilization-external-documents")
            compose.onNodeWithText("Tanggal dokumen *").performScrollTo().performTextInput("invalid-date")
            compose.onNodeWithText("Jenis pemanfaatan *").performScrollTo().performClick()
            compose.onNodeWithText("Pemanfaatan internal").performClick()
            listOf("Vendor *", "Nomor dokumen *", "Tanggal dokumen *", "Dokumen pendukung *").forEach {
                compose.onNodeWithText(it).assertDoesNotExist()
            }
            compose.onNodeWithText("Tujuan internal *").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Dokumen pendukung (opsional)").performScrollTo().assertIsDisplayed()
            capture("utilization-internal-switched")
            // Material, quantity and internal destination stay blank: validation only, no valid write.
            tap("Simpan ke server")
            compose.waitUntil(30_000) { !model.busy.value && model.editor.value?.errors?.isNotEmpty() == true }
            val errors = requireNotNull(model.editor.value).errors
            assertTrue("Visible required material must fail validation", "material_type" in errors)
            assertTrue("Visible required quantity must fail validation", "quantity" in errors)
            assertTrue("Visible internal destination must fail validation", "internal_destination_id" in errors)
            listOf("document_date", "document_number", "vendor_id", "attachment").forEach {
                assertFalse("Hidden/optional external field must not block internal use: $it", it in errors)
            }
            compose.onNodeWithText("Gunakan tanggal YYYY-MM-DD.").assertDoesNotExist()
            back()
            waitFor("Tambah pemanfaatan faba")
        } finally {
            // Delete exactly this test's local draft; never touch unrelated drafts or attachments.
            if (model.editor.value?.draft?.id == ownedDraft.id) {
                compose.runOnIdle { model.leaveEditor {} }
                compose.waitUntil(30_000) { !model.busy.value }
            }
            compose.runOnIdle { model.removeDraft(ownedDraft) }
            compose.waitUntil(30_000) { !model.busy.value }
        }
    }

    @Test fun wasteReviewConfirmationCancelPreservesRecord() {
        open("Tinjau", "Persetujuan limbah")
        waitFor("Oli pelumas bekas")
        tap("Oli pelumas bekas")
        waitFor("Rincian catatan")
        fun assertPendingRecord() {
            compose.onAllNodesWithText("DEMO-LMB/2026/001").onLast().assertExists()
            compose.onNodeWithText("Oli pelumas bekas").assertExists()
            compose.onNodeWithText("Menunggu persetujuan").assertExists()
            compose.onNodeWithText("Rincian catatan").assertExists()
        }
        assertPendingRecord()
        tap("Tolak")
        waitFor("Tolak?")
        val rejectConfirm = hasText("Tolak") and hasClickAction() and hasAnyAncestor(isDialog())
        compose.onNode(rejectConfirm).assertIsNotEnabled()
        compose.onNodeWithText("Alasan wajib").performTextInput("DEMO - hanya uji pembatalan")
        compose.onNode(rejectConfirm).assertIsEnabled()
        capture("reject-dialog")
        tap("Batal")
        compose.onNodeWithText("Tolak?").assertDoesNotExist()
        assertPendingRecord()
        tap("Setujui")
        waitFor("Setujui?")
        compose.onNodeWithText("Catatan (opsional)").assertExists()
        compose.onNode(hasText("Setujui") and hasClickAction() and hasAnyAncestor(isDialog())).assertIsEnabled()
        capture("approve-dialog")
        tap("Batal")
        compose.onNodeWithText("Setujui?").assertDoesNotExist()
        assertPendingRecord()
        // Reopening also verifies the abandoned rejection reason was not retained.
        tap("Tolak")
        waitFor("Tolak?")
        compose.onNode(rejectConfirm).assertIsNotEnabled()
        tap("Batal")
        assertPendingRecord()
        back()
        waitFor("Oli pelumas bekas")
    }

    @Test fun wasteFiltersEmptyErrorResetAndDateCancel() {
        open("Limbah", "Catatan limbah")
        waitFor("Oli pelumas bekas")
        tap("Filter")
        compose.onNodeWithText("Cari nomor / jenis / sumber").performTextInput("DEMO-EMPTY")
        tap("Terapkan")
        waitFor("Belum ada data.")
        capture("empty")
        compose.onNode(hasText("Filter \u00b7 1 aktif") and hasClickAction()).performClick()
        tap("Reset filter")
        waitFor("Oli pelumas bekas")
        tap("Filter")
        compose.onNodeWithText("Cari nomor / jenis / sumber").performTextInput("DEMO-ERROR")
        tap("Terapkan")
        waitFor("DEMO: simulasi layanan tidak tersedia.")
        capture("error")
        compose.onNode(hasText("Filter \u00b7 1 aktif") and hasClickAction()).performClick()
        tap("Reset filter")
        waitFor("Oli pelumas bekas")
        tap("Tambah catatan limbah")
        waitFor("Tanggal *")
        compose.onNodeWithContentDescription("Pilih tanggal").performScrollTo().performClick()
        waitFor("Batal")
        capture("date-picker")
        tap("Batal")
        compose.onNodeWithText("Jenis limbah *").assertExists()
        back()
        waitFor("Tambah catatan limbah")
    }
}
