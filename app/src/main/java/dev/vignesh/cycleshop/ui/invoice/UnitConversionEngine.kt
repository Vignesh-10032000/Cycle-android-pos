package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import java.util.Locale

/**
 * Result of a unit conversion calculation.
 */
sealed class UnitConversionResult {
    data class Success(
        val rawUnit: String,
        val targetUnit: InventoryUnit,
        val conversionFactor: Int,
        val effectiveBaseQuantity: Int,
        val notes: String
    ) : UnitConversionResult()

    data class RequiresReview(
        val rawUnit: String,
        val targetUnit: InventoryUnit,
        val conversionFactor: Int,
        val effectiveBaseQuantity: Int,
        val warningMessage: String
    ) : UnitConversionResult()

    data class Invalid(
        val rawUnit: String,
        val errorMessage: String
    ) : UnitConversionResult()
}

/**
 * Deterministic unit conversion engine for supplier invoices.
 *
 * Rules:
 * - Gross: 1 Gross = 144 Pieces (standard bicycle spokes packaging). If inventory tracks Gross directly, factor = 1.
 * - PC / Pcs / Units / Nos / Sets: 1 to 1 conversion.
 * - Pair: 1 Pair = 2 Pieces.
 * - Dozen: 1 Dozen = 12 Pieces.
 * - Non-positive quantities are strictly rejected as [UnitConversionResult.Invalid].
 */
object UnitConversionEngine {

    fun convert(
        rawUnit: String?,
        rawQuantity: Double?,
        targetInventoryUnit: InventoryUnit = InventoryUnit.PIECE
    ): UnitConversionResult {
        if (rawQuantity == null || rawQuantity <= 0.0) {
            return UnitConversionResult.Invalid(
                rawUnit = rawUnit ?: "Unknown",
                errorMessage = "Quantity must be greater than zero (found: $rawQuantity)."
            )
        }

        val norm = (rawUnit ?: "").trim().uppercase(Locale.ROOT)

        return when {
            norm == "GROSS" || norm == "GRS" -> {
                when (targetInventoryUnit) {
                    InventoryUnit.PIECE -> {
                        val factor = 144
                        val baseQty = (rawQuantity * factor).toInt()
                        UnitConversionResult.Success(
                            rawUnit = rawUnit ?: "Gross",
                            targetUnit = InventoryUnit.PIECE,
                            conversionFactor = factor,
                            effectiveBaseQuantity = baseQty,
                            notes = "$rawQuantity Gross = $baseQty Pieces (1 Gross = 144 Pieces)"
                        )
                    }
                    InventoryUnit.GROSS -> {
                        UnitConversionResult.Success(
                            rawUnit = rawUnit ?: "Gross",
                            targetUnit = InventoryUnit.GROSS,
                            conversionFactor = 1,
                            effectiveBaseQuantity = rawQuantity.toInt(),
                            notes = "$rawQuantity Gross tracked as Gross units"
                        )
                    }
                    else -> UnitConversionResult.Invalid(
                        rawUnit = rawUnit ?: "Gross",
                        errorMessage = "Cannot convert Gross to target unit $targetInventoryUnit"
                    )
                }
            }

            norm in listOf("PC", "PCS", "PIECE", "PIECES", "UNIT", "UNITS", "NOS", "NO", "SET", "SETS") -> {
                UnitConversionResult.Success(
                    rawUnit = rawUnit ?: "PC",
                    targetUnit = targetInventoryUnit,
                    conversionFactor = 1,
                    effectiveBaseQuantity = rawQuantity.toInt(),
                    notes = "$rawQuantity ${rawUnit ?: "PC"} = ${rawQuantity.toInt()} ${targetInventoryUnit.name}"
                )
            }

            norm == "PAIR" || norm == "PAIRS" -> {
                val factor = if (targetInventoryUnit == InventoryUnit.PIECE) 2 else 1
                val baseQty = (rawQuantity * factor).toInt()
                UnitConversionResult.Success(
                    rawUnit = rawUnit ?: "Pair",
                    targetUnit = targetInventoryUnit,
                    conversionFactor = factor,
                    effectiveBaseQuantity = baseQty,
                    notes = "$rawQuantity Pairs = $baseQty Pieces (1 Pair = 2 Pieces)"
                )
            }

            norm in listOf("DOZEN", "DOZ", "DZ") -> {
                val factor = 12
                val baseQty = (rawQuantity * factor).toInt()
                UnitConversionResult.Success(
                    rawUnit = rawUnit ?: "Dozen",
                    targetUnit = InventoryUnit.PIECE,
                    conversionFactor = factor,
                    effectiveBaseQuantity = baseQty,
                    notes = "$rawQuantity Dozen = $baseQty Pieces (1 Dozen = 12 Pieces)"
                )
            }

            norm.isBlank() -> {
                UnitConversionResult.RequiresReview(
                    rawUnit = "UNSPECIFIED",
                    targetUnit = targetInventoryUnit,
                    conversionFactor = 1,
                    effectiveBaseQuantity = rawQuantity.toInt(),
                    warningMessage = "Invoice unit was blank; defaulted 1:1, requires owner verification"
                )
            }

            else -> {
                UnitConversionResult.RequiresReview(
                    rawUnit = rawUnit ?: "Unknown",
                    targetUnit = targetInventoryUnit,
                    conversionFactor = 1,
                    effectiveBaseQuantity = rawQuantity.toInt(),
                    warningMessage = "Unrecognized unit '$rawUnit'; defaulted 1:1, requires owner verification"
                )
            }
        }
    }
}
