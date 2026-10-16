package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.network.ProductLookupResult
import com.example.data.network.ProductLookupService
import com.example.data.receipt.ParsedReceiptLine
import com.example.data.receipt.ReceiptParser
import com.example.data.recipe.ParsedRecipeIngredient
import com.example.data.recipe.ParsedRecipeResult
import com.example.data.recipe.RecipeIngredientParser
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Les 5 comportements de scan en magasin testables en direct (Lab Mode Magasin).
 */
enum class InStoreScanBehavior(val id: String, val label: String, val badge: String) {
    ALL_IN_ONE("all", "⭐ 5. Tout-en-un", "Combiné"),
    CHECK_OWNED("opt1", "🔍 1. Anti-Doublon", "Bullix / Grocy"),
    INSTANT_RESTOCK("opt2", "⚡ 2. Express 1-Tap", "Bring!"),
    CART_BATCH("opt3", "🛒 3. Mode Caddie", "Scan & Go"),
    NUTRI_DETAIL("opt4", "🥗 4. Fiche Nutri", "Open Food Facts")
}

data class InStoreCartItem(
    val barcode: String,
    val name: String,
    val categoryId: String,
    val unit: String,
    val location: String,
    val quantity: Int = 1,
    val existingInventoryId: Long? = null,
    val matchingShoppingItemId: Long? = null,
    val wasOnShoppingList: Boolean = false
)

data class UndoExpressAction(
    val itemId: Long,
    val itemName: String,
    val previousQuantity: Int,
    val shoppingItemId: Long?,
    val wasShoppingPurchased: Boolean
)

