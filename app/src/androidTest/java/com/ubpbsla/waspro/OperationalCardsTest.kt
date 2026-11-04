package com.ubpbsla.waspro

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ubpbsla.waspro.ui.DashboardSummary
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

class OperationalCardsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dashboardShowsReportedZeroAndKeepsOtherFieldsInDetails() {
        val data = Json.parseToJsonElement("""{"waste":{"pending_records":0,"draft_records":7}}""")
        compose.setContent { MaterialTheme { Column { DashboardSummary(data) } } }
        compose.onNodeWithText("0").assertExists()
        compose.onNodeWithText("Saldo FABA").assertDoesNotExist()
        compose.onNodeWithText("7").assertDoesNotExist()
        compose.onNodeWithText("Limbah").performClick()
        compose.onNodeWithText("7").assertExists()
    }
}
