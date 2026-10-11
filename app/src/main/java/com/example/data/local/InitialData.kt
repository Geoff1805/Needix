package com.example.data.local

import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity

object InitialData {
    val defaultCategories = listOf(
        CategoryEntity(id = "cuisine", name = "Cuisine & Épices", iconEmoji = "🥫", colorHex = 0xFFF59E0BL, orderIndex = 0),
        CategoryEntity(id = "aliments", name = "Aliments & Frais", iconEmoji = "🍎", colorHex = 0xFF10B981L, orderIndex = 1),
        CategoryEntity(id = "menager", name = "Produits Ménagers", iconEmoji = "🧴", colorHex = 0xFF06B6D4L, orderIndex = 2),
        CategoryEntity(id = "hygiene", name = "Hygiène & Soins", iconEmoji = "🧼", colorHex = 0xFF8B5CF6L, orderIndex = 3),
        CategoryEntity(id = "pharmacie", name = "Pharmacie & Santé", iconEmoji = "💊", colorHex = 0xFFEF4444L, orderIndex = 4),
        CategoryEntity(id = "bricolage", name = "Bricolage & Maison", iconEmoji = "🔨", colorHex = 0xFF64748BL, orderIndex = 5),
        CategoryEntity(id = "autre", name = "Divers", iconEmoji = "📦", colorHex = 0xFF6B7280L, orderIndex = 6)
    )

    val defaultStoreTypes = listOf(
        StoreTypeEntity(id = "supermarket", name = "Supermarché", iconEmoji = "🛒", colorHex = 0xFF3B82F6L, orderIndex = 0),
        StoreTypeEntity(id = "pharmacy", name = "Pharmacie", iconEmoji = "💊", colorHex = 0xFFEF4444L, orderIndex = 1),
        StoreTypeEntity(id = "online", name = "Internet / En ligne", iconEmoji = "🌐", colorHex = 0xFF8B5CF6L, orderIndex = 2),
        StoreTypeEntity(id = "bio", name = "Marché / Bio", iconEmoji = "🥦", colorHex = 0xFF10B981L, orderIndex = 3),
        StoreTypeEntity(id = "brico", name = "Bricolage", iconEmoji = "🔨", colorHex = 0xFFF59E0BL, orderIndex = 4)
    )

    val sampleInventoryItems = listOf(
        InventoryItemEntity(
            id = 1,
            name = "Huile d'olive Vierge Extra",
            categoryId = "cuisine",
            quantity = 2,
            minThreshold = 1,
            unit = "bouteille",
            preferredStoreId = "supermarket",
            barcode = "3250390001001",
            location = "Placard cuisine",
            notes = "Privilégier première pression à froid"
        ),
        InventoryItemEntity(
            id = 2,
            name = "Lessive liquide écologique",
            categoryId = "menager",
            quantity = 1,
            minThreshold = 1,
            unit = "bidon",
            preferredStoreId = "supermarket",
            barcode = "3014260020123",
            location = "Buanderie"
        ),
        InventoryItemEntity(
            id = 3,
            name = "Liquide vaisselle citron",
            categoryId = "menager",
            quantity = 1,
            minThreshold = 1,
            unit = "flacon",
            preferredStoreId = "supermarket",
            location = "Sous-évier"
        ),
        InventoryItemEntity(
            id = 4,
            name = "Pâtes Penne Rigate",
            categoryId = "aliments",
            quantity = 3,
            minThreshold = 1,
            unit = "paquet",
            preferredStoreId = "supermarket",
            barcode = "8076809513753",
            location = "Garde-manger"
        ),
        InventoryItemEntity(
            id = 5,
            name = "Café pur arabica grains",
            categoryId = "cuisine",
            quantity = 1,
            minThreshold = 1,
            unit = "paquet",
            preferredStoreId = "supermarket",
            location = "Cuisine"
        ),
        InventoryItemEntity(
            id = 6,
            name = "Paracétamol 1000mg",
            categoryId = "pharmacie",
            quantity = 1,
            minThreshold = 1,
            unit = "boîte",
            preferredStoreId = "pharmacy",
            barcode = "3400936081498",
            location = "Armoire pharmacie",
            notes = "Vérifier date péremption"
        ),
        InventoryItemEntity(
            id = 7,
            name = "Gel douche hydratant",
            categoryId = "hygiene",
            quantity = 2,
            minThreshold = 1,
            unit = "flacon",
            preferredStoreId = "supermarket",
            location = "Salle de bain"
        ),
        InventoryItemEntity(
            id = 8,
            name = "Filtres aspirateur HEPA",
            categoryId = "bricolage",
            quantity = 0,
            minThreshold = 1,
            unit = "pièce",
            preferredStoreId = "online",
            location = "Placard entrée",
            notes = "Réf V11 sur internet"
        )
    )

    val sampleShoppingItems = listOf(
        ShoppingItemEntity(
            id = 1,
            inventoryItemId = 8,
            name = "Filtres aspirateur HEPA",
            categoryId = "bricolage",
            storeId = "online",
            quantityNeeded = 1,
            unit = "pièce",
            isPurchased = false,
            isOnHold = false
        ),
        ShoppingItemEntity(
            id = 2,
            inventoryItemId = null,
            name = "Vitamine C 500mg",
            categoryId = "pharmacie",
            storeId = "pharmacy",
            quantityNeeded = 1,
            unit = "boîte",
            isPurchased = false,
            isOnHold = true // En attente
        )
    )
}
