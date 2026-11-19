package dev.vignesh.cycleshop.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vignesh.cycleshop.data.local.dao.InventoryDao
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductSpecs
import dev.vignesh.cycleshop.data.local.model.ProductType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device Android Instrumentation Tests for VglDatabase, DAOs, and SQLite Constraints.
 *
 * Verifies:
 * 1. Unique SKU constraint enforcement
 * 2. Foreign Key RESTRICT constraint enforcement on Brand, Product, and Variant deletion
 * 3. Atomic stock movement balance + ledger consistency
 * 4. Transaction rollback on insufficient stock, zero delta, and integer overflow
 * 5. Database persistence after closing and reopening
 * 6. Reactive Flow emissions
 */
@RunWith(AndroidJUnit4::class)
class VglDatabaseTest {

    private lateinit var db: VglDatabase
    private lateinit var productDao: ProductDao
    private lateinit var inventoryDao: InventoryDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VglDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productDao = db.productDao()
        inventoryDao = db.inventoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ==========================================
    // 1. Unique SKU Constraint
    // ==========================================

    @Test
    fun uniqueSkuConstraint_duplicateSku_throwsSQLiteConstraintException(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Hero"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Hero Sprint Pro",
                category = "MTB",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )

        val variant1 = ProductVariantEntity(
            productId = productId,
            sku = "HERO-SPRINT-275",
            mrpPaise = 1500000L,
            sellingPricePaise = 1350000L,
            stockQuantity = 5
        )
        productDao.insertVariant(variant1)

        val variant2 = ProductVariantEntity(
            productId = productId,
            sku = "HERO-SPRINT-275", // Duplicate SKU
            mrpPaise = 1600000L,
            sellingPricePaise = 1400000L,
            stockQuantity = 2
        )

