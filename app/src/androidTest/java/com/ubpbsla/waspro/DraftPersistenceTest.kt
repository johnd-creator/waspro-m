package com.ubpbsla.waspro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ubpbsla.waspro.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DraftPersistenceTest {
    @Test fun draftsSurviveDatabaseReopenAndStayScopedToAccountAndEnvironment() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "draft-test-${UUID.randomUUID()}.db"
        val first = Room.databaseBuilder(context, LocalStore::class.java, name).build()
        val draft = Draft("staging|org-a|operator", "draft-1", "waste", null,
            """{"quantity":"1,25"}""", null, "stable-request-key", null, 123)
        first.dao().save(draft)
        first.close()
        val reopened = Room.databaseBuilder(context, LocalStore::class.java, name).build()
        try {
            assertEquals(draft, reopened.dao().draft(draft.scope, draft.id))
            assertNull(reopened.dao().draft("production|org-a|operator", draft.id))
            assertNull(reopened.dao().draft("staging|org-b|operator", draft.id))
            assertNull(reopened.dao().draft("staging|org-a|supervisor", draft.id))
            reopened.dao().clearDrafts("staging|org-a|supervisor")
            assertNotNull(reopened.dao().draft(draft.scope, draft.id))
            reopened.dao().clearDrafts(draft.scope)
            assertNull(reopened.dao().draft(draft.scope, draft.id))
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
