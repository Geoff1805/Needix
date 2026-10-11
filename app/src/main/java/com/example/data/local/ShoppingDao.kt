package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isPurchased ASC, addedAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE storeId = :storeId ORDER BY isPurchased ASC, addedAt DESC")
    fun getShoppingItemsByStore(storeId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :id LIMIT 1")
    suspend fun getShoppingItemById(id: Long): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_items WHERE inventoryItemId = :inventoryItemId LIMIT 1")
    suspend fun getShoppingItemByInventoryId(inventoryItemId: Long): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE shopping_items SET isOnHold = :onHold WHERE id = :id")
    suspend fun setOnHold(id: Long, onHold: Boolean)

    @Query("UPDATE shopping_items SET isPurchased = :purchased WHERE id = :id")
    suspend fun setPurchased(id: Long, purchased: Boolean)

    @Query("UPDATE shopping_items SET storeId = :newStoreId WHERE id = :id")
    suspend fun updateStore(id: Long, newStoreId: String)

    @Query("DELETE FROM shopping_items WHERE isPurchased = 1")
    suspend fun clearPurchasedItems()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()
}
