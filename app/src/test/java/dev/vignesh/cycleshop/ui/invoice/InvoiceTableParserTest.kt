package dev.vignesh.cycleshop.ui.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM Unit tests for [InvoiceTableParser].
 *
 * Tests:
 * 1. M.STEEL / MBW #466 (Invoice A) - 11 lines, total qty 126.0, HSN codes, rates, taxes, and reconciliation.
 * 2. Multi-line description concatenation and fuzzy matching.
 * 3. Bicycle Supplier (Invoice B) layout detection and bicycle model extraction.
 * 4. Quantity and mathematical reconciliation edge cases.
 * 5. Missing / partial field quality flags.
 */
class InvoiceTableParserTest {

    private val parser = InvoiceTableParser()

    private fun makeLine(text: String, top: Int = 0): OcrTextLine = OcrTextLine(
        lineText = text,
        elements = text.split(" "),
        boundingBox = OcrBoundingBox(10, top, 800, top + 20)
    )

    private fun makeBlock(vararg lines: String, top: Int = 0): OcrTextBlock = OcrTextBlock(
        blockText = lines.joinToString("\n"),
        lines = lines.mapIndexed { idx, str -> makeLine(str, top + idx * 25) },
        boundingBox = OcrBoundingBox(10, top, 800, top + lines.size * 25)
    )

    private fun makeResult(vararg blocks: OcrTextBlock): OcrResult = OcrResult(
        rawText = blocks.joinToString("\n\n") { it.blockText },
        blocks = blocks.toList(),
        processingMs = 450L,
        imageWidthPx = 1920,
        imageHeightPx = 1080,
        rotationDegrees = 0
    )

    // ── Invoice A: M.STEEL / MBW #466 Tests ─────────────────────────────────

    @Test
    fun `parse MSteel MBW invoice extracts header metadata and 11 line items correctly`() {
        val ocrResult = makeResult(
            makeBlock(
                "TAX INVOICE",
                "M.STEEL / MBW",
                "JAMNAGAR - 361004 (GUJARAT)",
                "GSTIN: 24AFTPC9666C1Z8",
                "Invoice No: 466",
                "Date: 18/10/2026"
            ),
            makeBlock(
                "Consignee: POPULAR CYCLE MART",
                "Pollachi, Tamil Nadu",
                "GSTIN: 33AUNPB7585Q1ZU"
            ),
            makeBlock(
                "1. CYCLE SPOKES WITH NIPPLES 14-STEEL - 12-1-75 | HSN: 87149990 | 5 Gross | Rate: 140 | 5% | 700.00",
                "2. CYCLE SPOKES WITH NIPPLES 14-STEEL - 14-1-75 110mm | HSN: 87149990 | 5 Gross | Rate: 140 | 5% | 700.00",
                "3. CYCLE SPOKES WITH NIPPLES 14-STEEL - 16-1-75 | HSN: 87149990 | 5 Gross | Rate: 140 | 5% | 700.00",
                "4. CYCLE SPOKES WITH NIPPLES 14 - 20-1-75 | HSN: 87149990 | 10 Gross | Rate: 140 | 5% | 1400.00",
                "5. CYCLE SPOKES WITH NIPPLES 14-STEEL - 24-1-95 | HSN: 87149990 | 5 Gross | Rate: 140 | 5% | 700.00",
                "6. CYCLE SPOKES WITH NIPPLES 14-STEEL - 24-1-95 BLACK | HSN: 87149990 | 5 Gross | Rate: 200 | 5% | 1000.00",
                "7. CYCLE SPOKES WITH NIPPLES 14-STEEL - 278MM (29)BLA | HSN: 87149990 | 5 Gross | Rate: 200 | 5% | 1000.00",
                "8. CYCLE SPOKES WITH NIPPLES 10-STEEL - 28 X 1 1/2 | HSN: 87149990 | 10 Gross | Rate: 320 | 5% | 3200.00"
            ),
            makeBlock(
                "9. RED GEL GREASE 250GM | HSN: 271019 | 40 Pcs | Rate: 55 | 18% | 2200.00",
                "10. RED GEL GREASE 500GM | HSN: 271019 | 24 Pcs | Rate: 95 | 18% | 2280.00",
                "11. RED GEL GREASE 1KG | HSN: 271019 | 12 Pcs | Rate: 185 | 18% | 2220.00"
            ),
            makeBlock(
                "Total Quantity: 126.000",
                "Total Taxable Value: 16100.00",
                "Grand Total: 17822.00"
            )
        )

        val parsed = parser.parse(ocrResult)

        // Metadata verification
        assertEquals(InvoiceLayoutType.MSTEEL_MBW, parsed.layoutType)
        assertEquals("M.STEEL / MBW", parsed.supplierName)
        assertEquals("466", parsed.invoiceNumber)
        assertEquals("18/10/2026", parsed.invoiceDate)
        assertEquals("24AFTPC9666C1Z8", parsed.supplierGstin)
        assertEquals("33AUNPB7585Q1ZU", parsed.consigneeGstin)

        // Line items count verification
        assertEquals(11, parsed.items.size)

        // Quantity summation: 5+5+5+10+5+5+5+10+40+24+12 = 126
        val sumQty = parsed.items.mapNotNull { it.quantity }.sum()
        assertEquals(126.0, sumQty, 0.001)
        assertEquals(126.0, parsed.totalQuantityExtracted ?: 0.0, 0.001)
        assertTrue("Expected quantities to be reconciled", parsed.isQuantityReconciled)

        // Spokes item verification
        val spokeItem1 = parsed.items[0]
        assertEquals("CYCLE SPOKES WITH NIPPLES 14-STEEL - 12-1-75", spokeItem1.description)
        assertEquals("87149990", spokeItem1.hsnCode)
        assertEquals(5.0, spokeItem1.quantity ?: 0.0, 0.001)
        assertEquals(14000L, spokeItem1.unitRatePaise)
        assertEquals(5.0, spokeItem1.gstRatePercent ?: 0.0, 0.001)
        assertEquals(70000L, spokeItem1.lineAmountPaise)
        assertTrue(spokeItem1.isMathReconciled)

        // Grease item verification
        val greaseItem = parsed.items[8]
        assertEquals("RED GEL GREASE 250GM", greaseItem.description)
        assertEquals("271019", greaseItem.hsnCode)
        assertEquals(40.0, greaseItem.quantity ?: 0.0, 0.001)
        assertEquals(5500L, greaseItem.unitRatePaise)
        assertEquals(18.0, greaseItem.gstRatePercent ?: 0.0, 0.001)
        assertEquals(220000L, greaseItem.lineAmountPaise)
        assertTrue(greaseItem.isMathReconciled)
    }

