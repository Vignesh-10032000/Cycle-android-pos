package dev.vignesh.cycleshop.data.local

import androidx.room.TypeConverter
import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductSpecs
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus

/**
 * Room TypeConverters for non-primitive attributes.
 *
 * NOTE: Monetary values (paise) and timestamps (epoch millis) are stored directly
 * as primitive SQLite INTEGER (64-bit Long) without converters, avoiding serialization overhead.
 */
class Converters {

    @TypeConverter
    fun fromProductType(type: ProductType?): String? {
        return type?.dbValue
    }

    @TypeConverter
    fun toProductType(value: String?): ProductType? {
        return value?.let { ProductType.fromDb(it) }
    }

    @TypeConverter
    fun fromMovementType(type: MovementType?): String? {
        return type?.dbValue
    }

    @TypeConverter
    fun toMovementType(value: String?): MovementType? {
        return value?.let { MovementType.fromDb(it) }
    }

    @TypeConverter
    fun fromProductSpecs(specs: ProductSpecs?): String? {
        return specs?.toJson()
    }

    @TypeConverter
    fun toProductSpecs(json: String?): ProductSpecs? {
        return json?.let { ProductSpecs.fromJson(it) }
    }

    @TypeConverter
    fun fromPurchaseInvoiceStatus(status: PurchaseInvoiceStatus?): String? {
        return status?.dbValue
    }

    @TypeConverter
    fun toPurchaseInvoiceStatus(value: String?): PurchaseInvoiceStatus? {
        return value?.let { PurchaseInvoiceStatus.fromDb(it) }
    }

    @TypeConverter
    fun fromLineReviewStatus(status: LineReviewStatus?): String? {
        return status?.dbValue
    }

    @TypeConverter
    fun toLineReviewStatus(value: String?): LineReviewStatus? {
        return value?.let { LineReviewStatus.fromDb(it) }
    }

    @TypeConverter
    fun fromInventoryUnit(unit: InventoryUnit?): String? {
        return unit?.dbValue
    }

    @TypeConverter
    fun toInventoryUnit(value: String?): InventoryUnit? {
        return value?.let { InventoryUnit.fromDb(it) }
    }
}
