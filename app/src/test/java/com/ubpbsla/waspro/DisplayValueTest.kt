package com.ubpbsla.waspro

import com.ubpbsla.waspro.ui.displayValue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayValueTest {
    @Test fun yearsAreNotFormattedAsQuantities() {
        assertEquals("2026", displayValue("year", JsonPrimitive(2026)))
        assertEquals("2026", displayValue("period_year", JsonPrimitive(2026)))
        assertEquals("11", displayValue("month", JsonPrimitive(11)))
        assertEquals("1.250,5", displayValue("quantity", JsonPrimitive(1250.5)))
    }
    @Test fun datesUseReadableIndonesianDisplayWithoutHidingUnknownValues() {
        assertEquals("04 Nov 2026", displayValue("date", JsonPrimitive("2026-11-04")))
        assertEquals("unknown", displayValue("date", JsonPrimitive("unknown")))
    }
}
