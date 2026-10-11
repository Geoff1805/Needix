package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val colorHex: Long,
    val orderIndex: Int = 0
)

@Entity(tableName = "store_types")
data class StoreTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val colorHex: Long,
    val orderIndex: Int = 0
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: String,
    val quantity: Int = 1,
    val minThreshold: Int = 1,
    val unit: String = "pièce",
    val preferredStoreId: String = "supermarket",
    val barcode: String? = null,
    val location: String = "Maison",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long? = null,
    val name: String,
    val categoryId: String,
    val storeId: String = "supermarket",
    val quantityNeeded: Int = 1,
    val unit: String = "pièce",
    val isPurchased: Boolean = false,
    val isOnHold: Boolean = false, // "En attente" si pas besoin immédiatement
    val barcode: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
