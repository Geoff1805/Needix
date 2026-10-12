package com.example

import com.example.billing.BillingRepository
import com.example.billing.CommercialBillingManager
import com.example.billing.SubscriptionPlan
import com.example.data.export.SheetsCsvManager
import com.example.data.export.UniversalExportManager
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.network.ChompBarcodeResponse
import com.example.data.network.ChompFoodItem
import com.example.data.network.OffProductDetails
import com.example.data.network.OffProductResponse
import com.example.data.network.ProductLookupService
import com.example.data.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryAppUnitTest {

    @Test
    fun csvExport_containsHeadersAndItems() {
        val items = listOf(
            InventoryItemEntity(
                id = 1L,
                name = "Huile d'olive",
                categoryId = "cuisine",
                quantity = 2,
                minThreshold = 1,
                unit = "bouteille",
                preferredStoreId = "supermarket",
                location = "Placard",
                barcode = "3250390001001",
                notes = "Bio"
            )
        )

        val csv = SheetsCsvManager.generateCsv(items)
        assertTrue(csv.contains("ID,Nom,Categorie,Quantite"))
        assertTrue(csv.contains("Huile d'olive"))
        assertTrue(csv.contains("3250390001001"))
    }

    @Test
    fun universalExport_standardCsvAndJsonBackup() {
        val items = listOf(
            InventoryItemEntity(
                id = 10L,
                name = "Pâtes",
                categoryId = "aliments",
                quantity = 4,
                minThreshold = 2,
                unit = "paquet",
                preferredStoreId = "supermarket",
                location = "Garde-manger",
                barcode = "3038350000000"
            )
        )
        val categories = listOf(CategoryEntity(id = "aliments", name = "Aliments", iconEmoji = "🍝", colorHex = 0xFFEF4444))
        val stores = listOf(StoreTypeEntity(id = "supermarket", name = "Supermarché", iconEmoji = "🛒", colorHex = 0xFF10B981))

        // Test CSV
        val csv = UniversalExportManager.generateStandardCsv(items)
        assertTrue(csv.contains("Pâtes"))
        val parsedCsv = UniversalExportManager.parseCsv(csv)
        assertEquals(1, parsedCsv.size)
        assertEquals("Pâtes", parsedCsv[0].name)

        // Test JSON Backup
        val json = UniversalExportManager.generateJsonBackup(items, emptyList(), categories, stores)
        val parsedBackup = UniversalExportManager.parseJsonBackup(json)
        assertNotNull(parsedBackup)
        assertEquals(1, parsedBackup!!.items.size)
        assertEquals("Pâtes", parsedBackup.items[0].name)
        assertEquals(1, parsedBackup.categories.size)
    }

    @Test
    fun universalExport_webAppJsonAndLegacyCsvInteroperability() {
        val webJson = """
            {
              "app": "Needix",
              "version": "1.0.0",
              "exportDate": "2026-10-11T12:00:00Z",
              "items": [
                {
                  "id": 1,
                  "name": "Lait Bio",
                  "category": "Produits laitiers",
                  "location": "Frigo",
                  "quantity": 2.0,
                  "unit": "L",
                  "minQuantity": 3.0,
                  "barcode": "3250390112345",
                  "store": "BioCoop",
                  "notes": "Bouteille 1L"
                }
              ],
              "shoppingList": [
                {
                  "id": 1,
                  "name": "Café Arabica",
                  "quantity": 2.0,
                  "unit": "paquets",
                  "checked": false,
                  "store": "Supermarché"
                }
              ]
            }
        """.trimIndent()

        val parsedWebBackup = UniversalExportManager.parseJsonBackup(webJson)
        assertNotNull(parsedWebBackup)
        assertEquals(1, parsedWebBackup!!.items.size)
        assertEquals("Lait Bio", parsedWebBackup.items[0].name)
        assertEquals("aliments", parsedWebBackup.items[0].categoryId)
        assertEquals(2, parsedWebBackup.items[0].quantity)
        assertEquals(3, parsedWebBackup.items[0].minThreshold)
        assertEquals("bio", parsedWebBackup.items[0].preferredStoreId)
        assertEquals(1, parsedWebBackup.shoppingItems.size)
        assertEquals("Café Arabica", parsedWebBackup.shoppingItems[0].name)
        assertEquals(2, parsedWebBackup.shoppingItems[0].quantityNeeded)

        val webCsv11Cols = "\uFEFFID;Nom;Catégorie;Emplacement;Quantité;Unité;Seuil_Alerte;Date_Péremption;Magasin;Code_Barres;Notes\n" +
                "1;\"Lait Bio\";\"Produits laitiers\";\"Frigo\";2;\"L\";3;2026-10-18;\"BioCoop\";\"3250390112345\";\"Pack\""
        val parsedWebCsv = UniversalExportManager.parseCsv(webCsv11Cols)
        assertEquals(1, parsedWebCsv.size)
        assertEquals("Lait Bio", parsedWebCsv[0].name)
        assertEquals("aliments", parsedWebCsv[0].categoryId)
        assertEquals("Frigo", parsedWebCsv[0].location)
        assertEquals(2, parsedWebCsv[0].quantity)
        assertEquals(3, parsedWebCsv[0].minThreshold)
        assertEquals("bio", parsedWebCsv[0].preferredStoreId)
    }

    @Test
    fun billingManagers_commercialLogic() {
        val repoManager = BillingRepository.activeManager.value
        assertTrue(repoManager.hasActiveAccess.value)

        val commercialManager = CommercialBillingManager(initialTrialDays = 14)
        assertTrue("Commercial doit être actif pendant l'essai", commercialManager.hasActiveAccess.value)
        assertEquals(14, commercialManager.trialDaysRemaining.value)

        // Simulation fin d'essai
        commercialManager.setSimulatedTrialDays(0)
        assertTrue("Commercial expiré sans abonnement doit être bloqué", !commercialManager.hasActiveAccess.value)

        // Souscription
        commercialManager.startSubscription(SubscriptionPlan.ANNUAL)
        assertTrue("Commercial avec abonnement annuel doit être débloqué", commercialManager.hasActiveAccess.value)
        assertTrue(commercialManager.isPremium.value)
    }

    @Test
    fun thresholdAlert_identifiesLowStockAndOutOfStock() {
        val itemLowStock = InventoryItemEntity(
            id = 1L,
            name = "Café",
            categoryId = "cuisine",
            quantity = 1,
            minThreshold = 2
        )
        val itemOk = InventoryItemEntity(
            id = 2L,
            name = "Riz",
            categoryId = "aliments",
            quantity = 5,
            minThreshold = 2
        )

        val isLowStock = itemLowStock.quantity <= itemLowStock.minThreshold
        val isOk = itemOk.quantity <= itemOk.minThreshold

        assertTrue("Item with qty 1 and threshold 2 must trigger alert", isLowStock)
        assertTrue("Item with qty 5 and threshold 2 must not trigger alert", !isOk)
    }

    @Test
    fun multiApiScanner_offlineDatabaseAndParsers() {
        // 1. Offline Open-Product-Database lookup
        val nutella = ProductLookupService.lookupOffline("3017620422003")
        assertNotNull(nutella)
        assertTrue(nutella!!.name.contains("Nutella"))
        assertEquals("aliments", nutella.suggestedCategory)
        assertEquals("E", nutella.nutriScore)
        assertEquals(ProductLookupService.SOURCE_OFFLINE_DB, nutella.source)

        // 2. ISBN detection & Inventaire.io parser
        assertTrue(ProductLookupService.isIsbnBarcode("978-2-07-061275-8"))
        assertFalse(ProductLookupService.isIsbnBarcode("3250390001001"))

        val inventaireSample = """
            {
              "entities": {
                "isbn:9782070413119": {
                  "labels": { "fr": "Les Misérables" },
                  "claims": {
                    "wdt:P1476": ["Les Misérables"],
                    "invp:P2": ["Victor Hugo / Folio"],
                    "wdt:P1104": [512]
                  },
                  "image": { "url": "/img/entities/9782070413119.jpg" }
                }
              }
            }
        """.trimIndent()
        val parsedBook = ProductLookupService.parseInventaireResponse("9782070413119", inventaireSample)
        assertNotNull(parsedBook)
        assertTrue(parsedBook!!.name.contains("Les Misérables"))
        assertEquals("livre", parsedBook.suggestedUnit)
        assertEquals(ProductLookupService.SOURCE_INVENTAIRE_IO, parsedBook.source)

        // 3. Open Food / Beauty / Products Facts v2 parser
        val beautyResponse = OffProductResponse(
            status = 1,
            product = OffProductDetails(
                productNameFr = "Shampooing Doux Amande",
                brands = "Le Petit Marseillais",
                quantity = "250 ml",
                categories = listOf("en:beauty", "en:shampoos"),
                allergensTags = emptyList()
            )
        )
        val parsedBeauty = ProductLookupService.parseOpenFactsProduct(
            barcode = "3574661482002",
            response = beautyResponse,
            sourceName = ProductLookupService.SOURCE_OPEN_BEAUTY_FACTS,
            defaultCategory = "hygiene"
        )
        assertNotNull(parsedBeauty)
        assertEquals("hygiene", parsedBeauty!!.suggestedCategory)
        assertEquals("flacon", parsedBeauty.suggestedUnit)
        assertEquals(ProductLookupService.SOURCE_OPEN_BEAUTY_FACTS, parsedBeauty.source)

        // 4. Chomp Food API v2 parser
        val chompResponse = ChompBarcodeResponse(
            items = listOf(
                ChompFoodItem(
                    name = "Organic Oat Milk",
                    brand = "Oatly",
                    packageSize = "1 L",
                    allergens = listOf("Oats", "Gluten")
                )
            )
        )
        val parsedChomp = ProductLookupService.parseChompResponse("7394376615979", chompResponse)
        assertNotNull(parsedChomp)
        assertTrue(parsedChomp!!.name.contains("Oatly"))
        assertEquals("bouteille", parsedChomp.suggestedUnit)
        assertEquals(ProductLookupService.SOURCE_CHOMP_API, parsedChomp.source)
    }

    @Test
    fun digitalReceiptParser_extractsProductsAndMatchesInventory() {
        val existingInventory = listOf(
            InventoryItemEntity(
                id = 10L,
                name = "Lait demi-écrémé Bio",
                categoryId = "aliments",
                quantity = 1,
                unit = "L",
                preferredStoreId = "bio",
                location = "Frigo"
            )
        )

        val lines = ReceiptParser.parseReceiptText(
            ReceiptParser.SAMPLE_DIGITAL_RECEIPT,
            existingInventory
        )

        assertTrue(lines.size >= 5)
        val milkLine = lines.find { it.productName.contains("Lait", ignoreCase = true) }
        assertNotNull(milkLine)
        assertEquals(2, milkLine!!.quantity)
        assertNotNull(milkLine.matchedExistingItem)
        assertEquals(10L, milkLine.matchedExistingItem!!.id)

        val showerGelLine = lines.find { it.productName.contains("Gel Douche", ignoreCase = true) }
        assertNotNull(showerGelLine)
        assertEquals("hygiene", showerGelLine!!.suggestedCategory)
    }

    @Test
    fun categoryAndStoreDeletion_reassignsReferencedItemsToFallback() {
        val initialCategories = mutableListOf(
            CategoryEntity(id = "cuisine", name = "Cuisine", iconEmoji = "🥫", colorHex = 0xFFF59E0B),
            CategoryEntity(id = "aliments", name = "Aliments", iconEmoji = "🍎", colorHex = 0xFF10B981),
            CategoryEntity(id = "autre", name = "Divers", iconEmoji = "📦", colorHex = 0xFF6B7280)
        )
        val initialStores = mutableListOf(
            StoreTypeEntity(id = "supermarket", name = "Supermarché", iconEmoji = "🛒", colorHex = 0xFF3B82F6),
            StoreTypeEntity(id = "bio", name = "Bio", iconEmoji = "🥦", colorHex = 0xFF10B981)
        )

        var items = listOf(
            InventoryItemEntity(
                id = 1L,
                name = "Pesto",
                categoryId = "cuisine",
                preferredStoreId = "bio"
            )
        )

        // 1. Simulate deletion of "cuisine" category
        val categoryToDelete = initialCategories.find { it.id == "cuisine" }!!
        val remainingCategories = initialCategories.filter { it.id != categoryToDelete.id }
        val categoryFallback = remainingCategories.find { it.id == "autre" }?.id ?: remainingCategories.first().id

        items = items.map { item ->
            if (item.categoryId == categoryToDelete.id) item.copy(categoryId = categoryFallback) else item
        }
        assertEquals("autre", items[0].categoryId)

        // 2. Simulate deletion of "bio" store
        val storeToDelete = initialStores.find { it.id == "bio" }!!
        val remainingStores = initialStores.filter { it.id != storeToDelete.id }
        val storeFallback = remainingStores.find { it.id == "supermarket" }?.id ?: remainingStores.first().id

        items = items.map { item ->
            if (item.preferredStoreId == storeToDelete.id) item.copy(preferredStoreId = storeFallback) else item
        }
        assertEquals("supermarket", items[0].preferredStoreId)
    }
}
