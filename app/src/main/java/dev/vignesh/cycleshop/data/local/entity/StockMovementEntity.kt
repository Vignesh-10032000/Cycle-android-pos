package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vignesh.cycleshop.data.local.model.MovementType

/**
 * Append-only stock movement ledger tracking all inventory additions, sales,
 * service usage, and manual adjustments.
 *
 * Foreign key is strictly RESTRICT to guarantee audit trail preservation.
 */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["variant_id"]),
        Index(value = ["movement_type"]),
        Index(value = ["created_at"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "variant_id")
    val variantId: Long,

    @ColumnInfo(name = "delta")
    val delta: Int,

    @ColumnInfo(name = "movement_type")
    val movementType: MovementType,

    @ColumnInfo(name = "unit_price_paise")
    val unitPricePaise: Long? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(delta != 0) { "Stock movement delta cannot be zero." }
        if (unitPricePaise != null) {
            require(unitPricePaise >= 0) { "Unit price in paise cannot be negative." }
        }
        when (movementType) {
            MovementType.OPENING -> require(delta > 0) { "OPENING stock movement delta must be positive." }
            MovementType.PURCHASE -> require(delta > 0) { "PURCHASE stock movement delta must be positive." }
            MovementType.SALE -> require(delta < 0) { "SALE stock movement delta must be negative." }
            MovementType.SERVICE -> require(delta < 0) { "SERVICE stock movement delta must be negative." }
            MovementType.ADJUSTMENT -> { /* delta can be positive or negative for reconciliation */ }
        }
    }
}