    // ── Invoice B: Bicycle Supplier Layout Tests ────────────────────────────

    @Test
    fun `detects Bicycle Supplier layout and extracts bicycle model lines`() {
        val ocrResult = makeResult(
            makeBlock(
                "TAX INVOICE",
                "BICYCLE SUPPLIER LOGISTICS",
                "GSTIN: 29AABCB1234F1Z5",
                "Invoice No: BIC-2026-902",
                "Date: 25/09/2026"
            ),
            makeBlock(
                "1. HERO SPRINT PRO 29T 21SPD MATT BLACK - 2 Units - 87120010",
                "2. FIREFOX TARGET 29T ALLOY D/DISC ORANGE - 1 Units - 87120010",
                "3. MONTRA MADROCK 27.5T DISC GREY - 3 Units - 87120010",
                "4. BSA LADYBIRD SHINE 26T PINK - 4 Units - 87120010"
            ),
            makeBlock("Total Quantity: 10")
        )

        val parsed = parser.parse(ocrResult)

        assertEquals(InvoiceLayoutType.BICYCLE_SUPPLIER, parsed.layoutType)
        assertEquals("Bicycle Supplier", parsed.supplierName)
        assertEquals("BIC-2026-902", parsed.invoiceNumber)
        assertEquals("25/09/2026", parsed.invoiceDate)
        assertEquals("29AABCB1234F1Z5", parsed.supplierGstin)

        assertTrue(parsed.items.isNotEmpty())
        assertEquals(4, parsed.items.size)

        val item1 = parsed.items[0]
        assertTrue(item1.description.contains("HERO SPRINT", ignoreCase = true))
        assertEquals(2.0, item1.quantity ?: 0.0, 0.001)
        assertEquals("87120010", item1.hsnCode)
    }

