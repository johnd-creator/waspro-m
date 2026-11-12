package com.ubpbsla.waspro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** End-to-end entry/navigation against the loopback read-only DEMO fixture. */
class UserGuideNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitFor(text: String) = compose.waitUntil(30_000) {
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(500)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /data/local/tmp/waspro-mobile-guide-$name.png")
        ).use { it.readBytes() }
    }
    @Test fun guideEntryCaseBackAndTabNavigation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproVisualFixture") == "true")
        assumeTrue(BuildConfig.FLAVOR == "development" && android.net.Uri.parse(BuildConfig.API_BASE_URL).host == "127.0.0.1")
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Masuk ke akun Anda") or hasText("Halo, Aditya")).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Masuk ke akun Anda").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Email").performScrollTo().performTextInput("aditya@example.test")
            compose.onNodeWithText("Password").performScrollTo().performTextInput("demo")
            compose.onNodeWithText("Password").performImeAction()
        }
        waitFor("Halo, Aditya")
        compose.onNode(hasText("Lainnya") and isSelectable()).performClick()
        waitFor("Panduan penggunaan")
        compose.onNode(hasText("Panduan penggunaan") and hasClickAction()).performScrollTo().performClick()
        waitFor("Langkah sesuai pekerjaan Anda")
        capture("index")
        compose.onNodeWithText("Cari kasus atau kendala").performTextInput("Tinjau dan putuskan")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tinjau dan putuskan pengajuan"))
        compose.onNodeWithText("Tinjau dan putuskan pengajuan").performClick()
        waitFor("Sebelum mulai")
        capture("case")
        compose.onNodeWithContentDescription("Kembali").performClick()
        waitFor("Langkah sesuai pekerjaan Anda")
        compose.onNodeWithText("Cari kasus atau kendala").assertTextContains("Tinjau dan putuskan")
        compose.onNodeWithContentDescription("Kembali").performClick()
        waitFor("Panduan penggunaan")
        compose.onNode(hasText("Beranda") and isSelectable()).performClick()
        waitFor("Halo, Aditya")
    }
}