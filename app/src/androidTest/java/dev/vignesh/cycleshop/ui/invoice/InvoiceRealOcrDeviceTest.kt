package dev.vignesh.cycleshop.ui.invoice

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class InvoiceRealOcrDeviceTest {
    @Test
    fun mSteelPhotoReadsBothInvoiceUnits() = runBlocking {
        val parsed = parsePhoto("A")
        assertEquals(InvoiceLayoutType.MSTEEL_MBW, parsed.layoutType)
        assertEquals("466", parsed.invoiceNumber)
        assertQuantities("A", parsed, minimumReads = 11)
        assertEquals(0, BigDecimal(50).compareTo(parsed.quantitiesByUnit.getValue("GROSS")))
        assertEquals(0, BigDecimal(76).compareTo(parsed.quantitiesByUnit.getValue("PCS")))
        assertEquals(126.0, parsed.totalQuantityExtracted!!, 0.0)
        assertEquals(Reconciliation.UNAVAILABLE, parsed.quantityReconciliation)
        assertEquals(14000L, parsed.items[0].unitRatePaise)
        assertEquals(70000L, parsed.items[0].lineAmountPaise)
        assertEquals(220000L, parsed.items[8].lineAmountPaise)
    }

    @Test
    fun stryderPhotoReadsPrintedTotalsAndLeavesAmbiguousCellsUnresolved() = runBlocking {
        val parsed = parsePhoto("B")
        assertEquals(InvoiceLayoutType.BICYCLE_SUPPLIER, parsed.layoutType)
        assertQuantities("B", parsed, minimumReads = 22)
        assertEquals(62.0, parsed.totalQuantityExtracted!!, 0.0)
        assertEquals(31_277_000L, parsed.totalAmountExtractedPaise)
        assertEquals(580000L, parsed.items.first().unitRatePaise)
        assertEquals(580000L, parsed.items.first().lineAmountPaise)
        assertEquals(337500L, parsed.items.last().unitRatePaise)
        // The photo is cropped over the invoice number. A sales order number is a different field.
        assertNull(parsed.invoiceNumber)
        assertEquals(Reconciliation.UNAVAILABLE, parsed.quantityReconciliation)
    }

    private suspend fun parsePhoto(name: String): ParsedInvoice {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val bitmap = assets.open("invoice_$name.jpg").use {
            requireNotNull(BitmapFactory.decodeStream(it))
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            InvoiceTableParser().parse(text.toInvoiceOcrResult(bitmap.width, bitmap.height, 0))
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }

    private fun assertQuantities(name: String, parsed: ParsedInvoice, minimumReads: Int) {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val expected = assets.open("r2-quantity-$name.txt").bufferedReader().useLines { rows ->
            rows.filter { it.isNotBlank() }.map(String::toDouble).toList()
        }
        assertEquals(expected.size, parsed.items.size)
        assertTrue(parsed.items.all { it.boundingBox != null && it.rawLines.isNotEmpty() })
        var reads = 0
        parsed.items.zip(expected).forEach { (row, groundTruth) ->
            row.quantity?.let {
                assertEquals("Quantity on row ${row.lineNumber}", groundTruth, it, 0.0)
                reads++
            }
        }
        assertTrue("Read $reads of ${expected.size} quantities", reads >= minimumReads)
        Log.i("InvoiceParsingR2", "Photo $name: rows=${parsed.items.size}, valid quantities=$reads")
    }
}
