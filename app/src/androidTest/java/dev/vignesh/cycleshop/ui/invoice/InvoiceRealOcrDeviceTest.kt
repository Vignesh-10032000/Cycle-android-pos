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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * On-device instrumentation test running Google ML Kit Text Recognition v2,
 * InvoiceTableParser, and ProductMatchingEngine directly on the physical OnePlus 9R.
 *
 * Tests the real supplier invoice photographs:
 * 1. Invoice A: M.STEEL / MBW #466 (Spokes & Grease - 11 items, 126 units)
 * 2. Invoice B: Stryder Bicycles #319276820 (Bicycle models - 29 items, 62 units)
 */
@RunWith(AndroidJUnit4::class)
class InvoiceRealOcrDeviceTest {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val parser = InvoiceTableParser()

    @Test
    fun testRealInvoiceA_MSteelMbw_OnDevice(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("invoice_A.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Bitmap decoding should succeed", bitmap)

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        var ocrTimeMs = 0L
        val mlKitText = measureTimeMillis {
            // Processing directly on device CPU/NPU
        }.let {
            val start = System.currentTimeMillis()
            val text = recognizer.process(inputImage).await()
            ocrTimeMs = System.currentTimeMillis() - start
            text
        }

        Log.i("REAL_INVOICE_TEST", "=== INVOICE A (M.STEEL) OCR COMPLETE in ${ocrTimeMs}ms ===")
        Log.i("REAL_INVOICE_TEST", "Blocks recognized: ${mlKitText.textBlocks.size}")

        val ocrResult = OcrResult(
            rawText = mlKitText.text,
            blocks = mlKitText.textBlocks.map { block ->
                OcrTextBlock(
                    blockText = block.text,
                    lines = block.lines.map { line ->
                        OcrTextLine(
                            lineText = line.text,
                            elements = line.elements.map { it.text },
                            boundingBox = line.boundingBox.toOcrBoundingBox()
                        )
                    },
                    boundingBox = block.boundingBox.toOcrBoundingBox()
                )
            },
            processingMs = ocrTimeMs,
            imageWidthPx = bitmap.width,
            imageHeightPx = bitmap.height,
            rotationDegrees = 0
        )

        val parseTimeMs = measureTimeMillis {
            val parsed = parser.parse(ocrResult)

            Log.i("REAL_INVOICE_TEST", "Supplier: ${parsed.supplierName}")
            Log.i("REAL_INVOICE_TEST", "Invoice No: ${parsed.invoiceNumber}, Date: ${parsed.invoiceDate}")
            Log.i("REAL_INVOICE_TEST", "Supplier GSTIN: ${parsed.supplierGstin}")
            Log.i("REAL_INVOICE_TEST", "Items Extracted: ${parsed.items.size}")
            Log.i("REAL_INVOICE_TEST", "Total Quantity: ${parsed.totalQuantityExtracted}, Reconciled: ${parsed.isQuantityReconciled}")

            assertEquals(InvoiceLayoutType.MSTEEL_MBW, parsed.layoutType)
            assertEquals("M.STEEL / MBW", parsed.supplierName)
            assertEquals("466", parsed.invoiceNumber)
            assertEquals(11, parsed.items.size)
            assertEquals(126.0, parsed.items.mapNotNull { it.quantity }.sum(), 0.001)
            assertTrue("Quantity must reconcile to 126.0", parsed.isQuantityReconciled)
            assertNull("M.Steel invoice has no manufacturer item codes", parsed.items[0].itemCode)
        }

        Log.i("REAL_INVOICE_TEST", "Table Parsing time: ${parseTimeMs}ms")
    }

    @Test
    fun testRealInvoiceB_StryderBicycles_OnDevice(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("invoice_B.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Bitmap decoding should succeed", bitmap)

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        var ocrTimeMs = 0L
        val mlKitText = measureTimeMillis {
            // Processing on device
        }.let {
            val start = System.currentTimeMillis()
            val text = recognizer.process(inputImage).await()
            ocrTimeMs = System.currentTimeMillis() - start
            text
        }

        Log.i("REAL_INVOICE_TEST", "=== INVOICE B (STRYDER) OCR COMPLETE in ${ocrTimeMs}ms ===")
        Log.i("REAL_INVOICE_TEST", "Blocks recognized: ${mlKitText.textBlocks.size}")

        val ocrResult = OcrResult(
            rawText = mlKitText.text,
            blocks = mlKitText.textBlocks.map { block ->
                OcrTextBlock(
                    blockText = block.text,
                    lines = block.lines.map { line ->
                        OcrTextLine(
                            lineText = line.text,
                            elements = line.elements.map { it.text },
                            boundingBox = line.boundingBox.toOcrBoundingBox()
                        )
                    },
                    boundingBox = block.boundingBox.toOcrBoundingBox()
                )
            },
            processingMs = ocrTimeMs,
            imageWidthPx = bitmap.width,
            imageHeightPx = bitmap.height,
            rotationDegrees = 0
        )

        val parseTimeMs = measureTimeMillis {
            val parsed = parser.parse(ocrResult)

            Log.i("REAL_INVOICE_TEST", "Supplier: ${parsed.supplierName}")
            Log.i("REAL_INVOICE_TEST", "Invoice No: ${parsed.invoiceNumber}, Date: ${parsed.invoiceDate}")
            Log.i("REAL_INVOICE_TEST", "Supplier GSTIN: ${parsed.supplierGstin}")
            Log.i("REAL_INVOICE_TEST", "Items Extracted: ${parsed.items.size}")
            Log.i("REAL_INVOICE_TEST", "Total Quantity: ${parsed.totalQuantityExtracted}, Reconciled: ${parsed.isQuantityReconciled}")

            assertEquals(InvoiceLayoutType.BICYCLE_SUPPLIER, parsed.layoutType)
            assertEquals("Stryder Cycle Private Limited (Ludhiana)", parsed.supplierName)
            assertTrue("Invoice/Order number should be extracted", parsed.invoiceNumber != null && (parsed.invoiceNumber!!.contains("276820") || parsed.invoiceNumber!!.contains("201124120")))
            assertEquals(29, parsed.items.size)
            assertEquals(62.0, parsed.items.mapNotNull { it.quantity }.sum(), 0.001)
            assertTrue("Quantity must reconcile to 62.0", parsed.isQuantityReconciled)

            // Strict audit: Row 1 must have Qty 1.0 and Amount 5800.00 (NOT 2.0 or 11600.00)
            val row1 = parsed.items[0]
            assertEquals("6005008229", row1.itemCode)
            assertEquals(1.0, row1.quantity ?: 0.0, 0.001)
            assertEquals(580000L, row1.unitRatePaise)
            assertEquals(580000L, row1.lineAmountPaise)

            // Strict audit: Row 29 must be extracted
            val row29 = parsed.items[28]
            assertEquals("6012000265", row29.itemCode)
            assertEquals(2.0, row29.quantity ?: 0.0, 0.001)
            assertEquals(337500L, row29.unitRatePaise)
            assertEquals(675000L, row29.lineAmountPaise)
        }

        Log.i("REAL_INVOICE_TEST", "Table Parsing time: ${parseTimeMs}ms")
    }
}