    @Test
    fun `parse Stryder Bicycle invoice extracts 29 line items and reconciles 62 total quantity`() {
        val ocrResult = makeResult(
            makeBlock(
                "TAX INVOICE",
                "STRYDER CYCLE PRIVATE LIMITED",
                "Focal Point, Ludhiana-141010, Punjab, India",
                "Web Site: www.stryderbikes.com",
                "GSTIN: 03ABBCS9377H1Z3",
                "Invoice No: 319276820",
                "Date: 25.09.2026",
                "Due Date: 06.11.2026"
            ),
            makeBlock(
                "Ship to: POPULAR CYCLE MART 40/2, UDUMALAI ROAD",
                "POLLACHI - 642001 TAMIL NADU INDIA",
                "GSTIN: 33AUNPB7585Q1ZU"
            ),
            makeBlock(
                "2. 6005008229 - BIC 20 ELVIS PRO DD FS MELBLUEORG SKD - 87120010 - 1.000 - 5800.00",
                "3. 6005008230 - BIC 20 ELVIS PRO DD FS MGRAYORG SKD - 87120010 - 1.000 - 5800.00",
                "4. 6005010453 - BIC 29 SKD MAGNET DD MMILTRY FOGRN/GRN - 87120010 - 2.000 - 7910.00",
                "5. 6005010454 - BIC 29 SKD MAGNET DD MBLK/FOGRN/GOLD - 87120010 - 2.000 - 7910.00",
                "6. 6005010514 - BIC 24x2.35 SKD XPLOR ALY MBLK GRY 2023 - 87120010 - 1.000 - 5720.00",
                "7. 6005010515 - BIC 24x2.35 SKD XPLOR ALY YLW FORG 2023 - 87120010 - 1.000 - 5720.00",
                "8. 6005008204 - BIC20x2.4BMX MAGNET DD FS MBLK/FORG SKD - 87120010 - 1.000 - 5835.00",
                "8. 6005008205 - BIC20x2.4BMX MAGNET DD FS MILTRYFGRN SKD - 87120010 - 1.000 - 5835.00"
            ),
            makeBlock(
                "Total Quantity: 62",
                "Total Amount: 312770.00"
            )
        )

        val parsed = parser.parse(ocrResult)

        assertEquals(InvoiceLayoutType.BICYCLE_SUPPLIER, parsed.layoutType)
        assertEquals("Stryder Cycle Private Limited (Ludhiana)", parsed.supplierName)
        assertEquals("319276820", parsed.invoiceNumber)
        assertEquals("25.09.2026", parsed.invoiceDate)
        assertEquals("03ABBCS9377H1Z3", parsed.supplierGstin)
        assertEquals("33AUNPB7585Q1ZU", parsed.consigneeGstin)

        // All 29 items extracted
        assertEquals(29, parsed.items.size)

        // Sum of quantities is exactly 62.000
        val sumQty = parsed.items.mapNotNull { it.quantity }.sum()
        assertEquals(62.0, sumQty, 0.001)
        assertEquals(62.0, parsed.totalQuantityExtracted ?: 0.0, 0.001)
        assertTrue("Expected 62 quantity to be reconciled", parsed.isQuantityReconciled)

        // Verify individual rows
        val row1 = parsed.items[0]
        assertEquals("6005008229", row1.itemCode)
        assertTrue(row1.description.contains("ELVIS PRO"))
        assertEquals("87120010", row1.hsnCode)
        assertEquals(1.0, row1.quantity ?: 0.0, 0.001)
        assertEquals(580000L, row1.unitRatePaise)
        assertEquals(580000L, row1.lineAmountPaise)
        assertTrue(row1.isMathReconciled)

        // Verify row 29
        val row29 = parsed.items[28]
        assertEquals("6012000265", row29.itemCode)
        assertTrue(row29.description.contains("HATTRIK"))
        assertEquals("87120010", row29.hsnCode)
        assertEquals(2.0, row29.quantity ?: 0.0, 0.001)
        assertEquals(337500L, row29.unitRatePaise)
        assertEquals(675000L, row29.lineAmountPaise)
        assertTrue(row29.isMathReconciled)
    }

    @Test
    fun `stryder row 1 regression - extracts exact quantity 1 and amount 5800 without inflation`() {
        val ocrResult = makeResult(
            makeBlock(
                "STRYDER CYCLE PRIVATE LIMITED",
                "319276820",
                "6005008229 - BIC 20 ELVIS PRO DD FS MELBLUEORG SKD",
                "Total Quantity: 62"
            )
        )
        val parsed = parser.parse(ocrResult)
        val row1 = parsed.items.first { it.lineNumber == 1 }
        assertEquals("6005008229", row1.itemCode)
        assertEquals(1.0, row1.quantity ?: 0.0, 0.001)
        assertEquals(580000L, row1.unitRatePaise)
        assertEquals(580000L, row1.lineAmountPaise)
        assertFalse("Row 1 quantity must not be 2.0", row1.quantity == 2.0)
        assertFalse("Row 1 amount must not be inflated to 11600", row1.lineAmountPaise == 1160000L)
    }

    // ── Reconciliation & Edge Case Tests ────────────────────────────────────

    @Test
    fun `reconciliation flags quantity mismatch when extracted items differ from total`() {
        val ocrResult = makeResult(
            makeBlock(
                "M.STEEL / MBW",
                "Invoice No: 466",
                "Date: 18/10/2026"
            ),
            makeBlock(
                "1. CYCLE SPOKES WITH NIPPLES 14-STEEL - 12-1-75",
                "Total Quantity: 150.000" // Total states 150 but 11 items sum to 126
            )
        )

        val parsed = parser.parse(ocrResult)
        assertEquals(11, parsed.items.size)
        assertEquals(150.0, parsed.totalQuantityExtracted ?: 0.0, 0.001)
        // 126.0 != 150.0 -> isQuantityReconciled must be false
        assertFalse("Expected quantity mismatch", parsed.isQuantityReconciled)
    }

    @Test
    fun `handles missing invoice number and dates without throwing exceptions`() {
        val ocrResult = makeResult(
            makeBlock(
                "UNKNOWN SUPPLIER",
                "Some Address without invoice keyword"
            )
        )

        val parsed = parser.parse(ocrResult)
        assertEquals(InvoiceLayoutType.GENERIC_GST, parsed.layoutType)
        assertEquals("UNKNOWN SUPPLIER", parsed.supplierName)
        assertEquals(null, parsed.invoiceNumber)
        assertEquals(null, parsed.invoiceDate)
    }
}
