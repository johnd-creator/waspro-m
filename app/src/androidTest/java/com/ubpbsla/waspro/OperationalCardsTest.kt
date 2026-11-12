package com.ubpbsla.waspro

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ubpbsla.waspro.ui.DashboardSummary
import com.ubpbsla.waspro.ui.StateHeader
import com.ubpbsla.waspro.features.ScreenData
import androidx.compose.runtime.mutableStateOf
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

    @Test fun cachedNetworkFailureKeepsItsTimestampAndOffersRetry() {
        val state = mutableStateOf(ScreenData(cachedAt = 1000L, failure = "Koneksi terputus"))
        compose.setContent { MaterialTheme { Column { StateHeader(state.value) { state.value = ScreenData() } } } }
        compose.onNodeWithText("Data tersimpan", substring = true).assertExists()
        compose.onNodeWithText("Koneksi terputus").assertExists()
        compose.onNodeWithText("Belum ada data.").assertDoesNotExist()
        compose.onNodeWithText("Muat ulang").performClick()
        compose.onNodeWithText("Belum ada data.").assertExists()
    }
}
