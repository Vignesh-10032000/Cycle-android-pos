package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PurchaseValidationEngine].
 *
 * Verifies strict purchase staging boundaries:
 * - Line item requires verified variantId to achieve VERIFIED_MATCH.
 * - Missing variantId is flagged as PENDING_MATCH.
 * - Non-positive quantity or price is rejected as UNMATCHED_REJECTED.
 * - Partial or missing OCR fields are flagged for manual review.
 * - Missing invoice header fields block staging.
 */
class PurchaseValidationEngineTest {

    @Test
    fun `line with valid variantId and clean OCR gets VERIFIED_MATCH`() {
        val result = PurchaseValidationEngine.validateLine(
            lineNumber = 1,
            description = "Stryder 20 Elvis Pro MelBlueOrg",
            variantId = 10L,
            quantity = 1.0,
            unitRatePaise = 580000L,
            qualityFlags = mapOf(
                "description" to FieldQuality.EXACT,
                "quantity" to FieldQuality.EXACT,
                "unitRate" to FieldQuality.EXACT
            )
        )

        assertTrue(result.isValid)
        assertEquals(LineReviewStatus.VERIFIED_MATCH, result.reviewStatus)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `line without variantId gets PENDING_MATCH and requires review`() {
        val result = PurchaseValidationEngine.validateLine(
            lineNumber = 2,
            description = "Unknown Unmatched Rim 26 inch",
            variantId = null,
            quantity = 5.0,
            unitRatePaise = 35000L,
            qualityFlags = mapOf("description" to FieldQuality.EXACT)
        )

        assertTrue(result.isValid) // Can be staged for review
        assertEquals(LineReviewStatus.PENDING_MATCH, result.reviewStatus)
        assertTrue(result.issues.any { it.contains("variant") })
    }

    @Test
    fun `line with zero or negative quantity is marked UNMATCHED_REJECTED and invalid`() {
        val zeroQtyResult = PurchaseValidationEngine.validateLine(
            lineNumber = 3,
            description = "Spokes 14G",
            variantId = 1L,
            quantity = 0.0,
            unitRatePaise = 15000L
        )

        assertFalse(zeroQtyResult.isValid)
        assertEquals(LineReviewStatus.UNMATCHED_REJECTED, zeroQtyResult.reviewStatus)
        assertTrue(zeroQtyResult.issues.any { it.contains("quantity") })

        val negQtyResult = PurchaseValidationEngine.validateLine(
            lineNumber = 4,
            description = "Spokes 14G",
            variantId = 1L,
            quantity = -10.0,
            unitRatePaise = 15000L
        )

        assertFalse(negQtyResult.isValid)
        assertEquals(LineReviewStatus.UNMATCHED_REJECTED, negQtyResult.reviewStatus)
    }

    @Test
    fun `line with zero or missing price is marked UNMATCHED_REJECTED and invalid`() {
        val nullPriceResult = PurchaseValidationEngine.validateLine(
            lineNumber = 5,
            description = "Spokes 14G",
            variantId = 1L,
            quantity = 10.0,
            unitRatePaise = null
        )

        assertFalse(nullPriceResult.isValid)
        assertEquals(LineReviewStatus.UNMATCHED_REJECTED, nullPriceResult.reviewStatus)
        assertTrue(nullPriceResult.issues.any { it.contains("unit rate") })
    }

    @Test
    fun `line with partial or obscured OCR quality flags is marked MANUAL_CORRECTED`() {
        val result = PurchaseValidationEngine.validateLine(
            lineNumber = 6,
            description = "Spokes 10G 300MM",
            variantId = 2L,
            quantity = 10.0,
            unitRatePaise = 15000L,
            qualityFlags = mapOf(
                "description" to FieldQuality.PARTIAL,
                "quantity" to FieldQuality.EXACT
            )
        )

        assertTrue(result.isValid)
        assertEquals(LineReviewStatus.MANUAL_CORRECTED, result.reviewStatus)
        assertTrue(result.issues.any { it.contains("uncertain or partial OCR") })
    }

    @Test
    fun `invoice validation passes with valid headers and line items`() {
        val line1 = PurchaseValidationEngine.validateLine(1, "Prod A", 1L, 2.0, 10000L)
        val line2 = PurchaseValidationEngine.validateLine(2, "Prod B", null, 1.0, 5000L)

        val summary = PurchaseValidationEngine.validateInvoice(
            supplierName = "MBW Cycles",
            invoiceNumber = "466",
            lineResults = listOf(line1, line2)
        )

        assertTrue(summary.canStage)
        assertEquals(2, summary.totalLines)
        assertEquals(1, summary.verifiedLinesCount)
        assertEquals(1, summary.unverifiedLinesCount)
        assertTrue(summary.globalIssues.isEmpty())
    }

    @Test
    fun `invoice validation fails if supplier name or invoice number is missing`() {
        val line = PurchaseValidationEngine.validateLine(1, "Prod A", 1L, 2.0, 10000L)

        val summaryNoSupplier = PurchaseValidationEngine.validateInvoice(
            supplierName = "   ",
            invoiceNumber = "466",
            lineResults = listOf(line)
        )
        assertFalse(summaryNoSupplier.canStage)
        assertTrue(summaryNoSupplier.globalIssues.any { it.contains("Supplier name") })

        val summaryNoInvNo = PurchaseValidationEngine.validateInvoice(
            supplierName = "MBW Cycles",
            invoiceNumber = "",
            lineResults = listOf(line)
        )
        assertFalse(summaryNoInvNo.canStage)
        assertTrue(summaryNoInvNo.globalIssues.any { it.contains("Invoice number") })
    }
}
