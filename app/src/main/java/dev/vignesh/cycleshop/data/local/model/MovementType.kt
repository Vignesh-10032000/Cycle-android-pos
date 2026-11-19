package dev.vignesh.cycleshop.data.local.model

enum class MovementType(val dbValue: String) {
    OPENING("OPENING"),
    PURCHASE("PURCHASE"),
    SALE("SALE"),
    SERVICE("SERVICE"),
    ADJUSTMENT("ADJUSTMENT");

    companion object {
        fun fromDb(value: String?): MovementType {
            if (value.isNullOrBlank()) return ADJUSTMENT
            return entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) }
                ?: ADJUSTMENT
        }
    }
}
