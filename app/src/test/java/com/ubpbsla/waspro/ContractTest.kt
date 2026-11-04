package com.ubpbsla.waspro

import com.ubpbsla.waspro.data.*
import com.ubpbsla.waspro.features.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    @Test fun emptyArrayMetadataAndErrorsAreAccepted() {
        assertTrue(decodeResponse(200, """{"success":true,"data":[],"meta":[]}""").meta.isEmpty())
        val failure = runCatching { decodeResponse(422, """{"success":false,"message":"Tidak valid","errors":[]}""") }.exceptionOrNull() as ApiFailure
        assertTrue(failure.fields.isEmpty())
    }
    @Test fun nonJsonGatewayErrorsAreSafe() {
        val failure = runCatching { decodeResponse(502, "<html>Bad gateway</html>") }.exceptionOrNull() as ApiFailure
        assertEquals(502, failure.status)
        assertFalse(failure.message.contains("<html>"))
    }
    @Test fun fieldValidationMessagesRemainAvailable() {
        val failure = runCatching { decodeResponse(422, """{"success":false,"errors":{"quantity":["Jumlah wajib","Minimal 1"]}}""") }.exceptionOrNull() as ApiFailure
        assertEquals("Jumlah wajib\nMinimal 1", failure.fields["quantity"])
    }
    @Test fun indonesianDecimalKeepsBusinessPrecision() {
        assertEquals("1234.50", normalizedDecimal("1.234,50"))
        assertEquals("12.25", normalizedDecimal("12.25"))
        assertNull(normalizedDecimal("bukan angka"))
        val record = WasteRecordDto.from(wireJson.parseToJsonElement("""{"id":"uuid","record_number":"W-1","quantity":9999999999.99}""").jsonObject)
        assertEquals("9999999999.99", record.quantity.toPlainString())
    }
    @Test fun externalUtilizationRequiresDocumentsAndAttachment() {
        val errors = validateForm(Modules.utilization, mapOf("movement_type" to "utilization_external", "quantity" to "1", "material_type" to "fly_ash", "transaction_date" to "2026-01-01"), false)
        assertTrue(errors.keys.containsAll(listOf("vendor_id", "document_number", "document_date", "attachment")))
    }
    @Test fun internalUtilizationDoesNotRequireExternalDocument() {
        val errors = validateForm(Modules.utilization, mapOf("movement_type" to "utilization_internal", "quantity" to "1,50", "material_type" to "fly_ash", "transaction_date" to "2026-01-01", "internal_destination_id" to "destination"), false)
        assertTrue(errors.isEmpty())
    }
    @Test fun wasteAllowsZeroButHaulingDoesNot() {
        assertFalse("quantity" in validateForm(Modules.waste, mapOf("quantity" to "0"), false))
        assertTrue("quantity" in validateForm(Modules.hauling, mapOf("quantity" to "0"), false))
    }
    @Test fun permissionVisibilityUsesActualPermissions() {
        val operator = UserContext(wireJson.parseToJsonElement("""{"user":{"id":"operator"},"permissions":["waste_records.view_all","faba_approvals.submit"]}""").jsonObject)
        assertTrue(Modules.visible(Modules.waste, operator))
        assertFalse(operator.canReview)
    }
}
