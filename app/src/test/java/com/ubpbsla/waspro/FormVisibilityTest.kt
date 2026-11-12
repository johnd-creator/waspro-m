package com.ubpbsla.waspro

import com.ubpbsla.waspro.features.*
import org.junit.Assert.*
import org.junit.Test

class FormVisibilityTest {
    private val internal = mapOf("movement_type" to "utilization_internal", "quantity" to "1,50", "material_type" to "fly_ash", "transaction_date" to "2026-01-01", "internal_destination_id" to "destination", "document_date" to "invalid hidden value")
    @Test fun optionalPeriodFiltersValidateOnlySuppliedValues() {
        assertTrue(validatePeriodFilters(emptyMap()).isEmpty())
        assertTrue(validatePeriodFilters(mapOf("year" to "2026", "month" to "11")).isEmpty())
        assertEquals(setOf("year", "month"), validatePeriodFilters(mapOf("year" to "abc", "month" to "13")).keys)
    }
    @Test fun hiddenExternalFieldsDoNotBlockInternalForm() {
        assertTrue(validateForm(Modules.utilization, internal, false).isEmpty())
        assertFalse(isApplicableField(Modules.utilization, Modules.utilization.fields.first { it.key == "document_date" }, internal))
    }
    @Test fun conditionalRequiredLabelsMatchValidation() {
        val external = internal + ("movement_type" to "utilization_external")
        listOf("vendor_id", "document_number", "document_date").forEach { key ->
            assertTrue(effectiveField(Modules.utilization, Modules.utilization.fields.first { it.key == key }, external).required)
        }
        assertTrue("document_date" in validateForm(Modules.utilization, external, true))
        assertTrue(effectiveField(Modules.utilization, Modules.utilization.fields.first { it.key == "internal_destination_id" }, internal).required)
    }
}
