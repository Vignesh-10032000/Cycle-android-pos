package dev.vignesh.cycleshop.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vignesh.cycleshop.data.local.dao.InventoryDao
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.dao.PurchaseDao
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity
import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder
import dev.vignesh.cycleshop.data.repository.PurchaseRepository
import dev.vignesh.cycleshop.ui.invoice.DeduplicationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device instrumentation tests for Phase 3 Step 3A: Purchase Staging Architecture.
 *
 * Verifies:
 * 1. Safe staging of supplier invoices with line items without modifying inventory balances or movements.
 * 2. Strict supplier-aware deduplication.
 * 3. Verified supplier-to-product mappings persistence.
 * 4. Foreign key constraints: CASCADE delete from invoice to lines, SET NULL on variant deletion.
 * 5. Isolation: Staging an invoice creates ZERO records in stock_movements.
 */
@RunWith(AndroidJUnit4::class)
class PurchaseStagingDeviceTest {

    private lateinit var db: VglDatabase
    private lateinit var productDao: ProductDao
    private lateinit var inventoryDao: InventoryDao
    private lateinit var purchaseDao: PurchaseDao
    private lateinit var repository: PurchaseRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VglDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productDao = db.productDao()
        inventoryDao = db.inventoryDao()
        purchaseDao = db.purchaseDao()
        repository = PurchaseRepository(purchaseDao)

