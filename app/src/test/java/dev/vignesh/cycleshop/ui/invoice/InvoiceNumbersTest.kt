package dev.vignesh.cycleshop.ui.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InvoiceNumbersTest {
    @Test
    fun `price conversion keeps every paise and supports Indian and western grouping`() {
        assertEquals(29L, InvoiceNumbers.paise("0.29"))
        assertEquals(31_277_000L, InvoiceNumbers.paise("₹3,12,770.00"))
        assertEquals(31_277_000L, InvoiceNumbers.paise("312,770.00"))
        assertEquals(Long.MAX_VALUE, InvoiceNumbers.paise("92233720368547758.07"))
        assertNull(InvoiceNumbers.paise("92233720368547758.08"))
    }

    @Test
    fun `invalid prices reject partial matches and fractional paise`() {
        for (text in listOf("NaN", "Infinity", "-3.50", "0", "1.005", "1,23.00", "1e3", "12.00x")) {
            assertNull(text, InvoiceNumbers.paise(text))
        }
    }

    @Test
    fun `quantities reject non-finite negative or lossy values`() {
        for (text in listOf("NaN", "Infinity", "-3", "2147483648", "0.10000000000000001", "5.0x")) {
            assertNull(text, InvoiceNumbers.quantity(text))
        }
    }

    @Test
    fun `line reconciliation compares without multiplication overflow or rounding`() {
        val large = ParsedInvoiceItem(1, description = "Large", quantity = 2.0,
            unitRatePaise = Long.MAX_VALUE, lineAmountPaise = Long.MAX_VALUE)
        assertEquals(Reconciliation.DIFFERENCE, large.amountReconciliation)
        val fractional = large.copy(quantity = 0.5, unitRatePaise = 3, lineAmountPaise = 1)
        assertEquals(Reconciliation.DIFFERENCE, fractional.amountReconciliation)
        assertEquals(Reconciliation.UNAVAILABLE, fractional.copy(lineAmountPaise = null).amountReconciliation)
    }
}
