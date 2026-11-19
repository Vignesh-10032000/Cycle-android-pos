package dev.vignesh.cycleshop.data.local.model

/**
 * Standardized inventory base units supported by VGL Cycle Shop catalog.
 */
enum class InventoryUnit(val dbValue: String, val displayName: String) {
    PIECE("PIECE", "Piece / Unit"),
    GROSS("GROSS", "Gross (144 pcs)"),
    SET("SET", "Set"),
    PAIR("PAIR", "Pair");

    companion object {
        fun fromDb(value: String): InventoryUnit {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PIECE
        }
    }
}
