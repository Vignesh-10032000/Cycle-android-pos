package dev.vignesh.cycleshop.ui.invoice

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [OcrResult] and its supporting data classes.
 *
 * These run on the JVM without an Android device.
 * They verify the data model's computed properties and ensure the hierarchy
 * (OcrResult → OcrTextBlock → OcrTextLine) is correct.
 *
 * Note: The actual ML Kit OCR engine requires a device; it is tested through
 * on-device instrumentation via InvoiceScanInstrumentedTest.
 */
class OcrResultTest {

    // ── Helper builders ───────────────────────────────────────────────────────

    private fun makeLine(text: String): OcrTextLine = OcrTextLine(
        lineText = text,
        elements = text.split(" "),
        boundingBox = OcrBoundingBox(0, 0, 100, 20)
    )

    private fun makeBlock(vararg lines: String): OcrTextBlock = OcrTextBlock(
        blockText = lines.joinToString("\n"),
        lines = lines.map { makeLine(it) },
        boundingBox = OcrBoundingBox(0, 0, 500, 100)
    )

    private fun makeResult(vararg blocks: OcrTextBlock): OcrResult = OcrResult(
        rawText = blocks.joinToString("\n\n") { it.blockText },
        blocks = blocks.toList(),
        processingMs = 1234L,
        imageWidthPx = 1920,
        imageHeightPx = 1080,
        rotationDegrees = 0
    )

    // ── OcrResult computed properties ─────────────────────────────────────────

    @Test
    fun `blockCount returns correct count`() {
        val result = makeResult(
            makeBlock("Invoice No: 466"),
            makeBlock("M.STEEL / MBW", "Date: 18/10/2026")
        )
        assertEquals(2, result.blockCount)
    }

    @Test
    fun `lineCount sums lines across all blocks`() {
        // Block 1 has 1 line, Block 2 has 2 lines → total 3
        val result = makeResult(
            makeBlock("Invoice No: 466"),
            makeBlock("M.STEEL / MBW", "Date: 18/10/2026")
        )
        assertEquals(3, result.lineCount)
    }

    @Test
    fun `hasContent is true when rawText is non-blank`() {
        val result = makeResult(makeBlock("Invoice No: 466"))
        assertTrue(result.hasContent)
    }

    @Test
    fun `hasContent is false when rawText is blank`() {
        val empty = OcrResult(
            rawText = "   ",
            blocks = emptyList(),
            processingMs = 50L,
            imageWidthPx = 800,
            imageHeightPx = 600,
            rotationDegrees = 90
        )
        assertFalse(empty.hasContent)
    }

    @Test
    fun `hasContent is false for empty rawText`() {
        val empty = OcrResult(
            rawText = "",
            blocks = emptyList(),
            processingMs = 50L,
            imageWidthPx = 800,
            imageHeightPx = 600,
            rotationDegrees = 0
        )
        assertFalse(empty.hasContent)
    }

    // ── OcrTextBlock ──────────────────────────────────────────────────────────

    @Test
    fun `OcrTextBlock preserves bounding box coordinates`() {
        val block = OcrTextBlock(
            blockText = "HSN 8714",
            lines = listOf(makeLine("HSN 8714")),
            boundingBox = OcrBoundingBox(10, 20, 300, 60)
        )
        assertEquals(10, block.boundingBox!!.left)
        assertEquals(20, block.boundingBox!!.top)
        assertEquals(300, block.boundingBox!!.right)
        assertEquals(60, block.boundingBox!!.bottom)
        assertEquals(290, block.boundingBox!!.width)
        assertEquals(40, block.boundingBox!!.height)
    }

    @Test
    fun `OcrTextBlock can have null bounding box`() {
        val block = OcrTextBlock(
            blockText = "Qty: 5",
            lines = emptyList(),
            boundingBox = null
        )
        assertEquals(null, block.boundingBox)
    }

    // ── OcrTextLine ────────────────────────────────────────────────────────────

    @Test
    fun `OcrTextLine preserves individual elements`() {
        val line = OcrTextLine(
            lineText = "Total Amount 12345.00",
            elements = listOf("Total", "Amount", "12345.00"),
            boundingBox = OcrBoundingBox(0, 0, 400, 30)
        )
        assertEquals(3, line.elements.size)
        assertEquals("Total", line.elements[0])
        assertEquals("12345.00", line.elements[2])
    }

    // ── processingMs ──────────────────────────────────────────────────────────

    @Test
    fun `processingMs is preserved via copy`() {
        val base = makeResult(makeBlock("test"))
        val withMs = base.copy(processingMs = 987L)
        assertEquals(987L, withMs.processingMs)
    }

    // ── Rotation metadata ─────────────────────────────────────────────────────

    @Test
    fun `rotationDegrees is preserved correctly for each valid rotation`() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val result = makeResult(makeBlock("text")).copy(rotationDegrees = rotation)
            assertEquals("Rotation $rotation should be preserved", rotation, result.rotationDegrees)
        }
    }

    // ── Image dimensions ──────────────────────────────────────────────────────

    @Test
    fun `image dimensions are stored and retrievable`() {
        val result = makeResult(makeBlock("test")).copy(
            imageWidthPx = 4096,
            imageHeightPx = 3072
        )
        assertEquals(4096, result.imageWidthPx)
        assertEquals(3072, result.imageHeightPx)
    }
}
