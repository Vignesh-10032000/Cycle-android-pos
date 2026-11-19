package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vignesh.cycleshop.data.local.model.ProductType

/**
 * Represents a high-level catalog product in the VGL Cycle Shop catalog.
 * Concrete variants (size, color, prices, stock) are modeled in [ProductVariantEntity].
 */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = BrandEntity::class,
            parentColumns = ["id"],
            childColumns = ["brand_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["brand_id"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity @Ignore constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "brand_id")
    val brandId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "category")
    val category: String = "",

    @ColumnInfo(name = "type")
    val type: ProductType = ProductType.BICYCLE,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // In-memory bridge fields preserved for Phase 1 UI and test compatibility (ignored by Room)
    @Ignore
    val size: String? = null,
    @Ignore
    val color: String? = null,
    @Ignore
    val supplierCode: String? = null,
    @Ignore
    val price: Double = 0.0,
    @Ignore
    val stockQuantity: Int = 0
) {
    /**
     * Room persistence constructor: Binds strictly to the 7 SQLite columns of the 'products' table.
     */
    constructor(
        id: Long,
        brandId: Long,
        name: String,
        category: String,
        type: ProductType,
        isActive: Boolean,
        createdAt: Long
    ) : this(
        id = id,
        brandId = brandId,
        name = name,
        category = category,
        type = type,
        isActive = isActive,
        createdAt = createdAt,
        size = null,
        color = null,
        supplierCode = null,
        price = 0.0,
        stockQuantity = 0
    )

    init {
        require(name.isNotBlank() || id == 0L) { "Product name cannot be blank." }
        require(stockQuantity >= 0) { "Stock quantity cannot be negative." }
    }
}