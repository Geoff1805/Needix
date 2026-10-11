package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.network.ProductLookupResult
import com.example.data.network.ProductLookupService
import com.example.data.receipt.ParsedReceiptLine
import com.example.data.receipt.ReceiptParser
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScannerUiState(
    val scannedBarcode: String? = null,
    val isSearching: Boolean = false,
    val existingItem: InventoryItemEntity? = null,
    val onlineProduct: ProductLookupResult? = null,
    val notFoundMessage: String? = null,
    val receiptLines: List<ParsedReceiptLine> = emptyList()
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

    private val _beepEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val beepEvents = _beepEvents.asSharedFlow()

    val demoBarcodes = listOf(
        DemoBarcode("3250390001001", "🥫 Huile d'olive Puget", "cuisine"),
        DemoBarcode("3017620422003", "🍫 Nutella 400g", "aliments"),
        DemoBarcode("3600523724451", "🧴 Gel Douche Mixa", "hygiene"),
        DemoBarcode("3014260020123", "🧼 Lessive Ariel", "menager"),
        DemoBarcode("3400936081498", "💊 Doliprane 1000mg", "pharmacie"),
        DemoBarcode("9782070612758", "📚 Le Petit Prince (ISBN)", "autre")
    )

    fun onBarcodeDetected(barcode: String, forceNewScan: Boolean = false) {
        if (_uiState.value.isSearching) return
        // Verrouillage anti-rebond : tant qu'un produit scanné est affiché pour validation,
        // ignorer les nouvelles frames de la caméra jusqu'à l'ajout ou l'annulation.
        if (!forceNewScan && _uiState.value.scannedBarcode != null) return

        val cleanBarcode = ProductLookupService.normalizeBarcode(barcode)
        if (cleanBarcode.isBlank()) return

        // Émettre immédiatement le signal sonore (bip) et haptique
        _beepEvents.tryEmit(Unit)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scannedBarcode = cleanBarcode,
                isSearching = true,
                existingItem = null,
                onlineProduct = null,
                notFoundMessage = null
            )

            // 1. Vérifier d'abord l'inventaire local
            val localItem = inventoryRepository.getItemByBarcode(cleanBarcode)
            if (localItem != null) {
                _uiState.value = _uiState.value.copy(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    existingItem = localItem
                )
                _uiEvents.emit("Code-barres lu : « ${localItem.name} » est déjà en stock !")
                return@launch
            }

            // 2. Interroger la cascade Multi-API & Base Hors-ligne Open-Product-Database
            val online = ProductLookupService.lookupBarcode(cleanBarcode)
            if (online != null) {
                _uiState.value = _uiState.value.copy(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    onlineProduct = online
                )
                _uiEvents.emit("Code-barres lu : « ${online.name} » identifié (${online.source}) — prêt à ajouter !")
            } else {
                _uiState.value = _uiState.value.copy(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    notFoundMessage = "Code-barres lu ($cleanBarcode). Complétez le nom ci-dessous pour l'ajouter à votre inventaire !"
                )
                _uiEvents.emit("Code-barres $cleanBarcode détecté — prêt à ajouter !")
            }
        }
    }

    fun quickIncrementExisting(item: InventoryItemEntity, storeTypes: List<StoreTypeEntity>) {
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

    // ---------------------------------------------------------
    // Lecture de tickets de caisse numériques & Mise à jour stock
    // ---------------------------------------------------------

    fun analyzeDigitalReceipt(rawText: String) {
        viewModelScope.launch {
            val currentItems = inventoryRepository.allInventoryItems.first()
            val parsed = ReceiptParser.parseReceiptText(rawText, currentItems)
            _uiState.value = _uiState.value.copy(receiptLines = parsed)
            if (parsed.isNotEmpty()) {
                _beepEvents.tryEmit(Unit)
                _uiEvents.emit("${parsed.size} produit(s) détecté(s) sur le ticket numérique !")
            } else {
                _uiEvents.emit("Aucun produit reconnu sur ce ticket.")
            }
        }
    }

    fun toggleReceiptLineSelection(index: Int) {
        val current = _uiState.value.receiptLines.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isSelected = !current[index].isSelected)
            _uiState.value = _uiState.value.copy(receiptLines = current)
        }
    }

    fun updateReceiptLineQuantity(index: Int, delta: Int) {
        val current = _uiState.value.receiptLines.toMutableList()
        if (index in current.indices) {
            val newQty = (current[index].quantity + delta).coerceAtLeast(1)
            current[index] = current[index].copy(quantity = newQty)
            _uiState.value = _uiState.value.copy(receiptLines = current)
        }
    }

    fun clearReceipt() {
        _uiState.value = _uiState.value.copy(receiptLines = emptyList())
    }

    fun applyReceiptToInventory(storeTypes: List<StoreTypeEntity>) {
        val selected = _uiState.value.receiptLines.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var updatedCount = 0
            var createdCount = 0
            for (line in selected) {
                val existing = line.matchedExistingItem
                if (existing != null) {
                    inventoryRepository.updateQuantity(
                        existing.id,
                        existing.quantity + line.quantity,
                        storeTypes
                    )
                    updatedCount++
                } else {
                    inventoryRepository.saveItem(
                        InventoryItemEntity(
                            name = line.productName,
                            categoryId = line.suggestedCategory,
                            quantity = line.quantity,
                            minThreshold = 1,
                            unit = line.unit,
                            preferredStoreId = storeTypes.firstOrNull()?.id ?: "supermarket",
                            location = when (line.suggestedCategory) {
                                "hygiene" -> "Salle de bain"
                                "pharmacie" -> "Pharmacie"
                                "bricolage" -> "Garage / Cave"
                                else -> "Placard"
                            },
                            notes = "Importé depuis ticket numérique"
                        ),
                        storeTypes
                    )
                    createdCount++
                }
            }
            _uiState.value = _uiState.value.copy(receiptLines = emptyList())
            _uiEvents.emit("Inventaire mis à jour : $updatedCount réapprovisionné(s), $createdCount nouveau(x) produit(s) !")
        }
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(
            scannedBarcode = null,
            isSearching = false,
            existingItem = null,
            onlineProduct = null,
            notFoundMessage = null
        )
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
