package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.InventoryItemEntity
import com.example.data.network.ProductLookupResult
import com.example.data.network.ProductLookupService
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val scannedBarcode: String? = null,
    val isSearching: Boolean = false,
    val existingItem: InventoryItemEntity? = null,
    val onlineProduct: ProductLookupResult? = null,
    val notFoundMessage: String? = null
)

data class DemoBarcode(
    val code: String,
    val label: String,
    val category: String
)

class ScannerViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    val demoBarcodes = listOf(
        DemoBarcode("3250390001001", "🥫 Huile d'olive Puget", "cuisine"),
        DemoBarcode("3017620422003", "🍫 Nutella 400g", "aliments"),
        DemoBarcode("3600523724451", "🧴 Gel Douche Mixa", "hygiene"),
        DemoBarcode("3014260020123", "🧼 Lessive Ariel", "menager"),
        DemoBarcode("3400936081498", "💊 Doliprane 1000mg", "pharmacie"),
        DemoBarcode("9782070612758", "📚 Le Petit Prince (ISBN)", "autre")
    )

    fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.isSearching) return
        val cleanBarcode = ProductLookupService.normalizeBarcode(barcode)
        if (cleanBarcode.isBlank()) return

        viewModelScope.launch {
            _uiState.value = ScannerUiState(scannedBarcode = cleanBarcode, isSearching = true)

            // 1. Check local inventory
            val localItem = inventoryRepository.getItemByBarcode(cleanBarcode)
            if (localItem != null) {
                _uiState.value = ScannerUiState(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    existingItem = localItem
                )
                _uiEvents.emit("Article trouvé dans votre inventaire !")
                return@launch
            }

            // 2. Query Multi-API & Offline Open-Product-Database cascade
            val online = ProductLookupService.lookupBarcode(cleanBarcode)
            if (online != null) {
                _uiState.value = ScannerUiState(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    onlineProduct = online
                )
                _uiEvents.emit("Produit identifié via ${online.source} !")
            } else {
                _uiState.value = ScannerUiState(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    notFoundMessage = "Code-barres non répertorié dans les bases mondiales. Vous pouvez l'ajouter manuellement !"
                )
            }
        }
    }

    fun quickIncrementExisting(item: InventoryItemEntity, storeTypes: List<com.example.data.model.StoreTypeEntity>) {
        viewModelScope.launch {
            inventoryRepository.updateQuantity(item.id, item.quantity + 1, storeTypes)
            _uiEvents.emit("Quantité augmentée (+1) pour « ${item.name} »")
            reset()
        }
    }

    fun addScannedItemToInventory(
        name: String,
        categoryId: String,
        quantity: Int,
        unit: String,
        storeId: String,
        location: String,
        notes: String = ""
    ) {
        val barcode = _uiState.value.scannedBarcode
        viewModelScope.launch {
            inventoryRepository.saveItem(
                InventoryItemEntity(
                    name = name.trim(),
                    categoryId = categoryId,
                    quantity = quantity.coerceAtLeast(0),
                    unit = unit.ifBlank { "pièce" },
                    preferredStoreId = storeId,
                    location = location.ifBlank { "Maison" },
                    barcode = barcode,
                    notes = notes.trim()
                )
            )
            _uiEvents.emit("« $name » ajouté à l'inventaire")
            reset()
        }
    }

    fun reset() {
        _uiState.value = ScannerUiState()
    }
}

class ScannerViewModelFactory(
    private val inventoryRepository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(inventoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
