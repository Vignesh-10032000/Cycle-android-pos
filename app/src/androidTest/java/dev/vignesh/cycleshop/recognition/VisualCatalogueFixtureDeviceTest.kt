package dev.vignesh.cycleshop.recognition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vignesh.cycleshop.data.local.VglDatabase
import dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogueFixtureDeviceTest {
    @Test
    fun sampleVariantsAndKnownBrandDefectArePresentInDisposableRoom(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VglDatabase::class.java).build()
        try {
            DatabaseSeeder.seedIfEmpty(database)
            val repository = InventoryRepository(database.productDao(), database.inventoryDao())
            val catalogue = object : ReadOnlyVisualCatalogue {
                override suspend fun activeSnapshot() = VisualCatalogueSnapshot(
                    brands = repository.allBrands.first(),
                    products = repository.activeProductsWithVariants.first()
                )
            }
            val snapshot = catalogue.activeSnapshot()
            val variants = snapshot.products.flatMap { it.variants }.associateBy { it.sku }
            assertEquals(5, variants.getValue("VGL-HERO-SP275-BLK").stockQuantity)
            assertEquals(3, variants.getValue("VGL-HERO-SP275-BLU").stockQuantity)
            assertEquals(8, variants.getValue("VGL-SHIM-RDTX800").stockQuantity)
            assertEquals(20, variants.getValue("VGL-SHIM-VB-PADS").stockQuantity)

            val chain = snapshot.products.single { item -> item.variants.any { it.sku == "VGL-KMC-Z7-116" } }
            assertEquals("KMC Z7 7-Speed Bicycle Chain", chain.product.name)
            assertEquals("Shimano", snapshot.brands.single { it.id == chain.product.brandId }.name)
        } finally {
            database.close()
        }
    }
}
