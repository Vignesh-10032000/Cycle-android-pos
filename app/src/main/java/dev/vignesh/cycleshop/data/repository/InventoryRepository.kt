package dev.vignesh.cycleshop.data.repository

import dev.vignesh.cycleshop.data.local.dao.InventoryDao
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Production Room-backed repository for VGL Cycle Shop catalog and inventory management.
 *
 * Guarantees that:
 * 1. All stock additions, sales, service usages, and adjustments pass strictly through
 *    [InventoryDao.recordStockMovement] inside an atomic SQLite transaction.
 * 2. Every balance change produces an immutable audit record in the stock movements ledger.
 * 3. Search and catalog queries are reactive via Kotlin Coroutines [Flow].
 */
class InventoryRepository(
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao
) {
    // ==========================================
    // Reactive Catalog & Ledger Flows
    // ==========================================

    val allBrands: Flow<List<BrandEntity>> = productDao.getAllBrands()

    val allProducts: Flow<List<ProductEntity>> = productDao.getActiveProducts()

    val activeProductsWithVariants: Flow<List<ProductWithVariants>> =
        productDao.getActiveProductsWithVariants()

    val totalUnitsInStock: Flow<Int> = inventoryDao.getTotalStockCount().map { it ?: 0 }

    val lowStockVariants: Flow<List<ProductVariantEntity>> =
        inventoryDao.getLowStockVariants(threshold = 3)

    val recentMovements: Flow<List<StockMovementEntity>> =
        inventoryDao.getRecentMovements(limit = 15)

    fun getProductsForBrand(brandId: Long): Flow<List<ProductEntity>> {
        return productDao.getProductsByBrand(brandId)
    }

    // ==========================================
    // Reactive Search & Multi-Faceted Filtering
    // ==========================================

    /**
     * Reactively searches products by matching [query] across:
     * - Brand Name
     * - Product Name & Category
     * - Variant SKU, Size, Colour, and Supplier Code
     */
    fun searchCatalog(
        query: String,
        brandIdFilter: Long? = null,
        typeFilter: ProductType? = null
    ): Flow<List<ProductWithVariants>> {
        return combine(
            productDao.getActiveProductsWithVariants(),
            productDao.getAllBrands()
        ) { productsWithVariants, brands ->
            val brandMap = brands.associateBy { it.id }
            val trimmed = query.trim().lowercase()

            productsWithVariants.filter { item ->
                val product = item.product
                val brandName = brandMap[product.brandId]?.name.orEmpty().lowercase()

                // Filter by Brand if selected
                if (brandIdFilter != null && product.brandId != brandIdFilter) {
                    return@filter false
                }

                // Filter by ProductType if selected
                if (typeFilter != null && product.type != typeFilter) {
                    return@filter false
                }

                if (trimmed.isEmpty()) {
                    true
                } else {
                    val matchesProduct = product.name.lowercase().contains(trimmed) ||
                            product.category.lowercase().contains(trimmed) ||
                            brandName.contains(trimmed)

                    val matchesVariant = item.variants.any { v ->
                        v.sku.lowercase().contains(trimmed) ||
                                (v.size?.lowercase()?.contains(trimmed) == true) ||
                                (v.color?.lowercase()?.contains(trimmed) == true) ||
                                (v.supplierCode?.lowercase()?.contains(trimmed) == true)
                    }

                    matchesProduct || matchesVariant
                }
            }
        }
    }

    fun getProductWithVariants(productId: Long): Flow<ProductWithVariants?> {
        return productDao.getProductWithVariants(productId)
    }

    suspend fun getProduct(productId: Long): ProductEntity? {
        return productDao.getProductById(productId)
    }

    suspend fun getVariant(variantId: Long): ProductVariantEntity? {
        return productDao.getVariantById(variantId)
    }

    // ==========================================
    // Transactional Stock Ledger Operations
    // ==========================================

    suspend fun recordMovement(
        variantId: Long,
        delta: Int,
        movementType: MovementType,
        unitPricePaise: Long? = null,
        referenceId: String? = null,
        notes: String? = null
    ): Long {
        return inventoryDao.recordStockMovement(
            variantId = variantId,
            delta = delta,
            movementType = movementType,
            unitPricePaise = unitPricePaise,
            referenceId = referenceId,
            notes = notes
        )
    }

    suspend fun recordSale(
        variantId: Long,
        quantity: Int,
        sellingPricePaise: Long? = null,
        billReference: String? = null
    ): Long {
        require(quantity > 0) { "Sale quantity must be positive (received: $quantity)." }
        return recordMovement(
            variantId = variantId,
            delta = -quantity,
            movementType = MovementType.SALE,
            unitPricePaise = sellingPricePaise,
            referenceId = billReference,
            notes = "Customer Retail Sale"
        )
    }

    suspend fun recordPurchase(
        variantId: Long,
        quantity: Int,
        costPricePaise: Long? = null,
        invoiceReference: String? = null
    ): Long {
        require(quantity > 0) { "Purchase quantity must be positive (received: $quantity)." }
        return recordMovement(
            variantId = variantId,
            delta = quantity,
            movementType = MovementType.PURCHASE,
            unitPricePaise = costPricePaise,
            referenceId = invoiceReference,
            notes = "Supplier Restock Purchase"
        )
    }

    /**
     * Registers a scanned invoice item with deterministic deduplication by supplier code.
     * Guaranteed to mutate inventory only via atomic [InventoryDao.recordStockMovement].
     */
    suspend fun registerScannedInvoiceItem(
        brandName: String,
        productName: String,
        category: String,
        type: ProductType = ProductType.BICYCLE,
        sku: String,
        supplierCode: String?,
        size: String? = null,
        color: String? = null,
        mrpPaise: Long = 0L,
        costPricePaise: Long = 0L,
        sellingPricePaise: Long = 0L,
        quantity: Int,
        invoiceReference: String? = null
    ): Long {
        require(quantity > 0) { "Scanned invoice item quantity must be positive." }

        // 1. Resolve or create Brand
        val existingBrand = productDao.getBrandByName(brandName)
        val brandId = existingBrand?.id ?: productDao.insertBrand(BrandEntity(name = brandName))

        // 2. Deterministic lookup by supplier code
        val existingVariants = if (!supplierCode.isNullOrBlank()) {
            productDao.getVariantsBySupplierCode(supplierCode)
        } else {
            emptyList()
        }

        val matchedVariant = existingVariants.firstOrNull()

        return if (matchedVariant != null) {
            // Existing variant found: restock via atomic Purchase movement
            recordPurchase(
                variantId = matchedVariant.id,
                quantity = quantity,
                costPricePaise = costPricePaise,
                invoiceReference = invoiceReference
            )
            matchedVariant.id
        } else {
            // Insert product
            val productId = productDao.insertProduct(
                ProductEntity(
                    brandId = brandId,
                    name = productName,
                    category = category,
                    type = type,
                    isActive = true
                )
            )

            // Insert new variant with opening stock balance of 0
            val newVariantId = productDao.insertVariant(
                ProductVariantEntity(
                    productId = productId,
                    sku = sku,
                    supplierCode = supplierCode,
                    size = size,
                    color = color,
                    mrpPaise = mrpPaise,
                    costPricePaise = costPricePaise,
                    sellingPricePaise = sellingPricePaise,
                    stockQuantity = 0 // Will be incremented by ledger movement
                )
            )

            // Record Purchase movement
            recordPurchase(
                variantId = newVariantId,
                quantity = quantity,
                costPricePaise = costPricePaise,
                invoiceReference = invoiceReference
            )
            newVariantId
        }
    }
}