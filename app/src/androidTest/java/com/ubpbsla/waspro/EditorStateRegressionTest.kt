package com.ubpbsla.waspro

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ubpbsla.waspro.data.Draft
import com.ubpbsla.waspro.data.text
import com.ubpbsla.waspro.data.wireJson
import com.ubpbsla.waspro.features.Modules
import com.ubpbsla.waspro.features.WasproViewModel
import com.ubpbsla.waspro.ui.Page
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/** Local draft/file operations only. Login uses the synthetic loopback fixture. */
class EditorStateRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: WasproViewModel
    private val owned = mutableListOf<Draft>()
    private val sourceFiles = mutableListOf<File>()

    @Before fun connectFixture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wasproVisualFixture") == "true")
        val uri = android.net.Uri.parse(BuildConfig.API_BASE_URL)
        assumeTrue(BuildConfig.FLAVOR == "development" && uri.host == "127.0.0.1" && uri.port == 8798)
        model = compose.runOnIdle { ViewModelProvider(compose.activity)[WasproViewModel::class.java] }
        compose.waitUntil(30_000) { !model.starting.value }
        if (model.user.value == null) {
            compose.runOnIdle { model.login("aditya@example.test", "demo") }
            idle()
        }
        check(model.repository.scope == "development|ui-demo|ui-demo")
    }

    private fun idle() { compose.waitUntil(30_000) { !model.busy.value }; compose.waitForIdle() }
    private fun newDraft(): Draft {
        compose.runOnIdle { model.openEditor(Modules.categories) }
        idle()
        return requireNotNull(model.editor.value?.draft).also { owned += it }
    }
    private fun stored(draft: Draft): Draft? = runBlocking {
        model.repository.database.dao().draft(draft.scope, draft.id)
    }
    private fun leave() {
        compose.runOnIdle { model.leaveEditor {} }
        idle()
        assertNull(model.editor.value)
    }

    @After fun cleanOnlyTestDrafts() {
        if (!::model.isInitialized) return
        check(model.repository.scope == "development|ui-demo|ui-demo")
        owned.forEach { original ->
            val draft = stored(original) ?: original
            compose.runOnIdle { model.removeDraft(draft) }
            idle()
        }
        sourceFiles.forEach { it.delete() }
    }

    @Test fun freshIdentityPublicationDoesNotInvalidateHomeLoad() {
        // Local test-only identity reset avoids logout, which would erase other drafts.
        // All network requests remain restricted by connectFixture to the synthetic host.
        compose.runOnIdle { model.repository.identity.value = null }
        compose.waitForIdle()
        compose.runOnIdle { model.login("aditya@example.test", "demo") }
        idle()
        val home = Page("home").toString()
        compose.waitUntil(30_000) {
            model.screens.value[home]?.let { !it.loading && it.data != JsonNull } == true
        }
        assertNull(model.screens.value[home]?.failure)
    }

    @Test fun closingUntouchedNewFormDoesNotCreateDraft() {
        val draft = newDraft()
        leave()
        assertNull(stored(draft))
    }

    @Test fun immediateLeaveFlushesLatestEditAndReopensIt() {
        val draft = newDraft()
        compose.runOnIdle {
            model.change("name", "Rapid edit regression")
            model.leaveEditor {}
        }
        idle()
        val persisted = requireNotNull(stored(draft))
        assertEquals("Rapid edit regression", wireJson.parseToJsonElement(persisted.payload).jsonObject.text("name"))
        // Deliberately pass the stale original: reopening must prefer the stored snapshot.
        compose.runOnIdle { model.openEditor(Modules.categories, savedDraft = draft) }
        idle()
        assertEquals("Rapid edit regression", model.editor.value?.values?.get("name"))
        leave()
    }

    @Test fun leavingLockedDraftPreservesRetryIdentityAndPayload() {
        val draft = newDraft()
        val locked = draft.copy(submittedPayload = "{\"name\":\"Frozen\"}", submittedAttachmentHash = "fixture-hash")
        runBlocking { model.repository.database.dao().save(locked) }
        compose.runOnIdle {
            model.editor.value = requireNotNull(model.editor.value).copy(draft = locked)
            model.change("name", "Must not replace frozen input")
            model.leaveEditor {}
        }
        idle()
        compose.runOnIdle { model.openEditor(Modules.categories, savedDraft = locked) }
        idle()
        val reopened = requireNotNull(model.editor.value?.draft)
        assertEquals(locked.requestKey, reopened.requestKey)
        assertEquals(locked.submittedPayload, reopened.submittedPayload)
        assertEquals(locked.submittedAttachmentHash, reopened.submittedAttachmentHash)
        assertNotEquals("Must not replace frozen input", model.editor.value?.values?.get("name"))
        leave()
    }

    @Test fun deletingDraftCancelsDebouncedSaveWithoutResurrection() {
        val draft = newDraft()
        compose.runOnIdle {
            model.change("name", "Delete before autosave")
            model.removeDraft(draft)
        }
        idle()
        Thread.sleep(400)
        compose.waitForIdle()
        assertNull(stored(draft))
        assertNull(model.editor.value)
    }

    @Test fun oversizedReplacementPreservesExistingAttachment() {
        val draft = newDraft()
        val context = compose.activity
        fun source(oversized: Boolean): File {
            val folder = File(context.cacheDir, "camera").apply { mkdirs() }
            return File(folder, "regression-${UUID.randomUUID()}.png").also {
                sourceFiles += it
                if (oversized) RandomAccessFile(it, "rw").use { file -> file.setLength(5L * 1024 * 1024 + 1) }
                else it.writeBytes(byteArrayOf(1, 2, 3))
            }
        }
        fun attach(file: File) {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            compose.runOnIdle { model.attach(uri) }
            idle()
        }
        attach(source(false))
        val original = requireNotNull(stored(draft)?.attachment)
        attach(source(true))
        assertEquals(original, stored(draft)?.attachment)
        assertEquals(original, model.editor.value?.draft?.attachment)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(original).readBytes())
        leave()
    }

    @Test fun changedFiltersClearOldRowsEvenWhenTheNewRequestFails() {
        val key = "query-regression-${UUID.randomUUID()}"
        compose.runOnIdle { model.load(key, "waste-records") }
        compose.waitUntil(30_000) { model.screens.value[key]?.loading == false }
        val first = requireNotNull(model.screens.value[key])
        assertTrue((first.data as JsonArray).isNotEmpty())
        compose.runOnIdle {
            model.load(key, "waste-records")
            assertEquals(first.data, model.screens.value[key]?.data)
        }
        compose.waitUntil(30_000) { model.screens.value[key]?.loading == false }
        compose.runOnIdle {
            model.load(key, "waste-records", mapOf("search" to "DEMO-ERROR"))
            assertEquals(JsonNull, model.screens.value[key]?.data)
        }
        compose.waitUntil(30_000) { model.screens.value[key]?.loading == false }
        assertNotNull(model.screens.value[key]?.failure)
        assertEquals(JsonNull, model.screens.value[key]?.data)
    }

    @Test fun slowOlderFilterCannotOverwriteNewerResults() {
        val key = "query-race-regression-${UUID.randomUUID()}"
        compose.runOnIdle { model.load(key, "waste-records", mapOf("search" to "DEMO-SLOW")) }
        Thread.sleep(200)
        compose.runOnIdle { model.load(key, "waste-records") }
        compose.waitUntil(30_000) {
            model.screens.value[key]?.let { !it.loading && (it.data as? JsonArray)?.isNotEmpty() == true } == true
        }
        val newest = requireNotNull(model.screens.value[key]).data
        Thread.sleep(2_500)
        compose.waitForIdle()
        assertEquals(newest, model.screens.value[key]?.data)
        assertNull(model.screens.value[key]?.failure)
    }

}
