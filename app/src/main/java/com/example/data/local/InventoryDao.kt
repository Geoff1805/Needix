package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY updatedAt DESC")
    fun getAllInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getItemsByCategory(categoryId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE inventory_items SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("UPDATE inventory_items SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignInventoryCategory(oldCategoryId: String, newCategoryId: String)

    @Query("SELECT * FROM store_types ORDER BY orderIndex ASC")
    fun getAllStoreTypes(): Flow<List<StoreTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreType(storeType: StoreTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStoreTypes(storeTypes: List<StoreTypeEntity>)

    @Delete
    suspend fun deleteStoreType(storeType: StoreTypeEntity)

    @Query("DELETE FROM store_types WHERE id = :id")
    suspend fun deleteStoreTypeById(id: String)

    @Query("UPDATE inventory_items SET preferredStoreId = :newStoreId WHERE preferredStoreId = :oldStoreId")
    suspend fun reassignInventoryStore(oldStoreId: String, newStoreId: String)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllInventoryItems()
}
