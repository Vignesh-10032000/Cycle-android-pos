package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vignesh.cycleshop.data.local.model.ProductSpecs

/**
 * Concrete product variant capturing specific SKU, size, color, pricing (in paise),
 * stock balance, and dynamic versioned specifications (specsJson).
 */
@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["sku"], unique = true),
        Index(value = ["supplier_code"], unique = false)
    ]
)
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "product_id")
    val productId: Long,

    @ColumnInfo(name = "sku")
    val sku: String,

    @ColumnInfo(name = "supplier_code")
    val supplierCode: String? = null,

    @ColumnInfo(name = "size")
    val size: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "mrp_paise")
    val mrpPaise: Long,

    @ColumnInfo(name = "cost_price_paise", defaultValue = "0")
    val costPricePaise: Long = 0L,

    @ColumnInfo(name = "selling_price_paise")
    val sellingPricePaise: Long,

    @ColumnInfo(name = "stock_quantity", defaultValue = "0")
    val stockQuantity: Int = 0,

    @ColumnInfo(name = "specs_json")
    val specsJson: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(sku.isNotBlank()) { "Internal SKU must not be blank." }
        require(mrpPaise >= 0) { "MRP in paise cannot be negative." }
        require(costPricePaise >= 0) { "Cost price in paise cannot be negative." }
        require(sellingPricePaise >= 0) { "Selling price in paise cannot be negative." }
        require(stockQuantity >= 0) { "Stock quantity cannot be negative." }
    }

    /**
     * Type-safe access to structured specifications.
     */
    val specs: ProductSpecs
        @Ignore get() = ProductSpecs.fromJson(specsJson)
}
