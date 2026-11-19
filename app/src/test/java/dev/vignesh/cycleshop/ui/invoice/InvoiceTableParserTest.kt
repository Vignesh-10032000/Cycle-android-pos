package dev.vignesh.cycleshop.ui.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceTableParserTest {
    private val parser = InvoiceTableParser()

    @Test
    fun `supplier headers alone never create line items or totals`() {
        for (name in listOf("M.STEEL", "STRYDER CYCLE PRIVATE LIMITED")) {
            val invoice = parse(emptyList(), "$name\nInvoice No: NEW-123")
            assertEquals("NEW-123", invoice.invoiceNumber)
            assertTrue(invoice.items.isEmpty())
            assertNull(invoice.totalQuantityExtracted)
            assertNull(invoice.totalAmountExtractedPaise)
        }
    }

    @Test
    fun `changed quantities and prices follow positioned cells even when OCR is shuffled`() {
        val rows = row(140, "Blue tube", "3.5", "17.42", "PC", "60.97") +
            row(165, "Wrench", "2", "21.10", "PC", "42.20")
        val invoice = parse((header() + rows).shuffled(java.util.Random(12)), "Shop\nInvoice No: NEW-9")
        assertEquals(listOf("Blue tube", "Wrench"), invoice.items.map { it.description })
        assertEquals(listOf(3.5, 2.0), invoice.items.map { it.quantity })
        assertEquals(listOf(1742L, 2110L), invoice.items.map { it.unitRatePaise })
        assertEquals(listOf(6097L, 4220L), invoice.items.map { it.lineAmountPaise })
        assertEquals(Reconciliation.MATCH, invoice.items.first().amountReconciliation)
        assertNull(invoice.totalQuantityExtracted)
        assertNull(invoice.totalAmountExtractedPaise)
    }

    @Test
    fun `deleted row is absent and printed totals warn about the difference`() {
        val invoice = parse(
            header() + row(140, "Tube", "2", "10.00", "PC", "20.00") +
                row(190, "Spoke", "3", "10.00", "PC", "30.00"),
            "Total Quantity: 7\nTaxable Value: 70.00\nGrand Total: 73.50"
        )
        assertEquals(2, invoice.items.size)
        assertEquals(listOf(2.0, 3.0), invoice.items.map { it.quantity })
        assertEquals(7000L, invoice.totalAmountExtractedPaise)
        assertEquals(Reconciliation.DIFFERENCE, invoice.quantityReconciliation)
        assertEquals(Reconciliation.DIFFERENCE, invoice.taxableReconciliation)
    }

    @Test
    fun `obscured amount and malformed tokens remain missing or flagged`() {
        val invoice = parse(
            header() + row(140, "Tube", "O.5", "12.3x", "PC", null) +
                row(165, "Tube", "2", "10.49", "PC", "20.97"),
            "Total Quantity: 2\nTaxable Value: 20.97"
        )
        val partial = invoice.items.first()
        assertNull(partial.quantity)
        assertNull(partial.unitRatePaise)
        assertNull(partial.lineAmountPaise)
        assertEquals(FieldQuality.PARTIAL, partial.qualityFlags["quantity"])
        assertEquals(FieldQuality.MISSING, partial.qualityFlags["lineAmount"])
        assertEquals(Reconciliation.UNAVAILABLE, invoice.quantityReconciliation)
        assertEquals(Reconciliation.UNAVAILABLE, invoice.taxableReconciliation)
        assertEquals(Reconciliation.DIFFERENCE, invoice.items.last().amountReconciliation)
    }

    @Test
    fun `two tokens in a single quantity cell do not select a winner`() {
        val cells = row(140, "Tube", "2", "10.00", "PC", "20.00") + word("7", 418, 141)
        val item = parse(header() + cells).items.single()
        assertNull(item.quantity)
        assertEquals(FieldQuality.PARTIAL, item.qualityFlags["quantity"])
        assertEquals(2000L, item.lineAmountPaise)
    }

    @Test
    fun `competing quantity column headings leave the grid unresolved`() {
        val words = header() + word("Qty", 455, 100) + row(140, "Tube", "2", "10.00", "PC", "20.00")
        assertTrue(parse(words).items.isEmpty())
    }

    @Test
    fun `unrecognized layouts do not use product dimensions or order numbers as evidence`() {
        val invoice = parse(
            listOf(word("27.5", 410, 125), word("200.00", 690, 125)),
            "Unknown Supplier\nSales Order No: 45678\nTube 27.5 x 2.00"
        )
        assertEquals(InvoiceLayoutType.GENERIC_GST, invoice.layoutType)
        assertNull(invoice.invoiceNumber)
        assertTrue(invoice.items.isEmpty())
    }

    @Test
    fun `mixed units are never presented as a reconciled stock quantity`() {
        val invoice = parse(
            header() + row(140, "Spokes", "2", "140.00", "Gross", "280.00") +
                row(165, "Grease", "3", "20.00", "PC", "60.00"),
            "M.STEEL\nTotal Quantity: 5"
        )
        assertEquals(setOf("GROSS", "PC"), invoice.quantitiesByUnit.keys)
        assertEquals(Reconciliation.UNAVAILABLE, invoice.quantityReconciliation)
        assertFalse(invoice.items.any { it.quantity == 288.0 })
    }

    private fun parse(words: List<OcrWord>, raw: String = "Test Supplier"): ParsedInvoice {
        // One word per OCR line deliberately models columns arriving in separate OCR blocks.
        val blocks = words.map { word ->
            OcrTextBlock(word.text, listOf(OcrTextLine(word.text, listOf(word.text),
                word.boundingBox, listOf(word))), word.boundingBox)
        }
        return parser.parse(OcrResult(raw, blocks, 0, 800, 600, 0))
    }

    private fun header() = listOf(
        word("Description", 90, 100), word("HSN", 300, 100), word("Quantity", 410, 100),
        word("Rate", 500, 100), word("Unit", 580, 100), word("Amount", 690, 100)
    )

    private fun row(y: Int, desc: String, qty: String, rate: String, unit: String, amount: String?) =
        listOfNotNull(
            word(desc, 90, y), word("87149990", 300, y), word(qty, 410, y),
            word(rate, 500, y), word(unit, 580, y), amount?.let { word(it, 690, y) }
        )

    private fun word(text: String, x: Int, y: Int) = OcrWord(text, OcrBoundingBox(x - 10, y, x + 10, y + 12))
}
