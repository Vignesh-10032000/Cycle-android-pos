package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnitConversionEngine].
 *
 * Verifies mathematical conversion accuracy, packaging conversions (Gross, Dozen, Pair, Pcs),
 * non-positive rejection rules, and uncertain unit flagging.
 */
class UnitConversionEngineTest {

    @Test
    fun `convert gross to pieces multiplies by 144`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "GROSS",
            rawQuantity = 2.0,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.Success)
        val success = result as UnitConversionResult.Success
        assertEquals(144, success.conversionFactor)
        assertEquals(288, success.effectiveBaseQuantity)
        assertEquals(InventoryUnit.PIECE, success.targetUnit)
    }

    @Test
    fun `convert fractional gross to pieces calculates exact integer base quantity`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "Grs",
            rawQuantity = 0.5,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.Success)
        val success = result as UnitConversionResult.Success
        assertEquals(72, success.effectiveBaseQuantity)
    }

    @Test
    fun `convert gross with target unit gross maintains factor 1`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "Gross",
            rawQuantity = 5.0,
            targetInventoryUnit = InventoryUnit.GROSS
        )

        assertTrue(result is UnitConversionResult.Success)
        val success = result as UnitConversionResult.Success
        assertEquals(1, success.conversionFactor)
        assertEquals(5, success.effectiveBaseQuantity)
        assertEquals(InventoryUnit.GROSS, success.targetUnit)
    }

    @Test
    fun `convert pc and unit aliases preserve 1 to 1 factor`() {
        val aliases = listOf("PC", "PCS", "PIECE", "PIECES", "UNIT", "UNITS", "NOS", "NO", "SET", "SETS")
        for (unit in aliases) {
            val result = UnitConversionEngine.convert(
                rawUnit = unit,
                rawQuantity = 10.0,
                targetInventoryUnit = InventoryUnit.PIECE
            )
            assertTrue("Expected Success for $unit", result is UnitConversionResult.Success)
            val success = result as UnitConversionResult.Success
            assertEquals(1, success.conversionFactor)
            assertEquals(10, success.effectiveBaseQuantity)
        }
    }

    @Test
    fun `convert pair to pieces multiplies by 2`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "Pair",
            rawQuantity = 4.0,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.Success)
        val success = result as UnitConversionResult.Success
        assertEquals(2, success.conversionFactor)
        assertEquals(8, success.effectiveBaseQuantity)
    }

    @Test
    fun `convert dozen to pieces multiplies by 12`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "Dozen",
            rawQuantity = 3.0,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.Success)
        val success = result as UnitConversionResult.Success
        assertEquals(12, success.conversionFactor)
        assertEquals(36, success.effectiveBaseQuantity)
    }

    @Test
    fun `convert blank unit defaults to 1 to 1 and flags for review`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "   ",
            rawQuantity = 5.0,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.RequiresReview)
        val review = result as UnitConversionResult.RequiresReview
        assertEquals(1, review.conversionFactor)
        assertEquals(5, review.effectiveBaseQuantity)
        assertTrue(review.warningMessage.contains("blank"))
    }

    @Test
    fun `convert unrecognized unit defaults to 1 to 1 and flags for review`() {
        val result = UnitConversionEngine.convert(
            rawUnit = "BAG",
            rawQuantity = 2.0,
            targetInventoryUnit = InventoryUnit.PIECE
        )

        assertTrue(result is UnitConversionResult.RequiresReview)
        val review = result as UnitConversionResult.RequiresReview
        assertEquals(1, review.conversionFactor)
        assertEquals(2, review.effectiveBaseQuantity)
        assertTrue(review.warningMessage.contains("Unrecognized"))
    }

    @Test
    fun `non-positive quantity is strictly rejected as invalid`() {
        val zeroResult = UnitConversionEngine.convert(
            rawUnit = "PCS",
            rawQuantity = 0.0
        )
        assertTrue(zeroResult is UnitConversionResult.Invalid)

        val negativeResult = UnitConversionEngine.convert(
            rawUnit = "PCS",
            rawQuantity = -5.0
        )
        assertTrue(negativeResult is UnitConversionResult.Invalid)

        val nullResult = UnitConversionEngine.convert(
            rawUnit = "PCS",
            rawQuantity = null
        )
        assertTrue(nullResult is UnitConversionResult.Invalid)
    }
}
