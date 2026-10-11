package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val activeItems: List<ShoppingItemEntity> = emptyList(),
    val onHoldItems: List<ShoppingItemEntity> = emptyList(),
    val purchasedItems: List<ShoppingItemEntity> = emptyList(),
    val storeTypes: List<StoreTypeEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedStoreId: String? = null,
    val totalActiveCount: Int = 0,
    val totalOnHoldCount: Int = 0,
    val totalPurchasedCount: Int = 0
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _selectedStoreId = MutableStateFlow<String?>(null)
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    val uiState: StateFlow<ShoppingUiState> = combine(
        shoppingRepository.allShoppingItems,
        inventoryRepository.allStoreTypes,
        inventoryRepository.allCategories,
        _selectedStoreId
    ) { items, stores, categories, selectedStore ->
        val filtered = if (selectedStore != null) {
            items.filter { it.storeId == selectedStore }
        } else {
            items
        }

        val active = filtered.filter { !it.isPurchased && !it.isOnHold }
        val onHold = filtered.filter { !it.isPurchased && it.isOnHold }
        val purchased = filtered.filter { it.isPurchased }

        ShoppingUiState(
            activeItems = active,
            onHoldItems = onHold,
            purchasedItems = purchased,
            storeTypes = stores,
            categories = categories,
            selectedStoreId = selectedStore,
            totalActiveCount = active.size,
            totalOnHoldCount = onHold.size,
            totalPurchasedCount = purchased.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShoppingUiState()
    )

    fun onStoreSelect(storeId: String?) {
        _selectedStoreId.value = storeId
    }

    fun togglePurchased(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.setPurchased(item.id, !item.isPurchased)
        }
    }

    fun toggleOnHold(item: ShoppingItemEntity) {
        viewModelScope.launch {
            val newStatus = !item.isOnHold
            shoppingRepository.setOnHold(item.id, newStatus)
            if (newStatus) {
                _uiEvents.emit("« ${item.name} » mis en attente")
            } else {
                _uiEvents.emit("« ${item.name} » réactivé dans la liste")
            }
        }
    }

    fun changeStore(item: ShoppingItemEntity, newStoreId: String) {
        viewModelScope.launch {
            shoppingRepository.updateStore(item.id, newStoreId)
            val storeName = uiState.value.storeTypes.find { it.id == newStoreId }?.name ?: newStoreId
            _uiEvents.emit("Déplacé vers $storeName")
        }
    }

    fun deleteItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(item)
            _uiEvents.emit("« ${item.name} » supprimé de la liste")
        }
    }

    fun addItem(
        name: String,
        categoryId: String,
        storeId: String,
        quantity: Int,
        unit: String
    ) {
        viewModelScope.launch {
            val newItem = ShoppingItemEntity(
                name = name.trim(),
                categoryId = categoryId,
                storeId = storeId,
                quantityNeeded = quantity.coerceAtLeast(1),
                unit = unit.ifBlank { "pièce" },
                isPurchased = false,
                isOnHold = false
            )
            shoppingRepository.insertItem(newItem)
            _uiEvents.emit("Ajouté à la liste")
        }
    }

    fun restockItem(item: ShoppingItemEntity, restockQty: Int = 1) {
        viewModelScope.launch {
            shoppingRepository.restockPurchasedItem(item, restockQty, removeFromList = true)
            _uiEvents.emit("« ${item.name} » restocké (+$restockQty) dans l'inventaire")
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            shoppingRepository.clearPurchased()
            _uiEvents.emit("Articles achetés effacés")
        }
    }
}

class ShoppingViewModelFactory(
    private val shoppingRepository: ShoppingRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(shoppingRepository, inventoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
