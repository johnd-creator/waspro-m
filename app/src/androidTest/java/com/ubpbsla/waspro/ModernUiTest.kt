package com.ubpbsla.waspro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.ubpbsla.waspro.features.WasproViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Exercises the actual app against tools/ui_fixture.py, never a production account. */
class ModernUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tap(text: String, tab: Boolean = false) {
        val visibleText = if (tab && text == "Persetujuan") "Tinjau" else text
        waitFor(visibleText)
        val matcher = hasText(visibleText) and hasClickAction() and (if (tab) isSelectable() else !isSelectable())
        val node = compose.onNode(matcher)
        if (!node.isDisplayed()) {
            if (compose.onAllNodes(matcher and hasAnyAncestor(hasScrollToIndexAction())).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
            } else node.performScrollTo()
        }
        node.assertIsEnabled().performClick()
    }

    private fun shell(command: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }

    private fun capture(name: String) {
        val variant = InstrumentationRegistry.getArguments().getString("wasproCapture") ?: return
        require(variant.matches(Regex("[a-z0-9-]+")))
        compose.waitForIdle()
        // Wait for system navigation and keyboard animations, outside Compose's clock.
        Thread.sleep(600)
        shell("screencap -p /data/local/tmp/waspro-$variant-$name.png")
    }

    private fun cleanOwnFixtureDrafts() {
        val model = compose.runOnIdle { ViewModelProvider(compose.activity)[WasproViewModel::class.java] }
        check(model.repository.scope == "development|ui-demo|ui-demo")
        runBlocking {
            val dao = model.repository.database.dao()
            dao.drafts(model.repository.scope).first()
                .filter { it.payload.contains("Uji tampilan lokal") && it.attachment == null }
                .forEach { dao.deleteDraft(it.scope, it.id) }
        }
        compose.waitForIdle()
    }

    @Test fun dailyNavigationFormAndScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproVisualFixture") == "true")
        assumeTrue(BuildConfig.FLAVOR == "development" && android.net.Uri.parse(BuildConfig.API_BASE_URL).host == "127.0.0.1")
        if (InstrumentationRegistry.getArguments().getString("wasproFreshLogin") == "true") {
            val model = compose.runOnIdle { ViewModelProvider(compose.activity)[WasproViewModel::class.java] }
            compose.waitUntil(30_000) { !model.starting.value }
            if (model.user.value != null) {
                check(model.repository.scope == "development|ui-demo|ui-demo")
                check(runBlocking { model.repository.database.dao().drafts(model.repository.scope).first().isEmpty() }) {
                    "Fresh-login check must not delete any existing draft."
                }
                compose.runOnIdle { model.logout() }
                compose.waitUntil(30_000) { !model.busy.value && model.user.value == null }
            }
        }
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText("Masuk ke akun Anda") or hasText("Halo, Aditya")).fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Masuk ke akun Anda").fetchSemanticsNodes().isNotEmpty()) {
            capture("login")
            compose.onNodeWithText("Email").performScrollTo().performTextInput("aditya@example.test")
            compose.onNodeWithText("Email").performImeAction()
            compose.onNodeWithText("Password").performTextInput("demo")
            compose.onNodeWithText("Password").performImeAction()
        }
        try {
            waitFor("Halo, Aditya")
            waitFor("128")
        } catch (failure: Throwable) {
            capture("startup-failure")
            throw failure
        }
        cleanOwnFixtureDrafts()
        capture("home")
        tap("Limbah", tab = true)
        capture("waste-menu")
        tap("Catatan limbah")
        waitFor("Oli pelumas bekas")
        capture("records")
        tap("Oli pelumas bekas")
        waitFor("Rincian catatan")
        capture("detail")
        compose.onNodeWithContentDescription("Kembali").performClick()
        waitFor("Tambah catatan limbah")
        tap("Tambah catatan limbah")
        waitFor("Jenis limbah *")
        capture("form")
        tap("Simpan ke server")
        waitFor("Jenis limbah wajib diisi")
        compose.onNodeWithText("Jenis limbah *").performScrollTo().performClick()
        compose.onNodeWithText("Oli pelumas bekas", substring = false).performClick()
        compose.onNodeWithText("Jumlah *").performScrollTo().performTextInput("12,50")
        compose.onNodeWithText("Satuan *").performScrollTo().performTextInput("kg")
        compose.onNodeWithText("Sumber / lokasi").performScrollTo().performClick().performTextInput("Uji tampilan lokal")
        Thread.sleep(600)
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("Simpan ke server").assertIsDisplayed()
        capture("keyboard")
        val inputBounds = compose.onNodeWithText("Sumber / lokasi").getUnclippedBoundsInRoot()
        val saveBounds = compose.onNodeWithText("Simpan ke server").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("Focused field $inputBounds must fit above save action $saveBounds", inputBounds.bottom <= saveBounds.top)
        shell("input keyevent 4")
        compose.waitForIdle()
        tap("Lanjutkan nanti")
        tap("Lainnya", tab = true)
        tap("Draft lokal")
        waitFor("Lanjutkan")
        capture("drafts")
        compose.onAllNodes(hasText("Lanjutkan") and hasClickAction()).onFirst().performScrollTo().performClick()
        // Opening a persisted draft loads lookups asynchronously before the form is composed.
        waitFor("Sumber / lokasi")
        compose.onNodeWithText("Sumber / lokasi").performScrollTo().assertTextContains("Uji tampilan lokal")
        tap("Lanjutkan nanti")
        tap("FABA", tab = true)
        capture("faba-menu")
        tap("Dashboard FABA")
        waitFor("2.480,75 ton")
        capture("faba")
        tap("Persetujuan", tab = true)
        capture("approvals")
        tap("Lainnya", tab = true)
        capture("account")
        tap("Laporan")
        waitFor("Periode laporan")
        capture("reports")
        repeat(3) {
            tap("Beranda", tab = true)
            tap("Limbah", tab = true)
        }
        tap("Beranda", tab = true)
        compose.onNodeWithText("Halo, Aditya").assertExists()
        cleanOwnFixtureDrafts()
    }
}
