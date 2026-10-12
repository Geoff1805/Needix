package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.ImportResult
import com.example.data.export.SheetsCsvManager
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.repository.InventoryEvent
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.ui.notification.NotificationHelper
import com.example.ui.notification.ThresholdAlert

enum class SortOption(val label: String) {
    RECENT("Récents"),
    NAME("Nom (A-Z)"),
    QUANTITY_ASC("Stock croissant"),
    QUANTITY_DESC("Stock décroissant")
}

data class InventoryUiState(
    val items: List<InventoryItemEntity> = emptyList(),
    val filteredItems: List<InventoryItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val storeTypes: List<StoreTypeEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RECENT,
    val filterLowStockOnly: Boolean = false,
    val totalItemCount: Int = 0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val activeThresholdAlerts: List<ThresholdAlert> = emptyList(),
    val notificationsEnabled: Boolean = true
)

data class InventoryFilterState(
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RECENT,
    val filterLowStockOnly: Boolean = false
)

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(InventoryFilterState())
    private val _notificationsEnabled = MutableStateFlow(true)

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _thresholdAlertEvents = MutableSharedFlow<ThresholdAlert>()
    val thresholdAlertEvents = _thresholdAlertEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is InventoryEvent.AddedToShoppingList -> {
                        _uiEvents.emit("« ${event.itemName} » est épuisé -> Ajouté à la liste [${event.storeName}]")
                    }
                    is InventoryEvent.ThresholdReached -> {
                        val alert = ThresholdAlert(
                            id = "${event.item.id}_${System.currentTimeMillis()}",
                            itemId = event.item.id,
                            itemName = event.item.name,
                            currentQuantity = event.currentQuantity,
                            minThreshold = event.minThreshold,
                            unit = event.item.unit,
                            storeName = event.storeName
                        )
                        _thresholdAlertEvents.emit(alert)
                        val alertMsg = if (event.currentQuantity <= 0) {
                            "🚨 Rupture : « ${event.item.name} » est à zéro ! (Ajouté à la liste ${event.storeName})"
                        } else {
                            "⚠️ Seuil critique : « ${event.item.name} » n'a plus que ${event.currentQuantity} ${event.item.unit} (seuil min : ${event.minThreshold})"
                        }
                        _uiEvents.emit(alertMsg)
                    }
                }
            }
        }
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.allInventoryItems,
        repository.allCategories,
        repository.allStoreTypes,
        _filterState,
        _notificationsEnabled
    ) { items: List<InventoryItemEntity>, categories: List<CategoryEntity>, storeTypes: List<StoreTypeEntity>, filter: InventoryFilterState, notificationsEnabled: Boolean ->
        var list = items

        // Filter by category
        if (filter.selectedCategoryId != null) {
            list = list.filter { it.categoryId == filter.selectedCategoryId }
        }

        // Filter by search query
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.location.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q) ||
                        (it.barcode != null && it.barcode.contains(q))
            }
        }

        // Filter by low stock
        if (filter.filterLowStockOnly) {
            list = list.filter { it.quantity <= it.minThreshold }
        }

        // Sort
        list = when (filter.sortOption) {
            SortOption.RECENT -> list.sortedByDescending { it.updatedAt }
            SortOption.NAME -> list.sortedBy { it.name.lowercase() }
            SortOption.QUANTITY_ASC -> list.sortedBy { it.quantity }
            SortOption.QUANTITY_DESC -> list.sortedByDescending { it.quantity }
        }

        val outOfStock = items.count { it.quantity == 0 }
        val lowStock = items.count { it.quantity > 0 && it.quantity <= it.minThreshold }

        // Compute all current active threshold alerts
        val alerts = items
            .filter { it.quantity <= it.minThreshold }
            .map { item ->
                val storeName = storeTypes.find { it.id == item.preferredStoreId }?.name ?: item.preferredStoreId
                ThresholdAlert(
                    id = "${item.id}",
                    itemId = item.id,
                    itemName = item.name,
                    currentQuantity = item.quantity,
                    minThreshold = item.minThreshold,
                    unit = item.unit,
                    storeName = storeName
                )
            }

        InventoryUiState(
            items = items,
            filteredItems = list,
            categories = categories,
            storeTypes = storeTypes,
            selectedCategoryId = filter.selectedCategoryId,
            searchQuery = filter.searchQuery,
            sortOption = filter.sortOption,
            filterLowStockOnly = filter.filterLowStockOnly,
            totalItemCount = items.size,
            outOfStockCount = outOfStock,
            lowStockCount = lowStock,
            activeThresholdAlerts = alerts,
            notificationsEnabled = notificationsEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    fun onCategorySelect(categoryId: String?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = categoryId)
    }

    fun onSearchQueryChange(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun onSortOptionChange(sort: SortOption) {
        _filterState.value = _filterState.value.copy(sortOption = sort)
    }

    fun toggleLowStockFilter() {
        _filterState.value = _filterState.value.copy(filterLowStockOnly = !_filterState.value.filterLowStockOnly)
    }

    fun incrementQuantity(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.updateQuantity(item.id, item.quantity + 1, uiState.value.storeTypes)
        }
    }

    fun decrementQuantity(item: InventoryItemEntity) {
        viewModelScope.launch {
            if (item.quantity > 0) {
                repository.updateQuantity(item.id, item.quantity - 1, uiState.value.storeTypes)
            }
        }
    }

    fun setQuantity(item: InventoryItemEntity, newQty: Int) {
        viewModelScope.launch {
            repository.updateQuantity(item.id, newQty.coerceAtLeast(0), uiState.value.storeTypes)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun saveItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.saveItem(item, uiState.value.storeTypes)
            _uiEvents.emit("Article « ${item.name} » enregistré")
        }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _uiEvents.emit("« ${item.name} » supprimé")
        }
    }

    fun addCategory(name: String, emoji: String, colorHex: Long) {
        viewModelScope.launch {
            val id = name.trim().lowercase().replace("\\s+".toRegex(), "_")
            repository.insertCategory(
                CategoryEntity(
                    id = id,
                    name = name.trim(),
                    iconEmoji = emoji.ifBlank { "📦" },
                    colorHex = colorHex,
                    orderIndex = uiState.value.categories.size
                )
            )
            _uiEvents.emit("Catégorie « ${name.trim()} » ajoutée")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val currentList = uiState.value.categories
            if (currentList.size <= 1) {
                _uiEvents.emit("Impossible de supprimer la seule catégorie restante")
                return@launch
            }
            val remaining = currentList.filter { it.id != category.id }
            val fallback = remaining.find { it.id == "autre" }?.id ?: remaining.first().id
            repository.deleteCategory(category, fallback)
            if (_filterState.value.selectedCategoryId == category.id) {
                _filterState.value = _filterState.value.copy(selectedCategoryId = null)
            }
            _uiEvents.emit("Catégorie « ${category.name} » supprimée")
        }
    }

    fun addStoreType(name: String, emoji: String, colorHex: Long) {
        viewModelScope.launch {
            val id = name.trim().lowercase().replace("\\s+".toRegex(), "_")
            repository.insertStoreType(
                StoreTypeEntity(
                    id = id,
                    name = name.trim(),
                    iconEmoji = emoji.ifBlank { "🛒" },
                    colorHex = colorHex,
                    orderIndex = uiState.value.storeTypes.size
                )
            )
            _uiEvents.emit("Magasin « ${name.trim()} » ajouté")
        }
    }

    fun deleteStoreType(storeType: StoreTypeEntity) {
        viewModelScope.launch {
            val currentList = uiState.value.storeTypes
            if (currentList.size <= 1) {
                _uiEvents.emit("Impossible de supprimer le seul magasin restant")
                return@launch
            }
            val remaining = currentList.filter { it.id != storeType.id }
            val fallback = remaining.find { it.id == "supermarket" }?.id ?: remaining.first().id
            repository.deleteStoreType(storeType, fallback)
            _uiEvents.emit("Magasin « ${storeType.name} » supprimé")
        }
    }

    fun importCsv(context: Context, uri: Uri, replaceAll: Boolean, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val rawContent = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }?.removePrefix("\uFEFF")?.trim()

                if (rawContent.isNullOrBlank()) {
                    _uiEvents.emit("Fichier vide ou illisible")
                    onComplete(0)
                    return@launch
                }

                if (rawContent.startsWith("{") || rawContent.startsWith("[")) {
                    val backup = com.example.data.export.UniversalExportManager.parseJsonBackup(rawContent)
                    if (backup != null && (backup.items.isNotEmpty() || backup.shoppingItems.isNotEmpty())) {
                        repository.importFullBackup(backup, replaceAll)
                        _uiEvents.emit("${backup.items.size} articles restaurés depuis la sauvegarde JSON")
                        onComplete(backup.items.size)
                    } else {
                        _uiEvents.emit("Aucun article valide trouvé dans la sauvegarde JSON")
                        onComplete(0)
                    }
                } else {
                    val parsedItems = com.example.data.export.UniversalExportManager.parseCsv(rawContent)
                    if (parsedItems.isNotEmpty()) {
                        repository.importItems(parsedItems, replaceAll)
                        _uiEvents.emit("${parsedItems.size} articles importés avec succès")
                        onComplete(parsedItems.size)
                    } else {
                        _uiEvents.emit("Aucun article valide trouvé dans le fichier")
                        onComplete(0)
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit("Erreur lors de l'import: ${e.message}")
                onComplete(0)
            }
        }
    }
}

class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
