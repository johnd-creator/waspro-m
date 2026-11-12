package com.ubpbsla.waspro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ubpbsla.waspro.data.UserContext
import com.ubpbsla.waspro.ui.GuideDetail
import com.ubpbsla.waspro.ui.GuideIndex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Pure local guide checks: no login, network, account, or operational write. */
class UserGuideUiTest {
    @get:Rule val compose = createComposeRule()
    private val operator = UserContext(Json.parseToJsonElement("""{"user":{"id":"guide-demo"},"role":{"slug":"operator","name":"Operator"},"permissions":[]}""").jsonObject)

    @Composable private fun TestSurface(content: @Composable () -> Unit) {
        MaterialTheme { Surface(Modifier.fillMaxSize().safeDrawingPadding()) { content() } }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("wasproGuideCapture") != "true") return
        compose.waitForIdle()
        Thread.sleep(500)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /data/local/tmp/waspro-guide-$name.png")
        ).use { it.readBytes() }
    }

    @Test fun roleSearchEmptyResetAndOpenAreReadOnly() {
        var opened = ""
        compose.setContent { TestSurface { GuideIndex(operator) { opened = it } } }
        compose.onNodeWithText("Operator", substring = false).assertIsSelected()
        capture("index")
        compose.onNodeWithText("Cari kasus atau kendala").performTextInput("Tinjau dan putuskan")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Kasus tidak ditemukan"))
        compose.onNodeWithText("Kasus tidak ditemukan").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Reset pencarian dan role"))
        compose.onNodeWithText("Reset pencarian dan role").assertIsDisplayed().performClick()
        compose.onNodeWithText("Semua role").assertIsSelected()
        compose.onNodeWithText("Cari kasus atau kendala").performTextInput("Tinjau dan putuskan")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tinjau dan putuskan pengajuan"))
        compose.onNodeWithText("Tinjau dan putuskan pengajuan").performClick()
        assertEquals("review", opened)
    }

    @Test fun detailScreenshotCloseAndBackKeepTheCase() {
        compose.setContent { TestSurface { GuideDetail("review") } }
        compose.onNodeWithText("Tinjau dan putuskan pengajuan").assertIsDisplayed()
        capture("case")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Buka screenshot"))
        compose.onNodeWithText("Buka screenshot").performClick()
        compose.onNodeWithText("Screenshot DEMO").assertIsDisplayed()
        capture("image")
        compose.onNodeWithText("Tutup screenshot").performClick()
        compose.onNodeWithText("Screenshot DEMO").assertDoesNotExist()
        compose.onNodeWithText("Buka screenshot").performClick()
        compose.onNodeWithText("Screenshot DEMO").assertIsDisplayed()
        Thread.sleep(500) // Wait for the platform Dialog window to receive input focus.
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("Screenshot DEMO").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hasil yang diharapkan"))
        compose.onNodeWithText("Hasil yang diharapkan").assertIsDisplayed()
    }

    @Test fun unknownCaseHasRecoverableMessage() {
        compose.setContent { TestSurface { GuideDetail("not-a-case") } }
        compose.onNodeWithText("Panduan tidak ditemukan. Kembali ke daftar kasus.").assertIsDisplayed()
    }
}