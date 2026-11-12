package com.ubpbsla.waspro

import com.ubpbsla.waspro.features.UserGuide
import org.junit.Assert.*
import org.junit.Test

class UserGuideTest {
    @Test fun catalogHasUniqueCompleteCasesAndKnownRoles() {
        assertEquals(UserGuide.cases.size, UserGuide.cases.map { it.id }.toSet().size)
        val roleKeys = UserGuide.roles.map { it.key }.toSet() - "all"
        UserGuide.cases.forEach {
            assertTrue(it.title.isNotBlank() && it.route.isNotBlank() && it.prerequisite.isNotBlank())
            assertTrue(it.steps.size >= 3 && it.steps.none(String::isBlank))
            assertTrue(it.roles.isNotEmpty() && roleKeys.containsAll(it.roles))
            assertTrue(it.result.isNotBlank() && it.caution.isNotBlank())
            assertTrue(it.screenshot == null || it.screenshot in setOf("form", "records", "detail", "reports"))
        }
    }
    @Test fun filtersDescribeResponsibilitiesWithoutAssigningPermissions() {
        assertFalse(UserGuide.filter("operator", "").any { it.id == "review" })
        assertTrue(UserGuide.filter("operator", "").any { it.id == "period" })
        assertTrue(UserGuide.filter("supervisor", "").any { it.id == "review" })
        assertEquals(UserGuide.cases, UserGuide.filter("all", "  "))
    }
    @Test fun searchCombinesRoleWithAllWordsAndHandlesUnknownCases() {
        assertEquals(listOf("faba-external"), UserGuide.filter("operator", "NOMOR dokumen vendor").map { it.id })
        assertTrue(UserGuide.filter("all", "xyz-unknown").isEmpty())
        assertNull(UserGuide.find("unknown"))
    }
    @Test fun currentRoleMatchesExactSlugOrNameAndSafelyFallsBack() {
        assertEquals("super_admin", UserGuide.initialRole("super_admin", "Anything"))
        assertEquals("operator", UserGuide.initialRole("", "Operator"))
        assertEquals("all", UserGuide.initialRole("custom", "Auditor"))
    }
    @Test fun guidanceDoesNotPromiseUnsupportedMobileFeatures() {
        assertTrue(UserGuide.find("directory")!!.caution.contains("tidak dapat berganti tenant"))
        assertTrue(UserGuide.find("hauling")!!.caution.contains("belum menyediakan"))
        assertTrue(UserGuide.find("draft")!!.steps.any { it.contains("Tidak ada pengiriman otomatis") })
        assertTrue(UserGuide.find("review")!!.caution.contains("Jangan menyetujui transaksi sendiri"))
    }
}