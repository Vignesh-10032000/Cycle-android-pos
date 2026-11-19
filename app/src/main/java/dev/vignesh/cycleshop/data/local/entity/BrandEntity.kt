package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a bicycle or spare-part manufacturer/brand (e.g. Hero, Firefox, Shimano).
 */
@Entity(
    tableName = "brands",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class BrandEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String
) {
    init {
        require(name.isNotBlank()) { "Brand name cannot be blank." }
    }
}