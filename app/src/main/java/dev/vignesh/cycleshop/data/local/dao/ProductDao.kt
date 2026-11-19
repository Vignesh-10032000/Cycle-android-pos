package dev.vignesh.cycleshop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for catalog products, brands, and product variants.
 */
@Dao
interface ProductDao {

    // ==========================================
    // Brands
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBrand(brand: BrandEntity): Long

    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM brands WHERE id = :id")
    suspend fun getBrandById(id: Long): BrandEntity?

    @Query("SELECT * FROM brands WHERE name = :name")
    suspend fun getBrandByName(name: String): BrandEntity?

    @Delete
    suspend fun deleteBrand(brand: BrandEntity): Int

    // ==========================================
    // Products
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE brand_id = :brandId AND is_active = 1 ORDER BY name ASC")
    fun getProductsByBrand(brandId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE type = :type AND is_active = 1 ORDER BY name ASC")
    fun getProductsByType(type: ProductType): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category AND is_active = 1 ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    // ==========================================
    // Product Variants
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVariant(variant: ProductVariantEntity): Long

    /**
     * Updates catalog attributes of a variant.
     * Note: stock_quantity is intentionally excluded from this update to guarantee that all
     * inventory balance changes are enforced strictly through [InventoryDao.recordStockMovement].
     */
    @Query("""
        UPDATE product_variants 
        SET supplier_code = :supplierCode,
            size = :size,
            color = :color,
            mrp_paise = :mrpPaise,
            cost_price_paise = :costPricePaise,
            selling_price_paise = :sellingPricePaise,
            specs_json = :specsJson
        WHERE id = :id
    """)
    suspend fun updateVariantMetadata(
        id: Long,
        supplierCode: String?,
        size: String?,
        color: String?,
        mrpPaise: Long,
        costPricePaise: Long,
        sellingPricePaise: Long,
        specsJson: String?
    ): Int

    @Delete
    suspend fun deleteVariant(variant: ProductVariantEntity): Int

    @Query("SELECT * FROM product_variants WHERE id = :id")
    suspend fun getVariantById(id: Long): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE sku = :sku")
    suspend fun getVariantBySku(sku: String): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE supplier_code = :supplierCode")
    suspend fun getVariantsBySupplierCode(supplierCode: String): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE product_id = :productId ORDER BY id ASC")
    fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>>

    // ==========================================
    // Composite Relations
    // ==========================================

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId")
    fun getProductWithVariants(productId: Long): Flow<ProductWithVariants?>

    @Transaction
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveProductsWithVariants(): Flow<List<ProductWithVariants>>
}
