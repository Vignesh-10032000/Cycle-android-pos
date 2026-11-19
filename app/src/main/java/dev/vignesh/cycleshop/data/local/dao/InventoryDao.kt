package dev.vignesh.cycleshop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for stock inventory and append-only movement ledger.
 *
 * All inventory modifications are strictly transactional and execute atomic balance
 * adjustments alongside ledger recording to prevent stock discrepancies.
 */
@Dao
abstract class InventoryDao {

    // ==========================================
    // Low-Level / Engine Operations
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM product_variants WHERE id = :variantId")
    abstract suspend fun getVariantById(variantId: Long): ProductVariantEntity?

    @Query("SELECT stock_quantity FROM product_variants WHERE id = :variantId")
    abstract suspend fun getStockQuantity(variantId: Long): Int?

    /**
     * Executes atomic stock balance adjustment directly at the SQLite engine level.
     * Enforces the nonnegative invariant: (stock_quantity + :delta) >= 0.
     * Returns the number of affected rows (must be 1 for success; 0 indicates variant missing or negative stock).
     */
    @Query("""
        UPDATE product_variants 
        SET stock_quantity = stock_quantity + :delta 
        WHERE id = :variantId AND (stock_quantity + :delta) >= 0
    """)
    abstract suspend fun updateStockQuantityAtomic(variantId: Long, delta: Int): Int

    // ==========================================
    // Reactive Room Flow Queries
    // ==========================================

    @Query("SELECT * FROM stock_movements WHERE variant_id = :variantId ORDER BY created_at DESC")
    abstract fun getMovementsForVariant(variantId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY created_at DESC LIMIT :limit")
    abstract fun getRecentMovements(limit: Int = 50): Flow<List<StockMovementEntity>>

    @Query("SELECT SUM(stock_quantity) FROM product_variants")
    abstract fun getTotalStockCount(): Flow<Int?>

    @Query("SELECT * FROM product_variants WHERE stock_quantity <= :threshold ORDER BY stock_quantity ASC")
    abstract fun getLowStockVariants(threshold: Int = 3): Flow<List<ProductVariantEntity>>

    // ==========================================
    // High-Level Atomic Stock Transactions
    // ==========================================

    /**
     * Atomically records a stock movement in the ledger and updates the variant balance.
     *
     * Invariants enforced:
     * 1. delta != 0 (Zero-delta movements strictly rejected).
     * 2. Sign direction matching movementType (PURCHASE > 0, SALE < 0, SERVICE < 0, OPENING > 0).
     * 3. Integer overflow guard (currentStock + delta <= Int.MAX_VALUE).
     * 4. Nonnegative stock invariant (currentStock + delta >= 0).
     * 5. Atomic row count check (affected rows must equal 1).
     * 6. Transaction rollback: Any exception thrown inside this @Transaction rolls back
     *    both the stock balance and ledger entry.
     */
    @Transaction
    open suspend fun recordStockMovement(
        variantId: Long,
        delta: Int,
        movementType: MovementType,
        unitPricePaise: Long? = null,
        referenceId: String? = null,
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        // 1. Validation: Delta cannot be zero
        require(delta != 0) { "Stock movement delta cannot be zero." }

        // 2. Validation: Direction vs MovementType
        when (movementType) {
            MovementType.OPENING -> require(delta > 0) { "OPENING movement delta must be positive (received: $delta)." }
            MovementType.PURCHASE -> require(delta > 0) { "PURCHASE movement delta must be positive (received: $delta)." }
            MovementType.SALE -> require(delta < 0) { "SALE movement delta must be negative (received: $delta)." }
            MovementType.SERVICE -> require(delta < 0) { "SERVICE movement delta must be negative (received: $delta)." }
            MovementType.ADJUSTMENT -> { /* Can be positive or negative */ }
        }

        // 3. Validation: Monetary amount
        if (unitPricePaise != null) {
            require(unitPricePaise >= 0L) { "Unit price in paise cannot be negative (received: $unitPricePaise)." }
        }

        // 4. Fetch current variant to verify existence & overflow
        val variant = getVariantById(variantId)
            ?: throw IllegalArgumentException("Product variant with ID $variantId does not exist.")

        // 5. Overflow and underflow check using Long arithmetic before converting to Int
        val projectedStockLong = variant.stockQuantity.toLong() + delta.toLong()
        if (projectedStockLong > Int.MAX_VALUE.toLong()) {
            throw ArithmeticException(
                "Stock quantity would overflow Int.MAX_VALUE: current=${variant.stockQuantity}, delta=$delta, projected=$projectedStockLong"
            )
        }

        // 6. Underflow check: prevent negative stock
        if (projectedStockLong < 0L) {
            throw IllegalStateException(
                "Insufficient stock for variant '$variantId' (${variant.sku}). Current stock: ${variant.stockQuantity}, requested delta: $delta."
            )
        }
        val projectedStock = projectedStockLong.toInt()

        // 7. Atomic SQLite update with conditional WHERE (stock_quantity + :delta >= 0)
        val affectedRows = updateStockQuantityAtomic(variantId, delta)
        check(affectedRows == 1) {
            "Atomic stock update failed: expected 1 row affected, but got $affectedRows. Concurrent modification or constraint violation."
        }

        // 8. Ledger append-only record creation
        val movement = StockMovementEntity(
            variantId = variantId,
            delta = delta,
            movementType = movementType,
            unitPricePaise = unitPricePaise,
            referenceId = referenceId,
            notes = notes,
            createdAt = timestamp
        )

        val movementId = insertMovement(movement)
        check(movementId > 0) { "Failed to insert stock movement ledger entry." }

        return movementId
    }
}
