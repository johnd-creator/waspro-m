package com.ubpbsla.waspro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import androidx.lifecycle.ViewModelProvider
import com.ubpbsla.waspro.features.WasproViewModel
import com.ubpbsla.waspro.data.jsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class OperationalFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun capture(screen: String) {
        val stage = InstrumentationRegistry.getArguments().getString("wasproScreenshots") ?: return
        require(stage in setOf("before", "after"))
        compose.waitForIdle()
        val command = "screencap -p /data/local/tmp/waspro-$stage-$screen.png"
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
    private fun tap(text: String) {
        val visibleText = if (text == "Persetujuan") "Tinjau" else text
        waitFor(visibleText)
        val matcher = hasText(visibleText) and hasClickAction()
        val node = compose.onNode(if (text in setOf("Limbah", "Lainnya", "Persetujuan")) matcher and isSelectable() else matcher)
        if (!node.isDisplayed()) {
            if (compose.onAllNodes(matcher and hasAnyAncestor(hasScrollToIndexAction())).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
            } else node.performScrollTo()
        }
        compose.waitForIdle()
        node.assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { click -> click() }
    }
    private fun waitFor(text: String) {
        try {
            compose.waitUntil(30_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            throw AssertionError("Tidak menemukan $text", failure)
        }
    }
    private fun login(name: String) {
        waitFor("Masuk ke akun Anda")
        compose.onNodeWithText("Email").performScrollTo().performTextInput("$name@mobile.test")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("mobile-fixture-only")
        tap("Masuk")
        waitFor("Halo, $name")
    }
    @Test fun operatorCreatesAndSubmitsWasteThenSupervisorApproves() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproFixture") == "true")
        assumeTrue(BuildConfig.FLAVOR == "development" && android.net.Uri.parse(BuildConfig.API_BASE_URL).host in setOf("127.0.0.1", "10.0.2.2"))
        val source = "Uji perangkat ${System.currentTimeMillis()}"
        waitFor("Masuk ke akun Anda")
        capture("login")
        login("operator")
        waitFor("Diperbarui")
        capture("home")
        tap("Limbah")
        tap("Catatan limbah")
        waitFor("Diperbarui")
        capture("list")
        tap("Filter")
        compose.onNodeWithText("Cari nomor / jenis / sumber").performTextInput("Tidak ada $source")
        tap("Terapkan")
        waitFor("Belum ada data.")
        tap("Filter · 1 aktif")
        tap("Reset filter")
        waitFor("Diperbarui")
        tap("Tambah catatan limbah")
        waitFor("Jenis limbah *")
        capture("form")
        tap("Simpan ke server")
        waitFor("Jenis limbah wajib diisi")
        compose.onNodeWithText("Jenis limbah *").performClick()
        compose.onNodeWithText("Limbah Uji Mobile").performClick()
        compose.onNodeWithText("Jumlah *").performTextInput("12,50")
        compose.onNodeWithText("Satuan *").performTextInput("kg")
        compose.onNodeWithText("Sumber / lokasi").performScrollTo().performTextInput(source)
        compose.onNodeWithText("Simpan ke server").assertIsDisplayed()
        tap("Simpan ke server")
        waitFor("Data berhasil disimpan.")
        waitFor("Diperbarui")
        val recordMatcher = hasText(source, substring = true) and hasClickAction()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(recordMatcher)
        compose.onNode(recordMatcher).performSemanticsAction(SemanticsActions.OnClick) { it() }
        waitFor("Ajukan")
        tap("Edit")
        waitFor("Sumber / lokasi")
        // Simulate another session updating only the record this test just created.
        val model = compose.runOnIdle { ViewModelProvider(compose.activity)[WasproViewModel::class.java] }
        val editor = requireNotNull(model.editor.value)
        check(editor.values["source"] == source)
        val draft = requireNotNull(editor.draft)
        val path = "waste-records/${requireNotNull(draft.recordId)}"
        runBlocking {
            val fields = editor.values.filterKeys { !it.startsWith("_") } + mapOf(
                "notes" to "Pemeriksaan versi fixture", "_source_version" to requireNotNull(draft.sourceVersion))
            model.repository.mutate(path, "PUT", jsonObject(fields), java.util.UUID.randomUUID().toString())
        }
        tap("Simpan ke server")
        waitFor("Data server terbaru")
        tap("Gunakan versi terbaru sebagai acuan")
        compose.waitUntil(10_000) { model.conflict.value == null && !model.busy.value }
        tap("Simpan ke server")
        waitFor("Ajukan")
        tap("Ajukan")
        tap("Konfirmasi")
        waitFor("Tindakan berhasil diproses.")
        tap("Lainnya")
        tap("Keluar akun")
        tap("Keluar")
        login("supervisor-a")
        tap("Persetujuan")
        capture("approval")
        tap("Persetujuan limbah")
        waitFor("Diperbarui")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(recordMatcher)
        compose.onNode(recordMatcher).performSemanticsAction(SemanticsActions.OnClick) { it() }
        waitFor("Setujui")
        capture("detail")
        tap("Setujui")
        tap("Konfirmasi")
        waitFor("Disetujui")
        tap("Lainnya")
        tap("Laporan")
        waitFor("Periode laporan")
        capture("reports")
    }
}
