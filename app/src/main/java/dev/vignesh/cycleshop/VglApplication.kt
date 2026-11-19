package dev.vignesh.cycleshop

import android.app.Application
import dev.vignesh.cycleshop.data.local.VglDatabase
import dev.vignesh.cycleshop.data.local.seed.DatabaseSeeder
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point for VGL Cycle Shop.
 * Holds application-scoped singleton instances of the Room database and repository,
 * and initializes demo catalogue data on first launch.
 */
class VglApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: VglDatabase by lazy { VglDatabase.getInstance(this) }
    val repository: InventoryRepository by lazy { 
        InventoryRepository(database.productDao(), database.inventoryDao()) 
    }
    val purchaseRepository: dev.vignesh.cycleshop.data.repository.PurchaseRepository by lazy {
        dev.vignesh.cycleshop.data.repository.PurchaseRepository(database.purchaseDao())
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            DatabaseSeeder.seedIfEmpty(database)
        }
    }
}
