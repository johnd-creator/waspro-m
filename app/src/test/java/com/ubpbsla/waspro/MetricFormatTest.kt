package com.ubpbsla.waspro

import com.ubpbsla.waspro.ui.formatMetric
import org.junit.Assert.*
import org.junit.Test

class MetricFormatTest {
    @Test fun decimalStringsPreserveValueAndUseIndonesianSeparators() {
        assertEquals("2.480,75", formatMetric("2480.75"))
        assertEquals("9.999.999.999,125", formatMetric("9999999999.125"))
        assertEquals("-12,5", formatMetric("-12.50"))
    }

    @Test fun reportedZeroIsDifferentFromMissingOrInvalidData() {
        assertEquals("0", formatMetric("0.00"))
        assertNull(formatMetric(""))
        assertNull(formatMetric("unavailable"))
    }
}
