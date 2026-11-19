package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import java.math.BigDecimal
import java.util.Locale

sealed interface UnitConversionResult {
    data class Success(
        val targetUnit: InventoryUnit,
        val conversionFactor: Int,
        val effectiveBaseQuantity: Int
    ) : UnitConversionResult

    data class RequiresReview(val warningMessage: String) : UnitConversionResult
    data class Invalid(val errorMessage: String) : UnitConversionResult
}

object UnitConversionEngine {
    private enum class PurchaseUnit { PIECE, GROSS, SET, PAIR, DOZEN }

    fun convert(
        rawUnit: String?,
        rawQuantity: Double?,
        targetInventoryUnit: InventoryUnit = InventoryUnit.PIECE
    ): UnitConversionResult {
        val quantity = InvoiceNumbers.quantityDecimal(rawQuantity)
            ?: return UnitConversionResult.Invalid("Quantity must be finite, positive and within the supported range.")
        val source = when (rawUnit?.trim()?.uppercase(Locale.ROOT)) {
            "PC", "PCS", "PIECE", "PIECES", "UNIT", "UNITS", "NOS", "NO" -> PurchaseUnit.PIECE
            "GROSS", "GRS" -> PurchaseUnit.GROSS
            "SET", "SETS" -> PurchaseUnit.SET
            "PAIR", "PAIRS" -> PurchaseUnit.PAIR
            "DOZEN", "DOZ", "DZ" -> PurchaseUnit.DOZEN
            else -> return UnitConversionResult.RequiresReview(
                if (rawUnit.isNullOrBlank()) "Invoice unit is missing."
                else "Unrecognized invoice unit '$rawUnit'."
            )
        }
        val factor = when {
            source.name == targetInventoryUnit.name -> 1
            targetInventoryUnit == InventoryUnit.PIECE && source == PurchaseUnit.GROSS -> 144
            targetInventoryUnit == InventoryUnit.PIECE && source == PurchaseUnit.DOZEN -> 12
            targetInventoryUnit == InventoryUnit.PIECE && source == PurchaseUnit.PAIR -> 2
            else -> return UnitConversionResult.RequiresReview(
                "Confirm how '$rawUnit' converts to ${targetInventoryUnit.displayName} for this product."
            )
        }
        val effective = InvoiceNumbers.exactStockQuantity(quantity.multiply(BigDecimal.valueOf(factor.toLong())))
            ?: return UnitConversionResult.Invalid("Converted stock quantity must be a whole number in the supported range.")
        return UnitConversionResult.Success(targetInventoryUnit, factor, effective)
    }
}