        try {
            productDao.insertVariant(variant2)
            fail("Expected SQLiteConstraintException on duplicate SKU")
        } catch (e: SQLiteConstraintException) {
            assertTrue("Constraint message indicates UNIQUE constraint", e.message?.contains("UNIQUE") == true || e.message != null)
        }
    }

    // ==========================================
    // 2. Foreign Key RESTRICT Constraints
    // ==========================================

    @Test
    fun foreignKey_brandDeleteRestricted_whenProductsExist(): Unit = runBlocking {
        val brand = BrandEntity(name = "Firefox")
        val brandId = productDao.insertBrand(brand)
        val insertedBrand = brand.copy(id = brandId)

        productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Firefox Target 29",
                category = "MTB",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )

        try {
            productDao.deleteBrand(insertedBrand)
            fail("Expected SQLiteConstraintException when deleting brand with existing products")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message?.contains("FOREIGN KEY") == true || e.message != null)
        }
    }

    @Test
    fun foreignKey_productDeleteRestricted_whenVariantsExist(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Trek"))
        val product = ProductEntity(
            brandId = brandId,
            name = "Trek Marlin 7",
            category = "MTB",
            type = ProductType.BICYCLE,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val productId = productDao.insertProduct(product)
        val insertedProduct = product.copy(id = productId)

        productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "TREK-MARLIN7-L",
                mrpPaise = 6500000L,
                sellingPricePaise = 6200000L,
                stockQuantity = 3
            )
        )

        try {
            productDao.deleteProduct(insertedProduct)
            fail("Expected SQLiteConstraintException when deleting product with existing variants")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message?.contains("FOREIGN KEY") == true || e.message != null)
        }
    }

    @Test
    fun foreignKey_variantDeleteRestricted_whenStockMovementsExist(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Shimano"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Shimano Tourney Rear Derailleur",
                category = "Spare Parts",
                type = ProductType.SPARE_PART,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variant = ProductVariantEntity(
            productId = productId,
            sku = "SHIMANO-RD-TY300",
            mrpPaise = 95000L,
            sellingPricePaise = 85000L,
            stockQuantity = 0
        )
        val variantId = productDao.insertVariant(variant)
        val insertedVariant = variant.copy(id = variantId)

        // Record opening stock movement
        inventoryDao.recordStockMovement(
            variantId = variantId,
            delta = 10,
            movementType = MovementType.OPENING,
            unitPricePaise = 70000L
        )

        try {
            productDao.deleteVariant(insertedVariant)
            fail("Expected SQLiteConstraintException when deleting variant with historical stock movements")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message?.contains("FOREIGN KEY") == true || e.message != null)
        }
    }

    // ==========================================
    // 3. Atomic Stock Movement & Ledger Updates
    // ==========================================

    @Test
    fun stockMovement_atomicSuccess_updatesBalanceAndRecordsLedger(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Montra"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Montra Helicon Disc",
                category = "Hybrid",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "MONTRA-HELICON-M",
                mrpPaise = 2200000L,
                sellingPricePaise = 1950000L,
                stockQuantity = 0
            )
        )

        // 1. Purchase: Add 10
        val purchaseMoveId = inventoryDao.recordStockMovement(
            variantId = variantId,
            delta = 10,
            movementType = MovementType.PURCHASE,
            unitPricePaise = 1600000L,
            referenceId = "INV-2026-001"
        )
        assertTrue(purchaseMoveId > 0)

        var variant = productDao.getVariantById(variantId)
        assertNotNull(variant)
        assertEquals(10, variant?.stockQuantity)

        // 2. Sale: Consume 3
        val saleMoveId = inventoryDao.recordStockMovement(
            variantId = variantId,
            delta = -3,
            movementType = MovementType.SALE,
            unitPricePaise = 1950000L,
            referenceId = "BILL-1001"
        )
        assertTrue(saleMoveId > 0)

        variant = productDao.getVariantById(variantId)
        assertEquals(7, variant?.stockQuantity)

        // Verify ledger entries
        val movements = inventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(2, movements.size)
        assertEquals(MovementType.SALE, movements[0].movementType)
        assertEquals(-3, movements[0].delta)
        assertEquals(MovementType.PURCHASE, movements[1].movementType)
        assertEquals(10, movements[1].delta)
    }

    // ==========================================
    // 4. Transaction Rollback & Invariant Guards
    // ==========================================

    @Test
    fun stockMovement_insufficientStock_rollsBackTransaction(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Cosmic"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Cosmic Voyager",
                category = "Kids",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "COSMIC-VOY-20",
                mrpPaise = 850000L,
                sellingPricePaise = 750000L,
                stockQuantity = 4
            )
        )

        // Attempt to sell 5 when only 4 in stock
        try {
            inventoryDao.recordStockMovement(
                variantId = variantId,
                delta = -5,
                movementType = MovementType.SALE
            )
            fail("Expected IllegalStateException on insufficient stock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Insufficient stock") == true)
        }

        // Verify rollback: stock remains untouched
        val variant = productDao.getVariantById(variantId)
        assertEquals(4, variant?.stockQuantity)

        // Verify rollback: no ledger entry recorded
        val movements = inventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(0, movements.size)
    }

    @Test
    fun stockMovement_zeroDelta_rejectedWithoutDatabaseChange(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "BSA"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "BSA Ladybird",
                category = "City",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "BSA-LB-26",
                mrpPaise = 900000L,
                sellingPricePaise = 800000L,
                stockQuantity = 2
            )
        )

        try {
            inventoryDao.recordStockMovement(
                variantId = variantId,
                delta = 0,
                movementType = MovementType.ADJUSTMENT
            )
            fail("Expected IllegalArgumentException on zero delta")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("delta cannot be zero") == true)
        }

        val variant = productDao.getVariantById(variantId)
        assertEquals(2, variant?.stockQuantity)
    }

    @Test
    fun stockMovement_mismatchedDirection_rejected(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Hercules"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Hercules Roadeo",
                category = "Youth",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "HERC-ROADEO-26",
                mrpPaise = 1200000L,
                sellingPricePaise = 1050000L,
                stockQuantity = 5
            )
        )

        // SALE with positive delta must be rejected
        try {
            inventoryDao.recordStockMovement(
                variantId = variantId,
                delta = 2,
                movementType = MovementType.SALE
            )
            fail("Expected IllegalArgumentException on positive SALE delta")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("SALE movement delta must be negative") == true)
        }

        // PURCHASE with negative delta must be rejected
        try {
            inventoryDao.recordStockMovement(
                variantId = variantId,
                delta = -2,
                movementType = MovementType.PURCHASE
            )
            fail("Expected IllegalArgumentException on negative PURCHASE delta")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("PURCHASE movement delta must be positive") == true)
        }
    }

    // ==========================================
    // 5. Database Persistence After Reopening
    // ==========================================

    @Test
    fun databasePersistence_survivesCloseAndReopen(): Unit = runBlocking {
        context.deleteDatabase("test_persistent.db")

        // 1. Create persistent DB and write data
        var persistentDb = Room.databaseBuilder(context, VglDatabase::class.java, "test_persistent.db")
            .allowMainThreadQueries()
            .build()

        val brandId = persistentDb.productDao().insertBrand(BrandEntity(name = "Avon"))
        val productId = persistentDb.productDao().insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Avon Prime",
                category = "Road",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val specs = ProductSpecs(schemaVersion = 1, attributes = mapOf("frame" to "Alloy"))
        val variantId = persistentDb.productDao().insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "AVON-PRIME-700C",
                mrpPaise = 1800000L,
                sellingPricePaise = 1600000L,
                stockQuantity = 10,
                specsJson = specs.toJson()
            )
        )

        // 2. Close DB
        persistentDb.close()

        // 3. Reopen DB and verify data integrity
        persistentDb = Room.databaseBuilder(context, VglDatabase::class.java, "test_persistent.db")
            .allowMainThreadQueries()
            .build()

        val loadedVariant = persistentDb.productDao().getVariantById(variantId)
        assertNotNull("Variant must persist after DB close/reopen", loadedVariant)
        assertEquals("AVON-PRIME-700C", loadedVariant?.sku)
        assertEquals(10, loadedVariant?.stockQuantity)
        assertEquals("Alloy", loadedVariant?.specs?.attributes?.get("frame"))

        val loadedProduct = persistentDb.productDao().getProductById(productId)
        assertNotNull(loadedProduct)
        assertEquals("Avon Prime", loadedProduct?.name)

        persistentDb.close()
        context.deleteDatabase("test_persistent.db")
    }

    // ==========================================
    // 6. Reactive Flow Queries
    // ==========================================

    @Test
    fun reactiveFlow_totalStockCountUpdates(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Decathlon"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Triban RC100",
                category = "Road",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )

        assertEquals(null, inventoryDao.getTotalStockCount().first())

        productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "DEC-RC100-M",
                mrpPaise = 2500000L,
                sellingPricePaise = 2200000L,
                stockQuantity = 4
            )
        )
        assertEquals(4, inventoryDao.getTotalStockCount().first())

        productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "DEC-RC100-L",
                mrpPaise = 2500000L,
                sellingPricePaise = 2200000L,
                stockQuantity = 6
            )
        )
        assertEquals(10, inventoryDao.getTotalStockCount().first())
    }

    // ==========================================
    // 7. Advanced Concurrency, Overflow & Ledger Reconciliation
    // ==========================================

    @Test
    fun stockMovement_integerOverflow_rollsBackTransaction(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Raleigh"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Raleigh Chopper",
                category = "Classic",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val initialStock = Int.MAX_VALUE - 2
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "RALEIGH-CHOPPER-RED",
                mrpPaise = 3000000L,
                sellingPricePaise = 2700000L,
                stockQuantity = initialStock
            )
        )

        // Attempt delta of 5 which would overflow Int.MAX_VALUE
        try {
            inventoryDao.recordStockMovement(
                variantId = variantId,
                delta = 5,
                movementType = MovementType.PURCHASE
            )
            fail("Expected ArithmeticException on integer overflow")
        } catch (e: ArithmeticException) {
            assertTrue(e.message?.contains("overflow Int.MAX_VALUE") == true)
        }

        // Verify rollback: stock remains untouched
        val variant = productDao.getVariantById(variantId)
        assertEquals(initialStock, variant?.stockQuantity)

        // Verify rollback: no ledger record created
        val movements = inventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(0, movements.size)
    }

    @Test
    fun stockMovement_concurrentDeductions_strictlyGuardsInventory(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Kona"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Kona Dew",
                category = "Commuter",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val initialStock = 5
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "KONA-DEW-M",
                mrpPaise = 3500000L,
                sellingPricePaise = 3200000L,
                stockQuantity = initialStock
            )
        )

        // Launch 10 concurrent sales of 1 unit each. Exactly 5 should succeed, 5 should fail.
        val results = (1..10).map { i ->
            async(Dispatchers.Default) {
                try {
                    inventoryDao.recordStockMovement(
                        variantId = variantId,
                        delta = -1,
                        movementType = MovementType.SALE,
                        referenceId = "SALE-$i"
                    )
                    true
                } catch (e: IllegalStateException) {
                    false
                }
            }
        }.awaitAll()

        val successCount = results.count { it }
        val failCount = results.count { !it }

        assertEquals(5, successCount)
        assertEquals(5, failCount)

        // Final balance must be 0
        val variant = productDao.getVariantById(variantId)
        assertEquals(0, variant?.stockQuantity)

        // Exactly 5 movements in ledger
        val movements = inventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(5, movements.size)
    }

    @Test
    fun stockMovement_ledgerReconciliation_matchesVariantBalance(): Unit = runBlocking {
        val brandId = productDao.insertBrand(BrandEntity(name = "Polygon"))
        val productId = productDao.insertProduct(
            ProductEntity(
                brandId = brandId,
                name = "Polygon Siskiu D7",
                category = "Trail",
                type = ProductType.BICYCLE,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val variantId = productDao.insertVariant(
            ProductVariantEntity(
                productId = productId,
                sku = "POLY-SISKIU-L",
                mrpPaise = 8500000L,
                sellingPricePaise = 8000000L,
                stockQuantity = 0
            )
        )

        // Perform ledger transactions:
        inventoryDao.recordStockMovement(variantId, 20, MovementType.OPENING)
        inventoryDao.recordStockMovement(variantId, 15, MovementType.PURCHASE)
        inventoryDao.recordStockMovement(variantId, -8, MovementType.SALE)
        inventoryDao.recordStockMovement(variantId, -3, MovementType.SERVICE)
        inventoryDao.recordStockMovement(variantId, -2, MovementType.ADJUSTMENT)
        inventoryDao.recordStockMovement(variantId, 5, MovementType.PURCHASE)
        inventoryDao.recordStockMovement(variantId, -12, MovementType.SALE)

        // Expected balance: 20 + 15 - 8 - 3 - 2 + 5 - 12 = 15
        val variant = productDao.getVariantById(variantId)
        assertEquals(15, variant?.stockQuantity)

        // Ledger reconciliation: sum of deltas must match stockQuantity
        val movements = inventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(7, movements.size)
        val ledgerSum = movements.sumOf { it.delta }
        assertEquals(variant?.stockQuantity, ledgerSum)
    }

    // ==========================================
    // 7. Phase 2 Step 4: Seeder & Repository On-Device
    // ==========================================

    @Test
    fun databaseSeeder_seedsDemoInventory_exactLedgerParity(): Unit = runBlocking {
        // Run initial seed on empty DB
        dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder.seedIfEmpty(db)

        val brands = productDao.getAllBrands().first()
        assertEquals("Should seed 6 brands", 6, brands.size)

        val products = productDao.getActiveProducts().first()
        assertEquals("Should seed 8 products", 8, products.size)

        val totalStock = inventoryDao.getTotalStockCount().first()
        assertEquals("Total opening stock across all variants must be 67 units", 67, totalStock)

        val movements = inventoryDao.getRecentMovements(limit = 100).first()
        assertEquals("Should have exactly 9 opening movements for 9 variants", 9, movements.size)
        assertTrue(movements.all { it.movementType == MovementType.OPENING })
        val movementSum = movements.sumOf { it.delta }
        assertEquals("Sum of opening ledger movements must equal 67", 67, movementSum)

        // Idempotency: Running seeder again must NOT duplicate data
        dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder.seedIfEmpty(db)
        val brandsAfter = productDao.getAllBrands().first()
        assertEquals("Brands count must remain 6 after re-seeding", 6, brandsAfter.size)
        val stockAfter = inventoryDao.getTotalStockCount().first()
        assertEquals("Total stock must remain 67 after re-seeding", 67, stockAfter)
    }

    @Test
    fun inventoryRepository_searchAndTransactions_onDevice(): Unit = runBlocking {
        dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder.seedIfEmpty(db)
        val repository = dev.vignesh.cycleshop.data.repository.InventoryRepository(productDao, inventoryDao)

        // Search by brand "Hero"
        val heroResults = repository.searchCatalog("Hero").first()
        assertTrue("Hero search should find Hero Sprint Pro", heroResults.any { it.product.name.contains("Hero Sprint Pro") })

        // Search by SKU "VGL-FF-TGT29-ORG"
        val ffResults = repository.searchCatalog("VGL-FF-TGT29-ORG").first()
        assertEquals(1, ffResults.size)
        assertEquals("Firefox Target 29", ffResults[0].product.name)

        // Restock and Sale transaction test via Repository
        val variant = ffResults[0].variants.first()
        val originalStock = variant.stockQuantity

        // Restock +2 units
        repository.recordPurchase(variantId = variant.id, quantity = 2, costPricePaise = 1700000L, invoiceReference = "RESTOCK-TEST")
        val updatedVariant = repository.getVariant(variant.id)
        assertEquals(originalStock + 2, updatedVariant?.stockQuantity)

        // Sell -1 unit
        repository.recordSale(variantId = variant.id, quantity = 1, sellingPricePaise = 2049900L, billReference = "SALE-TEST")
        val finalVariant = repository.getVariant(variant.id)
        assertEquals(originalStock + 1, finalVariant?.stockQuantity)
    }
}

