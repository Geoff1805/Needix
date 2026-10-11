package com.example.data.repository

import com.example.data.local.InventoryDao
import com.example.data.local.ShoppingDao
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class InventoryEvent {
    data class AddedToShoppingList(val itemName: String, val storeName: String) : InventoryEvent()
    data class ThresholdReached(
        val item: InventoryItemEntity,
        val currentQuantity: Int,
        val minThreshold: Int,
        val storeName: String
    ) : InventoryEvent()
}

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val shoppingDao: ShoppingDao
) {
    val allInventoryItems: Flow<List<InventoryItemEntity>> = inventoryDao.getAllInventoryItems()
    val allCategories: Flow<List<CategoryEntity>> = inventoryDao.getAllCategories()
    val allStoreTypes: Flow<List<StoreTypeEntity>> = inventoryDao.getAllStoreTypes()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events = _events.asSharedFlow()

    suspend fun getItemById(id: Long): InventoryItemEntity? = inventoryDao.getItemById(id)

    suspend fun getItemByBarcode(barcode: String): InventoryItemEntity? = inventoryDao.getItemByBarcode(barcode)

    suspend fun saveItem(item: InventoryItemEntity, storeTypes: List<StoreTypeEntity> = emptyList()): Long {
        val id = inventoryDao.insertItem(item)
        val savedItem = item.copy(id = if (item.id == 0L) id else item.id)
        val storeName = storeTypes.find { it.id == savedItem.preferredStoreId }?.name ?: savedItem.preferredStoreId

        // Proactive alert if stock is at or below minimum threshold
        if (savedItem.quantity <= savedItem.minThreshold) {
            _events.emit(InventoryEvent.ThresholdReached(savedItem, savedItem.quantity, savedItem.minThreshold, storeName))
        }

        // Auto-transfer to shopping list if quantity is 0 or less
        if (savedItem.quantity <= 0) {
            checkAndAddToShoppingList(savedItem, storeTypes)
        }
        return id
    }

    suspend fun updateQuantity(id: Long, newQuantity: Int, storeTypes: List<StoreTypeEntity>) {
        val qty = newQuantity.coerceAtLeast(0)
        inventoryDao.updateQuantity(id, qty)
        val item = inventoryDao.getItemById(id)
        if (item != null) {
            val storeName = storeTypes.find { it.id == item.preferredStoreId }?.name ?: item.preferredStoreId
            // Proactive alert if stock reaches or drops below minimum threshold
            if (qty <= item.minThreshold) {
                _events.emit(InventoryEvent.ThresholdReached(item, qty, item.minThreshold, storeName))
            }
            if (qty <= 0) {
                checkAndAddToShoppingList(item, storeTypes)
            }
        }
    }

    private suspend fun checkAndAddToShoppingList(
        item: InventoryItemEntity,
        storeTypes: List<StoreTypeEntity> = emptyList()
    ) {
        val existing = shoppingDao.getShoppingItemByInventoryId(item.id)
        if (existing == null) {
            val shoppingItem = ShoppingItemEntity(
                inventoryItemId = item.id,
                name = item.name,
                categoryId = item.categoryId,
                storeId = item.preferredStoreId,
                quantityNeeded = 1,
                unit = item.unit,
                isPurchased = false,
                isOnHold = false,
                barcode = item.barcode
            )
            shoppingDao.insertShoppingItem(shoppingItem)
            val storeName = storeTypes.find { it.id == item.preferredStoreId }?.name ?: item.preferredStoreId
            _events.emit(InventoryEvent.AddedToShoppingList(item.name, storeName))
        }
    }

    suspend fun deleteItem(item: InventoryItemEntity) {
        inventoryDao.deleteItem(item)
    }

    suspend fun deleteItemById(id: Long) {
        inventoryDao.deleteItemById(id)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        inventoryDao.insertCategory(category)
    }

    suspend fun insertStoreType(storeType: StoreTypeEntity) {
        inventoryDao.insertStoreType(storeType)
    }

    suspend fun importItems(items: List<InventoryItemEntity>, replaceAll: Boolean) {
        if (replaceAll) {
            inventoryDao.deleteAllInventoryItems()
        }
        for (item in items) {
            val insertedId = inventoryDao.insertItem(item)
            val savedItem = if (item.id == 0L) item.copy(id = insertedId) else item
            if (savedItem.quantity <= 0) {
                checkAndAddToShoppingList(savedItem)
            }
        }
    }

    suspend fun importFullBackup(backup: com.example.data.export.FullBackupData, replaceAll: Boolean) {
        if (replaceAll) {
            inventoryDao.deleteAllInventoryItems()
            shoppingDao.deleteAllShoppingItems()
        }
        if (backup.categories.isNotEmpty()) {
            inventoryDao.insertAllCategories(backup.categories)
        }
        if (backup.storeTypes.isNotEmpty()) {
            inventoryDao.insertAllStoreTypes(backup.storeTypes)
        }
        if (backup.shoppingItems.isNotEmpty()) {
            shoppingDao.insertAll(backup.shoppingItems)
        }
        importItems(backup.items, replaceAll = false)
    }
}
