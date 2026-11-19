package dev.vignesh.cycleshop.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing reactive catalog browsing, multi-attribute search,
 * brand/category filtering, and transactional stock updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrandId = MutableStateFlow<Long?>(null)
    val selectedBrandId: StateFlow<Long?> = _selectedBrandId.asStateFlow()

    private val _selectedType = MutableStateFlow<ProductType?>(null)
    val selectedType: StateFlow<ProductType?> = _selectedType.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val allBrands: StateFlow<List<BrandEntity>> = repository.allBrands
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalStock: StateFlow<Int> = repository.totalUnitsInStock
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val productsWithVariants: StateFlow<List<ProductWithVariants>> =
        combine(_searchQuery, _selectedBrandId, _selectedType) { query, brandId, type ->
            Triple(query, brandId, type)
        }.flatMapLatest { (query, brandId, type) ->
            repository.searchCatalog(
                query = query,
                brandIdFilter = brandId,
                typeFilter = type
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onBrandSelected(brandId: Long?) {
        _selectedBrandId.value = brandId
    }

    fun onTypeSelected(type: ProductType?) {
        _selectedType.value = type
    }

    fun quickRestock(variantId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                repository.recordPurchase(
                    variantId = variantId,
                    quantity = quantity,
                    invoiceReference = "QUICK-RESTOCK"
                )
                _snackbarMessage.emit("Successfully restocked +$quantity unit(s)")
            } catch (e: Exception) {
                _snackbarMessage.emit("Restock error: ${e.message}")
            }
        }
    }

    fun quickSale(variantId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                repository.recordSale(
                    variantId = variantId,
                    quantity = quantity,
                    billReference = "QUICK-SALE"
                )
                _snackbarMessage.emit("Sale recorded: -$quantity unit(s)")
            } catch (e: Exception) {
                _snackbarMessage.emit("Sale error: ${e.message}")
            }
        }
    }
}

class InventoryViewModelFactory(
    private val repository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
