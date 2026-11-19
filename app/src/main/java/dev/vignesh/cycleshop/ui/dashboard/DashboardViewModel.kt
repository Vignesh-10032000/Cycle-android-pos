package dev.vignesh.cycleshop.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel providing reactive Room data for the Dashboard overview:
 * - Live total stock balance across all variants
 * - Append-only recent stock movements ledger
 * - Brands and catalog summary
 */
class DashboardViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    val totalUnitsInStock: StateFlow<Int> = repository.totalUnitsInStock
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val recentMovements: StateFlow<List<StockMovementEntity>> = repository.recentMovements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBrands: StateFlow<List<BrandEntity>> = repository.allBrands
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productsWithVariants: StateFlow<List<ProductWithVariants>> = repository.activeProductsWithVariants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

class DashboardViewModelFactory(
    private val repository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
