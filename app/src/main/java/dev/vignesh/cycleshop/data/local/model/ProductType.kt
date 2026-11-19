package dev.vignesh.cycleshop.data.local.model

enum class ProductType(val dbValue: String) {
    BICYCLE("BICYCLE"),
    SPARE_PART("SPARE_PART"),
    ACCESSORY("ACCESSORY");

    companion object {
        fun fromDb(value: String?): ProductType {
            if (value.isNullOrBlank()) return BICYCLE
            return entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) }
                ?: BICYCLE
        }
    }
}