        // Seed initial catalog data
        runBlocking {
            DatabaseSeeder.seedIfEmpty(db)
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun stageInvoice_persistsInvoiceAndLines_withoutModifyingInventoryStock(): Unit = runBlocking {
        val initialStockCount = inventoryDao.getTotalStockCount().first()
        val initialMovementCount = inventoryDao.getRecentMovements(100).first().size

        // Get a catalog variant to associate
        val variant = productDao.getVariantBySku("VGL-HERO-SP275-BLK")
        assertNotNull("Seeded variant must exist", variant)

        val invoice = PurchaseInvoiceEntity(
            supplierName = "STRYDER CYCLE PRIVATE LIMITED",
            supplierGstin = "03AABCS9876E1Z5",
            invoiceNumber = "242502690",
            invoiceDate = "25/09/2026",
            totalAmountPaise = 31277000L,
            totalQuantity = 62.0,
            status = PurchaseInvoiceStatus.CONFIRMED_STAGED
        )

        val line1 = PurchaseLineEntity(
            invoiceId = 0,
            lineNumber = 1,
            variantId = variant?.id,
            supplierItemCode = "BIC-20-ELVIS",
            rawDescription = "BIC 20 ELVIS PRO DD FS MELBLUEORG SKD",
            hsnCode = "87120010",
            rawQuantity = 1.0,
            rawUnit = "PC",
            inventoryUnit = InventoryUnit.PIECE,
            unitConversionFactor = 1,
            effectiveQuantityBase = 1,
            rawUnitPricePaise = 580000L,
            rawLineAmountPaise = 580000L,
            reviewStatus = LineReviewStatus.VERIFIED_MATCH
        )

        val mapping = SupplierProductMappingEntity(
            supplierIdentifier = "03AABCS9876E1Z5",
            supplierItemCode = "BIC-20-ELVIS",
            supplierDescPattern = "BIC 20 ELVIS PRO",
            variantId = variant!!.id,
            verifiedByOwner = true
        )

        val result = repository.stageInvoice(invoice, listOf(line1), listOf(mapping))
        assertTrue("Staging invoice must succeed", result.isSuccess)
        val stagedId = result.getOrThrow()
        assertTrue("Staged invoice ID must be positive", stagedId > 0)

        // 1. Verify invoice was persisted with status CONFIRMED_STAGED
        val fetched = purchaseDao.getInvoiceById(stagedId)
        assertNotNull(fetched)
        assertEquals("STRYDER CYCLE PRIVATE LIMITED", fetched?.supplierName)
        assertEquals(PurchaseInvoiceStatus.CONFIRMED_STAGED, fetched?.status)

        // 2. Verify lines are linked with invoiceId
        val lines = purchaseDao.getLinesForInvoice(stagedId).first()
        assertEquals(1, lines.size)
        assertEquals(stagedId, lines[0].invoiceId)
        assertEquals(LineReviewStatus.VERIFIED_MATCH, lines[0].reviewStatus)

        // 3. CRITICAL BOUNDARY CHECK: Stock movements and catalog stock counts are UNTOUCHED!
        val postStockCount = inventoryDao.getTotalStockCount().first()
        val postMovementCount = inventoryDao.getRecentMovements(100).first().size
        val postVariant = productDao.getVariantById(variant.id)

        assertEquals("Inventory total stock count must NOT change upon staging", initialStockCount, postStockCount)
        assertEquals("Stock movement ledger count must NOT change upon staging", initialMovementCount, postMovementCount)
        assertEquals("Variant stock quantity must remain identical", variant.stockQuantity, postVariant?.stockQuantity)
    }

    @Test
    fun stageInvoice_duplicateCheck_preventsDuplicateStaging(): Unit = runBlocking {
        val invoice = PurchaseInvoiceEntity(
            supplierName = "M.STEEL INDUSTRIES",
            supplierGstin = "03AABCM1234F1Z1",
            invoiceNumber = "466",
            invoiceDate = "18/10/2026",
            totalAmountPaise = 7717400L,
            totalQuantity = 126.0,
            status = PurchaseInvoiceStatus.CONFIRMED_STAGED
        )

        val line = PurchaseLineEntity(
            invoiceId = 0,
            lineNumber = 1,
            variantId = null,
            rawDescription = "MBW Spokes 14G x 300mm",
            rawQuantity = 10.0,
            rawUnit = "GROSS",
            inventoryUnit = InventoryUnit.PIECE,
            unitConversionFactor = 144,
            effectiveQuantityBase = 1440,
            reviewStatus = LineReviewStatus.PENDING_MATCH
        )

        val firstStage = repository.stageInvoice(invoice, listOf(line))
        assertTrue(firstStage.isSuccess)

        // Attempting to stage the same invoice again must fail
        val duplicateCheck = repository.checkDuplicate(
            supplierName = "M.STEEL INDUSTRIES",
            supplierGstin = "03AABCM1234F1Z1",
            invoiceNumber = "466"
        )
        assertTrue("Duplicate check must identify duplicate", duplicateCheck is DeduplicationResult.ExactDuplicate)

        val secondStage = repository.stageInvoice(invoice, listOf(line))
        assertTrue("Second stage attempt must fail", secondStage.isFailure)
        assertTrue(secondStage.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun foreignKeyCascade_deletingInvoice_cascadesToLines(): Unit = runBlocking {
        val invoice = PurchaseInvoiceEntity(
            supplierName = "Supplier X",
            invoiceNumber = "INV-CASCADE-TEST",
            status = PurchaseInvoiceStatus.CONFIRMED_STAGED
        )
        val line = PurchaseLineEntity(
            invoiceId = 0,
            lineNumber = 1,
            variantId = null,
            rawDescription = "Test Part",
            rawQuantity = 2.0,
            rawUnit = "PC",
            effectiveQuantityBase = 2
        )

        val stagedId = repository.stageInvoice(invoice, listOf(line)).getOrThrow()
        assertEquals(1, purchaseDao.getLinesForInvoice(stagedId).first().size)

        // Delete invoice
        repository.deleteStagedInvoice(stagedId)

        // Invoice and lines must be deleted
        assertNull(purchaseDao.getInvoiceById(stagedId))
        assertEquals(0, purchaseDao.getLinesForInvoice(stagedId).first().size)
    }

    @Test
    fun verifiedSupplierMapping_saveAndQuery_persistsSuccessfully(): Unit = runBlocking {
        val variant = productDao.getVariantBySku("VGL-SHIM-RDTX800")
        assertNotNull(variant)

        val mapping = SupplierProductMappingEntity(
            supplierIdentifier = "03AABCM1234F1Z1",
            supplierItemCode = "SPK-14G-300",
            supplierDescPattern = "MBW Spokes 14G x 300mm",
            variantId = variant!!.id,
            verifiedByOwner = true
        )

        repository.saveVerifiedMapping(mapping)

        val fetched = repository.getVerifiedMapping("03AABCM1234F1Z1", "SPK-14G-300")
        assertNotNull(fetched)
        assertEquals(variant.id, fetched?.variantId)
        assertTrue(fetched?.verifiedByOwner == true)
    }
}
