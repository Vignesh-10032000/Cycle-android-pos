package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceReviewSessionTest {
    private val active = setOf(4L, 5L)

    @Test
    fun `exact suggestions do not select or verify a variant`() {
        val original = ReviewLine.from(item().copy(suggestedCandidates = listOf(candidate())))
        assertEquals(OwnerReviewStatus.SUGGESTED_MATCH, original.status(active))
        assertNull(original.values.variantId)
        assertNull(original.verification)
        assertNull(original.verify(active, 10).verification)
        val selected = original.edit(original.values.copy(variantId = 4L))
        assertEquals(OwnerReviewStatus.OWNER_SELECTED, selected.status(active))
        assertNull(selected.verification)
        val verified = selected.verify(active, 123)
        assertEquals(OwnerReviewStatus.OWNER_VERIFIED, verified.status(active))
        assertEquals(123L, verified.verification?.verifiedAtMillis)
        assertEquals(selected.values, verified.verification?.values)
        assertEquals(selected.revision, verified.verification?.revision)
    }

    @Test
    fun `all material edits invalidate the verified revision and reverting does not restore it`() {
        val verified = selectedLine().verify(active, 100)
        val v = verified.values
        val edits = listOf(v.copy(quantity = 4.0), v.copy(ratePaise = 130), v.copy(unit = "Gross"),
            v.copy(amountPaise = 400), v.copy(description = "Corrected"), v.copy(variantId = 5L))
        edits.forEach { values ->
            val edited = verified.edit(values)
            assertEquals(verified.revision + 1, edited.revision)
            assertEquals(OwnerReviewStatus.NEEDS_REVIEW, edited.status(active))
            assertNull(edited.verification)
            assertNull(edited.edit(v).verification)
            val reverification = edited.verify(active, 200)
            assertEquals(edited.revision, reverification.verification?.revision)
            assertEquals(values, reverification.verification?.values)
        }
        assertEquals(verified.revision, verified.edit(v).revision)
        assertNotNull(verified.edit(v).verification)
    }

    @Test
    fun `invalid mandatory fields and conversions block verification`() {
        val line = selectedLine()
        val v = line.values
        val invalid = listOf(
            v.copy(quantity = null), v.copy(quantity = Double.NaN), v.copy(quantity = Double.POSITIVE_INFINITY),
            v.copy(quantity = -1.0), v.copy(quantity = Int.MAX_VALUE.toDouble() + 1),
            v.copy(quantity = 0.0), v.copy(quantity = 1.5), v.copy(unit = "Gross", quantity = Int.MAX_VALUE.toDouble()),
            v.copy(ratePaise = null), v.copy(ratePaise = 0), v.copy(ratePaise = -1),
            v.copy(description = ""), v.copy(unit = null), v.copy(unit = "Set"),
            v.copy(amountPaise = null), v.copy(amountPaise = -1), v.copy(variantId = null)
        )
        invalid.forEach { assertNull(it.toString(), line.edit(it).verify(active, 100).verification) }
        assertNull(line.verify(emptySet(), 100).verification)
        assertEquals(OwnerReviewStatus.NEEDS_REVIEW, line.verify(active, 100).status(emptySet()))
    }

    @Test
    fun `missing header rejected line and unavailable variant block one whole invoice approval`() {
        val reviewed = session().updateLine(0) { selectAndVerify(it) }
        val rejected = reviewed.updateLine(1) { selectAndVerify(it).setRejected(true) }
        assertEquals(OwnerReviewStatus.REJECTED, rejected.lines.last().status(active))
        assertFalse(rejected.canApprove(active))
        assertFalse(rejected.approve(active, 100).isApproved(active))
        val complete = reviewed.updateLine(1) { selectAndVerify(it) }
        assertFalse(complete.editHeader("", "X-7").canApprove(active))
        assertFalse(complete.canApprove(setOf(5L)))
        val approved = complete.approve(active, 200)
        assertTrue(approved.isApproved(active))
        assertFalse(approved.editHeader("Shop", "X-8").isApproved(active))
        val edited = approved.updateLine(0) { it.edit(it.values.copy(quantity = 3.0)) }
        assertFalse(edited.isApproved(active))
        assertEquals(1, edited.verifiedCount(active))
    }

    @Test
    fun `immutable OCR collections and values survive correction and verification`() {
        val words = mutableListOf("tub3", "2", "PC")
        val flags = mutableMapOf("description" to FieldQuality.PARTIAL)
        val source = item().copy(rawLines = words, qualityFlags = flags, boundingBox = OcrBoundingBox(0, 0, 20, 20))
        val original = ReviewLine.from(source)
        words.clear()
        flags.clear()
        val edited = original.edit(ReviewValues("Tube", 3.0, "PCS", 125L, 375L, 4L)).verify(active, 100)
        assertEquals(listOf("tub3", "2", "PC"), edited.evidence.rawLines)
        assertEquals(mapOf("description" to FieldQuality.PARTIAL), edited.evidence.qualityFlags)
        assertEquals(source.boundingBox, edited.evidence.boundingBox)
        assertEquals(source.description, edited.original.description)
        assertEquals(2.0, edited.original.quantity!!, 0.0)
        assertEquals("PC", edited.original.unit)
        assertEquals(100L, edited.original.ratePaise)
        assertEquals(200L, edited.original.amountPaise)
        assertEquals(375L, edited.verification?.values?.amountPaise)
    }

    @Test
    fun `v2 stages representable corrections separately and requires fresh review`() {
        val session = session(item()).updateLine(0) {
            it.edit(it.values.copy(description = "New description", quantity = 3.0,
                ratePaise = 120, amountPaise = 360, variantId = 4)).verify(active, 200)
        }
        assertTrue(runCatching { InvoiceStagingV2.from(session, active, "image") }.isFailure)
        val draft = InvoiceStagingV2.from(session.approve(active, 300), active, "image")
        assertEquals(PurchaseInvoiceStatus.NEEDS_REVIEW, draft.invoice.status)
        assertEquals("source words", draft.invoice.rawOcrText)
        val row = draft.lines.single()
        assertEquals(LineReviewStatus.PENDING_MATCH, row.reviewStatus)
        assertEquals("OCR tube", row.rawDescription)
        assertEquals("New description", row.correctedDescription)
        assertEquals(2.0, row.rawQuantity, 0.0)
        assertEquals(3.0, row.correctedQuantity!!, 0.0)
        assertEquals(100L, row.rawUnitPricePaise)
        assertEquals(120L, row.correctedUnitPricePaise)
        assertEquals(200L, row.rawLineAmountPaise)
        assertEquals(360L, row.correctedLineAmountPaise)
    }

    @Test
    fun `v2 refuses to invent original quantity or silently discard a corrected unit`() {
        val cases = listOf(item().copy(quantity = null), item().copy(unit = "PO"))
        for (source in cases) {
            val reviewed = session(source).updateLine(0) {
                it.edit(it.values.copy(unit = "PC", quantity = 2.0, variantId = 4L)).verify(active, 100)
            }.approve(active, 200)
            assertTrue(reviewed.isApproved(active))
            assertTrue(reviewed.v2StagingIssues().isNotEmpty())
            assertTrue(runCatching { InvoiceStagingV2.from(reviewed, active, "image") }.isFailure)
            assertEquals(source.quantity, reviewed.lines.single().original.quantity)
            assertEquals(source.unit, reviewed.lines.single().original.unit)
        }
    }

    private fun selectedLine() = ReviewLine.from(item()).let { it.edit(it.values.copy(variantId = 4)) }
    private fun selectAndVerify(line: ReviewLine) = line.edit(line.values.copy(variantId = 4)).verify(active, 100)
    private fun item(number: Int = 1) =
        ParsedInvoiceItem(number, description = "OCR tube", quantity = 2.0, unit = "PC", unitRatePaise = 100, lineAmountPaise = 200)

    private fun session(vararg items: ParsedInvoiceItem) = InvoiceReviewSession.from(
        ParsedInvoice("Shop", "X-7", null, null, null, null,
            items.toList().ifEmpty { listOf(item(), item(2)) },
            null, null, InvoiceLayoutType.GENERIC_GST, "source words")
    )

    private fun candidate() = ProductMatchCandidate(4, 2, "Tube", "Brand", "SKU-1", null, null, null, 100, 140, 1f, "Exact SKU")
}
