package dev.vignesh.cycleshop

import dev.vignesh.cycleshop.data.local.dao.InventoryDao
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class InventoryRepositoryTest {

    private lateinit var repository: InventoryRepository
    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeInventoryDao: FakeInventoryDao

    @Before
    fun setUp() {
        val variantsMap = mutableMapOf<Long, ProductVariantEntity>()
        fakeProductDao = FakeProductDao(variantsMap)
        fakeInventoryDao = FakeInventoryDao(variantsMap)
        repository = InventoryRepository(fakeProductDao, fakeInventoryDao)
    }

    @Test
    fun testRegisterNewProductAndStock(): Unit = runBlocking {
        val variantId = repository.registerScannedInvoiceItem(
            brandName = "Hero",
            productName = "Hero Sprint 27.5T",
            category = "Bicycle",
            type = ProductType.BICYCLE,
            sku = "HERO-275-NG",
            supplierCode = "SUP-HERO-01",
            size = "27.5T",
            color = "Neon Green",
            mrpPaise = 1400000L,
            costPricePaise = 1000000L,
            sellingPricePaise = 1250000L,
            quantity = 5,
            invoiceReference = "INV-001"
        )

        val variant = repository.getVariant(variantId)
        assertNotNull("Registered variant should exist", variant)
        assertEquals("HERO-275-NG", variant?.sku)
        assertEquals(5, variant?.stockQuantity)

        val totalStock = repository.totalUnitsInStock.first()
        assertEquals(5, totalStock)

        val movements = fakeInventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(1, movements.size)
        assertEquals(MovementType.PURCHASE, movements[0].movementType)
        assertEquals(5, movements[0].delta)
    }

    @Test
    fun testDuplicatePreventionBySupplierCode(): Unit = runBlocking {
        // First scan
        repository.registerScannedInvoiceItem(
            brandName = "Firefox",
            productName = "Firefox Meteor",
            category = "Bicycle",
            type = ProductType.BICYCLE,
            sku = "FFX-MET-01",
            supplierCode = "SUP-FFX-MET",
            size = "29T",
            color = "Black",
            mrpPaise = 2000000L,
            costPricePaise = 1500000L,
            sellingPricePaise = 1800000L,
            quantity = 2,
            invoiceReference = "INV-100"
        )

        // Second scan of the same supplier code: restock by 3
        repository.registerScannedInvoiceItem(
            brandName = "Firefox",
            productName = "Firefox Meteor",
            category = "Bicycle",
            type = ProductType.BICYCLE,
            sku = "FFX-MET-01",
            supplierCode = "SUP-FFX-MET",
            size = "29T",
            color = "Black",
            mrpPaise = 2000000L,
            costPricePaise = 1500000L,
            sellingPricePaise = 1800000L,
            quantity = 3,
            invoiceReference = "INV-101"
        )

        val variants = fakeProductDao.getVariantsBySupplierCode("SUP-FFX-MET")
        assertEquals("Only one variant should exist for the supplier code", 1, variants.size)
        assertEquals(5, variants[0].stockQuantity)

        val movements = fakeInventoryDao.getMovementsForVariant(variants[0].id).first()
        assertEquals(2, movements.size)
    }

    @Test
    fun testStockSaleAndDeduction(): Unit = runBlocking {
        val variantId = repository.registerScannedInvoiceItem(
            brandName = "Shimano",
            productName = "Shimano Brake Pad",
            category = "Spare Part",
            type = ProductType.SPARE_PART,
            sku = "SHI-BP-01",
            supplierCode = "SUP-SHI-01",
            size = "Standard",
            color = "Black",
            mrpPaise = 50000L,
            costPricePaise = 30000L,
            sellingPricePaise = 45000L,
            quantity = 10,
            invoiceReference = "INV-200"
        )

        // Customer purchase of 4 units
        repository.recordSale(
            variantId = variantId,
            quantity = 4,
            sellingPricePaise = 45000L,
            billReference = "BILL-001"
        )

        var variant = repository.getVariant(variantId)
        assertEquals(6, variant?.stockQuantity)

        // Attempt overdraft of 10 units when only 6 remain
        try {
            repository.recordSale(
                variantId = variantId,
                quantity = 10,
                sellingPricePaise = 45000L,
                billReference = "BILL-002"
            )
            fail("Overdraft sale must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Insufficient stock") == true)
        }

        // Verify balance remained unchanged at 6
        variant = repository.getVariant(variantId)
        assertEquals(6, variant?.stockQuantity)
    }

    @Test
    fun testSearchCatalog_matchesBrandProductSkuSizeColor(): Unit = runBlocking {
        repository.registerScannedInvoiceItem(
            brandName = "Montra",
            productName = "Montra Helicon Disc",
            category = "Hybrid",
            type = ProductType.BICYCLE,
            sku = "MON-HELI-DISC-M",
            supplierCode = "SUP-MON-01",
            size = "700x35C",
            color = "Gunmetal Grey",
            mrpPaise = 2400000L,
            costPricePaise = 1850000L,
            sellingPricePaise = 2199900L,
            quantity = 4,
            invoiceReference = "INV-300"
        )

        // Search by brand
        val brandSearch = repository.searchCatalog("montra").first()
        assertEquals(1, brandSearch.size)

        // Search by product name
        val nameSearch = repository.searchCatalog("Helicon").first()
        assertEquals(1, nameSearch.size)

        // Search by SKU
        val skuSearch = repository.searchCatalog("HELI-DISC").first()
        assertEquals(1, skuSearch.size)

        // Search by size
        val sizeSearch = repository.searchCatalog("700x35C").first()
        assertEquals(1, sizeSearch.size)

        // Search by color
        val colorSearch = repository.searchCatalog("Gunmetal").first()
        assertEquals(1, colorSearch.size)

        // Unmatched search
        val emptySearch = repository.searchCatalog("NonExistentBike").first()
        assertEquals(0, emptySearch.size)
    }

    @Test
    fun testLedgerReconciliation_matchesStockBalance(): Unit = runBlocking {
        val variantId = repository.registerScannedInvoiceItem(
            brandName = "Hercules",
            productName = "Hercules Roadeo",
            category = "Youth",
            type = ProductType.BICYCLE,
            sku = "HERC-RD-01",
            supplierCode = "SUP-HERC-01",
            size = "26T",
            color = "Yellow",
            mrpPaise = 1350000L,
            costPricePaise = 1000000L,
            sellingPricePaise = 1220000L,
            quantity = 15,
            invoiceReference = "INV-400"
        )

        repository.recordSale(variantId, 3, 1220000L, "BILL-10")
        repository.recordSale(variantId, 2, 1220000L, "BILL-11")
        repository.recordPurchase(variantId, 5, 1000000L, "INV-401")

        // Expected balance: 15 - 3 - 2 + 5 = 15
        val variant = repository.getVariant(variantId)
        assertEquals(15, variant?.stockQuantity)

        val movements = fakeInventoryDao.getMovementsForVariant(variantId).first()
        assertEquals(4, movements.size)
        val ledgerSum = movements.sumOf { it.delta }
        assertEquals(variant?.stockQuantity, ledgerSum)
    }

    // ==========================================
    // Test Doubles (In-Memory DAOs for JVM Unit Tests)
    // ==========================================

    class FakeProductDao(
        private val variantsMap: MutableMap<Long, ProductVariantEntity>
    ) : ProductDao {
        val brands = mutableListOf<BrandEntity>()
        val products = mutableListOf<ProductEntity>()
        private val brandsFlow = MutableStateFlow<List<BrandEntity>>(emptyList())
        private val productsFlow = MutableStateFlow<List<ProductEntity>>(emptyList())

        override suspend fun insertBrand(brand: BrandEntity): Long {
            val id = (brands.size + 1).toLong()
            val saved = brand.copy(id = id)
            brands.add(saved)
            brandsFlow.value = brands.toList()
            return id
        }

        override fun getAllBrands(): Flow<List<BrandEntity>> = brandsFlow

        override suspend fun getBrandById(id: Long): BrandEntity? = brands.find { it.id == id }

        override suspend fun getBrandByName(name: String): BrandEntity? = brands.find { it.name.equals(name, ignoreCase = true) }

        override suspend fun deleteBrand(brand: BrandEntity): Int {
            val removed = brands.removeIf { it.id == brand.id }
            brandsFlow.value = brands.toList()
            return if (removed) 1 else 0
        }

        override suspend fun insertProduct(product: ProductEntity): Long {
            val id = (products.size + 1).toLong()
            val saved = product.copy(id = id)
            products.add(saved)
            productsFlow.value = products.toList()
            return id
        }

        override suspend fun updateProduct(product: ProductEntity): Int {
            val index = products.indexOfFirst { it.id == product.id }
            if (index != -1) {
                products[index] = product
                productsFlow.value = products.toList()
                return 1
            }
            return 0
        }

        override suspend fun deleteProduct(product: ProductEntity): Int {
            val removed = products.removeIf { it.id == product.id }
            productsFlow.value = products.toList()
            return if (removed) 1 else 0
        }

        override fun getActiveProducts(): Flow<List<ProductEntity>> =
            productsFlow.map { list -> list.filter { it.isActive } }

        override fun getAllProducts(): Flow<List<ProductEntity>> = productsFlow

        override suspend fun getProductById(id: Long): ProductEntity? = products.find { it.id == id }

        override fun getProductsByBrand(brandId: Long): Flow<List<ProductEntity>> =
            productsFlow.map { list -> list.filter { it.brandId == brandId && it.isActive } }

        override fun getProductsByType(type: ProductType): Flow<List<ProductEntity>> =
            productsFlow.map { list -> list.filter { it.type == type && it.isActive } }

        override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
            productsFlow.map { list -> list.filter { it.category == category && it.isActive } }

        override suspend fun insertVariant(variant: ProductVariantEntity): Long {
            val id = (variantsMap.size + 1).toLong()
            val saved = variant.copy(id = id)
            variantsMap[id] = saved
            return id
        }

        override suspend fun updateVariantMetadata(
            id: Long,
            supplierCode: String?,
            size: String?,
            color: String?,
            mrpPaise: Long,
            costPricePaise: Long,
            sellingPricePaise: Long,
            specsJson: String?
        ): Int {
            val current = variantsMap[id] ?: return 0
            variantsMap[id] = current.copy(
                supplierCode = supplierCode,
                size = size,
                color = color,
                mrpPaise = mrpPaise,
                costPricePaise = costPricePaise,
                sellingPricePaise = sellingPricePaise,
                specsJson = specsJson
            )
            return 1
        }

        override suspend fun deleteVariant(variant: ProductVariantEntity): Int =
            if (variantsMap.remove(variant.id) != null) 1 else 0

        override suspend fun getVariantById(id: Long): ProductVariantEntity? = variantsMap[id]

        override suspend fun getVariantBySku(sku: String): ProductVariantEntity? =
            variantsMap.values.find { it.sku == sku }

        override suspend fun getVariantsBySupplierCode(supplierCode: String): List<ProductVariantEntity> =
            variantsMap.values.filter { it.supplierCode == supplierCode }

        override fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>> =
            MutableStateFlow(variantsMap.values.filter { it.productId == productId })

        override fun getProductWithVariants(productId: Long): Flow<ProductWithVariants?> =
            productsFlow.map { list ->
                val p = list.find { it.id == productId } ?: return@map null
                ProductWithVariants(p, variantsMap.values.filter { it.productId == productId })
            }

        override fun getActiveProductsWithVariants(): Flow<List<ProductWithVariants>> =
            productsFlow.map { list ->
                list.filter { it.isActive }.map { p ->
                    ProductWithVariants(p, variantsMap.values.filter { it.productId == p.id })
                }
            }
    }

    class FakeInventoryDao(
        private val variantsMap: MutableMap<Long, ProductVariantEntity>
    ) : InventoryDao() {
        val movements = mutableListOf<StockMovementEntity>()
        private val movementsFlow = MutableStateFlow<List<StockMovementEntity>>(emptyList())
        private val totalStockFlow = MutableStateFlow<Int?>(0)

        override suspend fun insertMovement(movement: StockMovementEntity): Long {
            val id = (movements.size + 1).toLong()
            val saved = movement.copy(id = id)
            movements.add(0, saved)
            movementsFlow.value = movements.toList()
            return id
        }

        override suspend fun getVariantById(variantId: Long): ProductVariantEntity? = variantsMap[variantId]

        override suspend fun getStockQuantity(variantId: Long): Int? = variantsMap[variantId]?.stockQuantity

        override suspend fun updateStockQuantityAtomic(variantId: Long, delta: Int): Int {
            val variant = variantsMap[variantId] ?: return 0
            val newStock = variant.stockQuantity + delta
            if (newStock < 0) return 0
            variantsMap[variantId] = variant.copy(stockQuantity = newStock)
            totalStockFlow.value = variantsMap.values.sumOf { it.stockQuantity }
            return 1
        }

        override fun getMovementsForVariant(variantId: Long): Flow<List<StockMovementEntity>> =
            movementsFlow.map { list -> list.filter { it.variantId == variantId } }

        override fun getRecentMovements(limit: Int): Flow<List<StockMovementEntity>> =
            movementsFlow.map { it.take(limit) }

        override fun getTotalStockCount(): Flow<Int?> = totalStockFlow

        override fun getLowStockVariants(threshold: Int): Flow<List<ProductVariantEntity>> =
            MutableStateFlow(variantsMap.values.filter { it.stockQuantity <= threshold })
    }
}
