package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [InvoiceDeduplicationEngine].
 *
 * Verifies supplier-aware deduplication:
 * - Scoped to (supplier, invoice_number) so different suppliers can use the same invoice number.
 * - Detects duplicate by GSTIN + invoice number.
 * - Detects duplicate by normalized supplier name + invoice number.
 * - Flags supporting doc ref collision for the same supplier.
 */
class InvoiceDeduplicationEngineTest {

    private val existingInvoices = listOf(
        PurchaseInvoiceEntity(
            id = 101L,
            supplierName = "M.STEEL INDUSTRIES",
            supplierGstin = "03AABCM1234F1Z1",
            invoiceNumber = "466",
            invoiceDate = "18/10/2026",
            totalAmountPaise = 7717400L,
            totalQuantity = 126.0,
            status = PurchaseInvoiceStatus.CONFIRMED_STAGED,
            supportingDocRef = "SO-2026-88",
            createdAt = 1000L,
            updatedAt = 1000L
        ),
        PurchaseInvoiceEntity(
            id = 102L,
            supplierName = "STRYDER CYCLE PRIVATE LIMITED",
            supplierGstin = "03AABCS9876E1Z5",
            invoiceNumber = "242502690",
            invoiceDate = "25/09/2026",
            totalAmountPaise = 31277000L,
            totalQuantity = 62.0,
            status = PurchaseInvoiceStatus.CONFIRMED_STAGED,
            supportingDocRef = "DC-9921",
            createdAt = 2000L,
            updatedAt = 2000L
        )
    )

    @Test
    fun `detects exact duplicate by GSTIN and invoice number`() {
        val result = InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = "M Steel Industries",
            incomingSupplierGstin = "03AABCM1234F1Z1",
            incomingInvoiceNumber = "466",
            incomingDocRef = null,
            existingInvoices = existingInvoices
        )

        assertTrue(result is DeduplicationResult.ExactDuplicate)
        val dup = result as DeduplicationResult.ExactDuplicate
        assertEquals(101L, dup.existingInvoiceId)
        assertEquals("466", dup.invoiceNumber)
    }

    @Test
    fun `detects duplicate by normalized supplier name when GSTIN is missing`() {
        val result = InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = "M. Steel Industries Ltd.",
            incomingSupplierGstin = null,
            incomingInvoiceNumber = " 466 ",
            incomingDocRef = null,
            existingInvoices = existingInvoices
        )

        assertTrue(result is DeduplicationResult.ExactDuplicate)
        val dup = result as DeduplicationResult.ExactDuplicate
        assertEquals(101L, dup.existingInvoiceId)
    }

    @Test
    fun `allows distinct supplier with identical invoice number`() {
        // Bicycle supplier also issues invoice #466
        val result = InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = "Hero Cycles Ltd",
            incomingSupplierGstin = "07AAAAH0000A1Z9",
            incomingInvoiceNumber = "466",
            incomingDocRef = null,
            existingInvoices = existingInvoices
        )

        // Must be unique because supplier is completely different
        assertEquals(DeduplicationResult.Unique, result)
    }

    @Test
    fun `allows new invoice number from same supplier`() {
        val result = InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = "M.STEEL INDUSTRIES",
            incomingSupplierGstin = "03AABCM1234F1Z1",
            incomingInvoiceNumber = "467",
            incomingDocRef = null,
            existingInvoices = existingInvoices
        )

        assertEquals(DeduplicationResult.Unique, result)
    }

    @Test
    fun `flags potential duplicate on matching supporting document reference`() {
        val result = InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = "STRYDER CYCLE PVT LTD",
            incomingSupplierGstin = "03AABCS9876E1Z5",
            incomingInvoiceNumber = "NEW-INV-001",
            incomingDocRef = "DC-9921",
            existingInvoices = existingInvoices
        )

        assertTrue(result is DeduplicationResult.PotentialDuplicate)
        val pot = result as DeduplicationResult.PotentialDuplicate
        assertEquals(102L, pot.existingInvoiceId)
        assertEquals("DC-9921", pot.matchedValue)
    }
}
