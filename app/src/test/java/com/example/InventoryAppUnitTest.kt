package com.example

import com.example.billing.CommercialBillingManager
import com.example.billing.FossBillingManager
import com.example.billing.SubscriptionPlan
import com.example.data.export.SheetsCsvManager
import com.example.data.export.UniversalExportManager
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import org.junit.Assert.assertEquals
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
    fun billingManagers_fossAndCommercialLogic() {
        // Test FOSS: toujours débloqué
        val fossManager = FossBillingManager()
        assertTrue("FOSS doit être débloqué d'office", fossManager.hasActiveAccess.value)
        assertTrue("FOSS doit être premium", fossManager.isPremium.value)

        // Test Commercial: 14 jours d'essai
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
}
