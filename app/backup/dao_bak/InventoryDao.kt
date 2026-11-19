package dev.vignesh.cycleshop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: BrandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE brandId = :brandId")
    fun getProductsByBrand(brandId: Long): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity WHERE id = :productId")
    suspend fun addStock(productId: Long, quantity: Int)
    
    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId AND stockQuantity >= :quantity")
    suspend fun reduceStock(productId: Long, quantity: Int): Int // Returns number of rows updated. If 0, stock was insufficient.

    @Transaction
    suspend fun registerNewProductStock(brandName: String, product: ProductEntity) {
        // Find existing brand, or create new one
        val existingBrand = getBrandByName(brandName)
        val brandId = existingBrand?.id ?: insertBrand(BrandEntity(name = brandName))
        
        // Insert product linked to this brand
        insertProduct(product.copy(brandId = brandId))
    }
    
    @Query("SELECT * FROM brands WHERE name = :name LIMIT 1")
    suspend fun getBrandByName(name: String): BrandEntity?
}