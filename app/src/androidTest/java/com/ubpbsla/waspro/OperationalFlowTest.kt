package com.ubpbsla.waspro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class OperationalFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tap(text: String) {
        waitFor(text)
        val node = compose.onNode(hasText(text) and hasClickAction())
        if (!node.isDisplayed()) node.performScrollTo()
        compose.waitForIdle()
        node.assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { click -> click() }
    }
    private fun waitFor(text: String) {
        try {
            compose.waitUntil(30_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            val tree = runCatching { compose.onRoot().printToString() }.getOrDefault("Tree unavailable")
            android.util.Log.e("WASPRO_FLOW", "Tidak menemukan $text\n$tree")
            throw AssertionError("Tidak menemukan $text\n$tree", failure)
        }
    }
    private fun login(name: String) {
        waitFor("Masuk ke akun Anda")
        compose.onNodeWithText("Email").performTextInput("$name@mobile.test")
        compose.onNodeWithText("Password").performTextInput("mobile-fixture-only")
        tap("Masuk")
        waitFor("Halo, $name")
    }
    @Test fun operatorCreatesAndSubmitsWasteThenSupervisorApproves() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproFixture") == "true")
        val source = "Uji perangkat ${System.currentTimeMillis()}"
        login("operator")
        tap("Limbah")
        tap("Catatan limbah")
        tap("Tambah catatan limbah")
        waitFor("Jenis limbah")
        compose.onNodeWithText("Jenis limbah *").performClick()
        compose.onNodeWithText("Limbah Uji Mobile").performClick()
        compose.onNodeWithText("Jumlah *").performTextInput("12,50")
        compose.onNodeWithText("Satuan *").performTextInput("kg")
        compose.onNodeWithText("Sumber / lokasi").performScrollTo().performTextInput(source)
        tap("Simpan ke server")
        waitFor("Data berhasil disimpan.")
        waitFor(source)
        compose.onNode(hasText(source, substring = true) and hasClickAction()).performClick()
        waitFor("Ajukan")
        tap("Ajukan")
        tap("Konfirmasi")
        waitFor("Tindakan berhasil diproses.")
        tap("Lainnya")
        tap("Keluar akun")
        tap("Keluar")
        login("supervisor-a")
        tap("Persetujuan")
        tap("Persetujuan limbah")
        waitFor(source)
        compose.onNode(hasText(source, substring = true) and hasClickAction()).performClick()
        waitFor("Setujui")
        tap("Setujui")
        tap("Konfirmasi")
        waitFor("Disetujui")
    }
}
