package com.example.data.repository

import com.example.data.local.InventoryDao
import com.example.data.local.ShoppingDao
import com.example.data.model.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val inventoryDao: InventoryDao
) {
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllShoppingItems()

    suspend fun insertItem(item: ShoppingItemEntity): Long {
        return shoppingDao.insertShoppingItem(item)
    }

    suspend fun updateItem(item: ShoppingItemEntity) {
        shoppingDao.updateShoppingItem(item)
    }

    suspend fun deleteItem(item: ShoppingItemEntity) {
        shoppingDao.deleteShoppingItem(item)
    }

    suspend fun deleteById(id: Long) {
        shoppingDao.deleteById(id)
    }

    suspend fun setOnHold(id: Long, onHold: Boolean) {
        shoppingDao.setOnHold(id, onHold)
    }

    suspend fun setPurchased(id: Long, purchased: Boolean) {
        shoppingDao.setPurchased(id, purchased)
    }

    suspend fun updateStore(id: Long, newStoreId: String) {
        shoppingDao.updateStore(id, newStoreId)
        // Also update preferredStoreId in inventory if linked
        val item = shoppingDao.getShoppingItemByInventoryId(id)
        if (item?.inventoryItemId != null) {
            val invItem = inventoryDao.getItemById(item.inventoryItemId)
            if (invItem != null) {
                inventoryDao.updateItem(invItem.copy(preferredStoreId = newStoreId))
            }
        }
    }

    /**
     * When user buys an item, restock back into inventory and mark purchased or remove
     */
    suspend fun restockPurchasedItem(shoppingItem: ShoppingItemEntity, restockQty: Int, removeFromList: Boolean) {
        if (shoppingItem.inventoryItemId != null) {
            val invItem = inventoryDao.getItemById(shoppingItem.inventoryItemId)
            if (invItem != null) {
                inventoryDao.updateQuantity(
                    invItem.id,
                    invItem.quantity + restockQty
                )
            }
        }
        if (removeFromList) {
            shoppingDao.deleteShoppingItem(shoppingItem)
        } else {
            shoppingDao.setPurchased(shoppingItem.id, true)
        }
    }

    suspend fun clearPurchased() {
        shoppingDao.clearPurchasedItems()
    }
}
