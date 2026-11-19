package dev.vignesh.cycleshop.ui.invoice

import java.math.BigDecimal

/** Parses complete decimal fields and converts them at the limits of the current data model. */
internal object InvoiceNumbers {
    private val decimalPattern = Regex(
        """(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})+,[0-9]{3})(?:\.[0-9]+)?"""
    )
    private val maxQuantity = BigDecimal.valueOf(Int.MAX_VALUE.toLong())

    private fun parse(text: String): BigDecimal? {
        val value = text.trim()
        if (value.length > 48 || !decimalPattern.matches(value)) return null
        return value.replace(",", "").toBigDecimalOrNull()
    }

    fun quantity(text: String): Double? {
        val decimal = parse(text) ?: return null
        if (decimal.signum() <= 0 || decimal > maxQuantity) return null
        val value = decimal.toDouble()
        return value.takeIf { BigDecimal.valueOf(it).compareTo(decimal) == 0 }
    }

    fun quantityDecimal(value: Double?): BigDecimal? =
        value?.takeIf { it.isFinite() && it > 0.0 && it <= Int.MAX_VALUE }
            ?.let { BigDecimal.valueOf(it) }

    fun paise(text: String, allowZero: Boolean = false): Long? {
        val decimal = parse(text.trim().removePrefix("₹")) ?: return null
        if (!allowZero && decimal.signum() == 0) return null
        return exactLong(decimal.movePointRight(2))
    }

    fun percentage(text: String): Double? =
        parse(text.trim().removeSuffix("%"))?.takeIf {
            it <= BigDecimal(100) && it.stripTrailingZeros().scale() <= 2
        }
            ?.let { decimal -> decimal.toDouble().takeIf { BigDecimal.valueOf(it).compareTo(decimal) == 0 } }

    fun exactLong(value: BigDecimal): Long? =
        try { value.longValueExact() } catch (_: ArithmeticException) { null }

    fun exactStockQuantity(value: BigDecimal): Int? =
        try { value.intValueExact().takeIf { it > 0 } } catch (_: ArithmeticException) { null }

    fun decimalRupees(paise: Long): BigDecimal = BigDecimal.valueOf(paise, 2)
}
