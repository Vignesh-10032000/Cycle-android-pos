package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted mapping between a supplier's item code or description pattern and a verified
 * catalog [ProductVariantEntity.id].
 *
 * Mappings are created or confirmed ONLY by the shop owner, enabling automatic Tier 1 matching
 * on future invoice scans for the same supplier.
 */
@Entity(
    tableName = "supplier_product_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["supplier_identifier", "supplier_item_code"]),
        Index(value = ["variant_id"])
    ]
)
data class SupplierProductMappingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "supplier_identifier")
    val supplierIdentifier: String,

    @ColumnInfo(name = "supplier_item_code")
    val supplierItemCode: String? = null,

    @ColumnInfo(name = "supplier_desc_pattern")
    val supplierDescPattern: String,

    @ColumnInfo(name = "variant_id")
    val variantId: Long,

    @ColumnInfo(name = "verified_by_owner", defaultValue = "1")
    val verifiedByOwner: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