data class ScannerUiState(
    val activeTabMode: String = "barcode", // "barcode", "recipe", "receipt"
    val scanBehavior: InStoreScanBehavior = InStoreScanBehavior.ALL_IN_ONE,
    val scannedBarcode: String? = null,
    val isSearching: Boolean = false,
    val existingItem: InventoryItemEntity? = null,
    val matchingShoppingItem: ShoppingItemEntity? = null,
    val onlineProduct: ProductLookupResult? = null,
    val notFoundMessage: String? = null,
    val inStoreCart: List<InStoreCartItem> = emptyList(),
    val lastExpressUndo: UndoExpressAction? = null,
    val receiptLines: List<ParsedReceiptLine> = emptyList(),
    val isAnalyzingRecipe: Boolean = false,
    val recipeResult: ParsedRecipeResult? = null
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
        DemoBarcode("3017620422003", "🍫 Nutella 400g", "aliments"),
        DemoBarcode("3250390001001", "🫒 Huile d'olive Puget", "cuisine"),
        DemoBarcode("3600523724451", "🧴 Gel Douche Mixa", "hygiene"),
        DemoBarcode("3014260020123", "🧼 Lessive Ariel", "menager"),
        DemoBarcode("3400936081498", "💊 Doliprane 1000mg", "pharmacie"),
        DemoBarcode("9782070612758", "📚 Le Petit Prince (ISBN)", "autre")
    )

    fun setActiveTabMode(mode: String) {
        _uiState.value = _uiState.value.copy(activeTabMode = mode)
    }

    fun setScanBehavior(behavior: InStoreScanBehavior) {
        _uiState.value = _uiState.value.copy(scanBehavior = behavior)
    }

    fun onBarcodeDetected(
        barcode: String,
        forceNewScan: Boolean = false,
        storeTypes: List<StoreTypeEntity> = emptyList()
    ) {
        if (_uiState.value.isSearching) return
        val currentBehavior = _uiState.value.scanBehavior

        // En mode Caddie ou Express, permettre d'enchaîner des codes-barres différents sans blocage
        if (!forceNewScan && _uiState.value.scannedBarcode != null) {
            val cleanCandidate = ProductLookupService.normalizeBarcode(barcode)
            if (cleanCandidate == _uiState.value.scannedBarcode) return
            if (currentBehavior != InStoreScanBehavior.CART_BATCH && currentBehavior != InStoreScanBehavior.INSTANT_RESTOCK) {
                return
            }
        }

        val cleanBarcode = ProductLookupService.normalizeBarcode(barcode)
        if (cleanBarcode.isBlank()) return

        _beepEvents.tryEmit(Unit)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scannedBarcode = cleanBarcode,
                isSearching = true,
                existingItem = null,
                matchingShoppingItem = null,
                onlineProduct = null,
                notFoundMessage = null
            )

            // 1. Vérifier l'inventaire local + la liste de courses active
            val localItem = inventoryRepository.getItemByBarcode(cleanBarcode)
            val shoppingMatch = inventoryRepository.findMatchingShoppingItem(
                barcode = cleanBarcode,
                inventoryItemId = localItem?.id,
                name = localItem?.name
            )

            if (localItem != null) {
                // Comportement Option 2 (Validation Express 1-Tap)
                if (currentBehavior == InStoreScanBehavior.INSTANT_RESTOCK) {
                    val incrementBy = if (shoppingMatch != null && !shoppingMatch.isPurchased) {
                        shoppingMatch.quantityNeeded.coerceAtLeast(1)
                    } else {
                        1
                    }
                    val prevQty = localItem.quantity
                    val newQty = prevQty + incrementBy
                    inventoryRepository.updateQuantity(localItem.id, newQty, storeTypes)
                    if (shoppingMatch != null) {
                        inventoryRepository.markShoppingItemPurchased(shoppingMatch.id, true)
                    }
                    val updatedLocal = localItem.copy(quantity = newQty)
                    _uiState.value = _uiState.value.copy(
                        scannedBarcode = cleanBarcode,
                        isSearching = false,
                        existingItem = updatedLocal,
                        matchingShoppingItem = shoppingMatch?.copy(isPurchased = true),
                        lastExpressUndo = UndoExpressAction(
                            itemId = localItem.id,
                            itemName = localItem.name,
                            previousQuantity = prevQty,
                            shoppingItemId = shoppingMatch?.id,
                            wasShoppingPurchased = shoppingMatch?.isPurchased ?: false
                        )
                    )
                    _uiEvents.emit("⚡ Express : « ${localItem.name} » +$incrementBy en stock (Total: $newQty) !")
                    return@launch
                }

                // Comportement Option 3 (Mode Caddie Continu)
                if (currentBehavior == InStoreScanBehavior.CART_BATCH) {
                    addOrIncrementCartItem(
                        InStoreCartItem(
                            barcode = cleanBarcode,
                            name = localItem.name,
                            categoryId = localItem.categoryId,
                            unit = localItem.unit,
                            location = localItem.location,
                            quantity = shoppingMatch?.quantityNeeded?.coerceAtLeast(1) ?: 1,
                            existingInventoryId = localItem.id,
                            matchingShoppingItemId = shoppingMatch?.id,
                            wasOnShoppingList = shoppingMatch != null
                        )
                    )
                }

                // Enrichir aussi avec Open Food Facts si Option 4 (Fiche Nutri) ou Tout-en-un
                val online = ProductLookupService.lookupBarcode(cleanBarcode)
                _uiState.value = _uiState.value.copy(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    existingItem = localItem,
                    matchingShoppingItem = shoppingMatch,
                    onlineProduct = online
                )
                return@launch
            }

            // 2. Produit non encore en stock : interroger la cascade Multi-API & Base Hors-ligne
            val online = ProductLookupService.lookupBarcode(cleanBarcode)
            if (online != null) {
                val shopByName = shoppingMatch ?: inventoryRepository.findMatchingShoppingItem(
                    barcode = cleanBarcode,
                    inventoryItemId = null,
                    name = online.name
                )

                if (currentBehavior == InStoreScanBehavior.CART_BATCH) {
                    addOrIncrementCartItem(
                        InStoreCartItem(
                            barcode = cleanBarcode,
                            name = online.name,
                            categoryId = online.suggestedCategory,
                            unit = online.suggestedUnit,
                            location = defaultLocationForCategory(online.suggestedCategory),
                            quantity = shopByName?.quantityNeeded?.coerceAtLeast(1) ?: 1,
                            existingInventoryId = null,
                            matchingShoppingItemId = shopByName?.id,
                            wasOnShoppingList = shopByName != null
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    scannedBarcode = cleanBarcode,
                    isSearching = false,
                    matchingShoppingItem = shopByName,
                    onlineProduct = online
                )
                _uiEvents.emit("Code-barres lu : « ${online.name} » (${online.source})")
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

    private fun defaultLocationForCategory(category: String): String = when (category) {
        "hygiene" -> "Salle de bain"
        "pharmacie" -> "Pharmacie"
        "bricolage" -> "Garage / Cave"
        "menager" -> "Placard"
        else -> "Cuisine"
    }

    fun quickIncrementExisting(
        item: InventoryItemEntity,
        storeTypes: List<StoreTypeEntity>,
        delta: Int = 1
    ) {
        viewModelScope.launch {
            val newQty = (item.quantity + delta).coerceAtLeast(0)
            inventoryRepository.updateQuantity(item.id, newQty, storeTypes)
            val shopMatch = _uiState.value.matchingShoppingItem
            if (delta > 0 && shopMatch != null && !shopMatch.isPurchased) {
                inventoryRepository.markShoppingItemPurchased(shopMatch.id, true)
            }
            _uiState.value = _uiState.value.copy(
                existingItem = item.copy(quantity = newQty),
                matchingShoppingItem = if (delta > 0) shopMatch?.copy(isPurchased = true) else shopMatch
            )
            val sign = if (delta >= 0) "+$delta" else "$delta"
            _uiEvents.emit("Stock mis à jour ($sign) pour « ${item.name} » → $newQty ${item.unit}")
        }
    }

    fun undoLastExpressRestock(storeTypes: List<StoreTypeEntity>) {
        val undo = _uiState.value.lastExpressUndo ?: return
        viewModelScope.launch {
            inventoryRepository.updateQuantity(undo.itemId, undo.previousQuantity, storeTypes)
            if (undo.shoppingItemId != null) {
                inventoryRepository.markShoppingItemPurchased(undo.shoppingItemId, undo.wasShoppingPurchased)
            }
            val refreshed = inventoryRepository.getItemById(undo.itemId)
            _uiState.value = _uiState.value.copy(
                existingItem = refreshed,
                lastExpressUndo = null
            )
            _uiEvents.emit("↩ Scan annulé pour « ${undo.itemName} » (Stock : ${undo.previousQuantity})")
        }
    }

    // ---------------------------------------------------------
    // Mode 3 : Panier / Caddie Continu (Scan & Go en magasin)
    // ---------------------------------------------------------

    fun addCurrentScannedToCart() {
        val state = _uiState.value
        val existing = state.existingItem
        val online = state.onlineProduct
        val barcode = state.scannedBarcode ?: return

        val cartItem = when {
            existing != null -> InStoreCartItem(
                barcode = barcode,
                name = existing.name,
                categoryId = existing.categoryId,
                unit = existing.unit,
                location = existing.location,
                quantity = 1,
                existingInventoryId = existing.id,
                matchingShoppingItemId = state.matchingShoppingItem?.id,
                wasOnShoppingList = state.matchingShoppingItem != null
            )
            online != null -> InStoreCartItem(
                barcode = barcode,
                name = online.name,
                categoryId = online.suggestedCategory,
                unit = online.suggestedUnit,
                location = defaultLocationForCategory(online.suggestedCategory),
                quantity = 1,
                existingInventoryId = null,
                matchingShoppingItemId = state.matchingShoppingItem?.id,
                wasOnShoppingList = state.matchingShoppingItem != null
            )
            else -> return
        }
        addOrIncrementCartItem(cartItem)
        viewModelScope.launch {
            _uiEvents.emit("🛒 « ${cartItem.name} » ajouté à votre caddie en cours")
        }
    }

    private fun addOrIncrementCartItem(newItem: InStoreCartItem) {
        val current = _uiState.value.inStoreCart.toMutableList()
        val idx = current.indexOfFirst { it.barcode == newItem.barcode }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = current[idx].quantity + 1)
        } else {
            current.add(0, newItem)
        }
        _uiState.value = _uiState.value.copy(inStoreCart = current)
    }

    fun updateCartItemQuantity(barcode: String, delta: Int) {
        val current = _uiState.value.inStoreCart.toMutableList()
        val idx = current.indexOfFirst { it.barcode == barcode }
        if (idx >= 0) {
            val newQty = current[idx].quantity + delta
            if (newQty <= 0) {
                current.removeAt(idx)
            } else {
                current[idx] = current[idx].copy(quantity = newQty)
            }
            _uiState.value = _uiState.value.copy(inStoreCart = current)
        }
    }

    fun clearInStoreCart() {
        _uiState.value = _uiState.value.copy(inStoreCart = emptyList())
    }

    fun checkoutInStoreCart(storeTypes: List<StoreTypeEntity>) {
        val cart = _uiState.value.inStoreCart
        if (cart.isEmpty()) return

        viewModelScope.launch {
            var totalUnits = 0
            for (item in cart) {
                totalUnits += item.quantity
                val local = item.existingInventoryId?.let { inventoryRepository.getItemById(it) }
                    ?: inventoryRepository.getItemByBarcode(item.barcode)

                if (local != null) {
                    inventoryRepository.updateQuantity(local.id, local.quantity + item.quantity, storeTypes)
                } else {
                    inventoryRepository.saveItem(
                        InventoryItemEntity(
                            name = item.name,
                            categoryId = item.categoryId,
                            quantity = item.quantity,
                            minThreshold = 1,
                            unit = item.unit,
                            preferredStoreId = storeTypes.firstOrNull()?.id ?: "supermarket",
                            barcode = item.barcode,
                            location = item.location,
                            notes = "Acheté en Mode Magasin"
                        ),
                        storeTypes
                    )
                }

                if (item.matchingShoppingItemId != null) {
                    inventoryRepository.markShoppingItemPurchased(item.matchingShoppingItemId, true)
                }
            }

            _beepEvents.tryEmit(Unit)
            _uiState.value = _uiState.value.copy(inStoreCart = emptyList())
            reset()
            _uiEvents.emit("✓ Passage en caisse validé : ${cart.size} référence(s) ($totalUnits art.) ajoutées au stock !")
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
        val shopMatch = _uiState.value.matchingShoppingItem
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
            if (shopMatch != null && quantity > 0) {
                inventoryRepository.markShoppingItemPurchased(shopMatch.id, true)
            }
            _uiEvents.emit("« $name » ajouté à l'inventaire")
            reset()
        }
    }

    // ---------------------------------------------------------
    // Module Recettes : Screenshot d'ingrédients (OCR) & Lien URL
    // ---------------------------------------------------------

    fun analyzeRecipeTextOrOcr(
        rawText: String,
        sourceLabel: String = "📸 Capture d'écran d'ingrédients (OCR)"
    ) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeTabMode = "recipe",
                isAnalyzingRecipe = true
            )
            val currentItems = inventoryRepository.allInventoryItems.first()
            val result = RecipeIngredientParser.parseRecipeText(
                rawText = rawText,
                existingInventory = currentItems,
                sourceLabel = sourceLabel
            )
            _uiState.value = _uiState.value.copy(
                isAnalyzingRecipe = false,
                recipeResult = result
            )
            if (result.ingredients.isNotEmpty()) {
                _beepEvents.tryEmit(Unit)
                val missingCount = result.ingredients.count { it.isMissing }
                _uiEvents.emit("🍳 Recette analysée : ${result.ingredients.size} ingrédient(s) dont $missingCount manquant(s) !")
            } else {
                _uiEvents.emit("Aucun ingrédient détecté dans ce texte/screenshot.")
            }
        }
    }

    fun analyzeRecipeUrl(urlOrText: String) {
        if (urlOrText.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeTabMode = "recipe",
                isAnalyzingRecipe = true
            )
            val currentItems = inventoryRepository.allInventoryItems.first()
            val result = RecipeIngredientParser.fetchAndParseRecipeFromUrl(
                url = urlOrText,
                existingInventory = currentItems
            )
            _uiState.value = _uiState.value.copy(
                isAnalyzingRecipe = false,
                recipeResult = result
            )
            if (result.ingredients.isNotEmpty()) {
                _beepEvents.tryEmit(Unit)
                val missingCount = result.ingredients.count { it.isMissing }
                _uiEvents.emit("🔗 Recette « ${result.title} » : $missingCount ingrédient(s) manquant(s) pré-coché(s) !")
            } else {
                _uiEvents.emit("Impossible d'extraire les ingrédients de ce lien.")
            }
        }
    }

    fun toggleRecipeIngredientSelection(index: Int) {
        val currentRecipe = _uiState.value.recipeResult ?: return
        val list = currentRecipe.ingredients.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelectedForShopping = !list[index].isSelectedForShopping)
            _uiState.value = _uiState.value.copy(
                recipeResult = currentRecipe.copy(ingredients = list)
            )
        }
    }

    fun updateRecipeIngredientQuantity(index: Int, delta: Int) {
        val currentRecipe = _uiState.value.recipeResult ?: return
        val list = currentRecipe.ingredients.toMutableList()
        if (index in list.indices) {
            val newQty = (list[index].suggestedQuantityToBuy + delta).coerceAtLeast(1)
            list[index] = list[index].copy(suggestedQuantityToBuy = newQty)
            _uiState.value = _uiState.value.copy(
                recipeResult = currentRecipe.copy(ingredients = list)
            )
        }
    }

    fun clearRecipeResult() {
        _uiState.value = _uiState.value.copy(recipeResult = null)
    }

    fun addSelectedRecipeIngredientsToShoppingList(storeTypes: List<StoreTypeEntity>) {
        val currentRecipe = _uiState.value.recipeResult ?: return
        val selected = currentRecipe.ingredients.filter { it.isSelectedForShopping }
        if (selected.isEmpty()) return

        val defaultStoreId = storeTypes.firstOrNull()?.id ?: "supermarket"
        viewModelScope.launch {
            for (ing in selected) {
                inventoryRepository.addRecipeIngredientToShoppingList(
                    name = ing.ingredientName,
                    categoryId = ing.suggestedCategory,
                    storeId = ing.matchedInventoryItem?.preferredStoreId ?: defaultStoreId,
                    quantityNeeded = ing.suggestedQuantityToBuy,
                    unit = ing.unit,
                    inventoryItemId = ing.matchedInventoryItem?.id,
                    barcode = ing.matchedInventoryItem?.barcode
                )
            }
            _beepEvents.tryEmit(Unit)
            _uiState.value = _uiState.value.copy(recipeResult = null)
            _uiEvents.emit("🛒 ${selected.size} ingrédient(s) manquant(s) ajouté(s) à votre liste de courses !")
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
            matchingShoppingItem = null,
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
