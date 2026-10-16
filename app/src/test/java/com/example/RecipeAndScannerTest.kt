package com.example

import com.example.data.model.InventoryItemEntity
import com.example.data.recipe.RecipeIngredientParser
import com.example.ui.viewmodel.InStoreCartItem
import com.example.ui.viewmodel.InStoreScanBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAndScannerTest {

    private val sampleInventory = listOf(
        InventoryItemEntity(
            id = 1L,
            name = "Farine de blé T55",
            categoryId = "cuisine",
            quantity = 2,
            minThreshold = 1,
            unit = "paquet",
            preferredStoreId = "supermarket",
            location = "Placard"
        ),
        InventoryItemEntity(
            id = 2L,
            name = "Lait demi-écrémé",
            categoryId = "aliments",
            quantity = 0, // Rupture !
            minThreshold = 2,
            unit = "brique",
            preferredStoreId = "supermarket",
            location = "Frigo"
        ),
        InventoryItemEntity(
            id = 3L,
            name = "Beurre doux",
            categoryId = "aliments",
            quantity = 1,
            minThreshold = 1,
            unit = "plaquette",
            preferredStoreId = "supermarket",
            location = "Frigo"
        )
    )

    @Test
    fun `parseRecipeText identifies owned vs missing ingredients and preselects only missing`() {
        val rawOcrText = """
            Recette : Pâte à Crêpes
            Ingrédients (pour 4 personnes) :
            - 250 g de farine de blé
            - 4 oeufs frais bio
            - 500 ml de lait demi-écrémé
            - 50 g de beurre doux
            - 2 c. à soupe de sucre vanillé
            Préparation :
            1. Mélanger la farine et les oeufs dans un saladier.
        """.trimIndent()

        val result = RecipeIngredientParser.parseRecipeText(
            rawText = rawOcrText,
            existingInventory = sampleInventory
        )

        assertEquals("Pâte à Crêpes", result.title)
        assertEquals(5, result.ingredients.size)

        // Farine -> en stock (qty = 2) -> non manquant, non sélectionné
        val farine = result.ingredients.first { it.ingredientName.lowercase().contains("farine") }
        assertFalse(farine.isMissing)
        assertFalse(farine.isSelectedForShopping)
        assertEquals(2, farine.ownedQuantity)

        // Oeufs -> absent de l'inventaire -> manquant, pré-sélectionné
        val oeufs = result.ingredients.first { it.ingredientName.lowercase().contains("oeuf") }
        assertTrue(oeufs.isMissing)
        assertTrue(oeufs.isSelectedForShopping)
        assertEquals(0, oeufs.ownedQuantity)

        // Lait -> présent dans l'inventaire mais quantity = 0 -> manquant, pré-sélectionné !
        val lait = result.ingredients.first { it.ingredientName.lowercase().contains("lait") }
        assertTrue(lait.isMissing)
        assertTrue(lait.isSelectedForShopping)
        assertEquals(0, lait.ownedQuantity)

        // Beurre -> en stock (qty = 1) -> non manquant, non sélectionné
        val beurre = result.ingredients.first { it.ingredientName.lowercase().contains("beurre") }
        assertFalse(beurre.isMissing)
        assertFalse(beurre.isSelectedForShopping)

        // Sucre vanillé -> absent -> manquant, pré-sélectionné
        val sucre = result.ingredients.first { it.ingredientName.lowercase().contains("sucre") }
        assertTrue(sucre.isMissing)
        assertTrue(sucre.isSelectedForShopping)

        assertEquals(3, result.ingredients.count { it.isMissing })
        assertEquals(2, result.ingredients.count { !it.isMissing })
    }

    @Test
    fun `parseRecipeText handles empty or noisy OCR input gracefully`() {
        val emptyResult = RecipeIngredientParser.parseRecipeText(
            rawText = "   \n  \t  ",
            existingInventory = sampleInventory
        )
        assertTrue(emptyResult.ingredients.isEmpty())
    }

    @Test
    fun `tryExtractJsonLdRecipe extracts Schema org Recipe JSON-LD ingredients and title`() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "Recipe",
                  "name": "Quiche Lorraine Traditionnelle",
                  "recipeIngredient": [
                    "1 pâte brisée",
                    "200 g de lardons fumés",
                    "250 g de farine de blé",
                    "50 g de beurre doux"
                  ]
                }
                </script>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val parsed = RecipeIngredientParser.tryExtractJsonLdRecipe(
            html = sampleHtml,
            sourceUrl = "https://www.marmiton.org/recettes/quiche-lorraine",
            existingInventory = sampleInventory
        )
        assertNotNull(parsed)
        assertEquals("Quiche Lorraine Traditionnelle", parsed!!.title)
        assertEquals(4, parsed.ingredients.size)
        // Pâte brisée & lardons -> missing (2) ; farine & beurre -> owned (2)
        assertEquals(2, parsed.ingredients.count { it.isMissing })
        assertEquals(2, parsed.ingredients.count { !it.isMissing })
    }

    @Test
    fun `tryExtractJsonLdRecipe returns null when HTML has no Recipe JSON-LD`() {
        val plainHtml = "<html><body><h1>Pas de JSON-LD ici</h1></body></html>"
        val parsed = RecipeIngredientParser.tryExtractJsonLdRecipe(
            html = plainHtml,
            sourceUrl = "https://example.com",
            existingInventory = sampleInventory
        )
        assertNull(parsed)
    }

    @Test
    fun `InStoreScanBehavior exposes all 5 expected modes including 4 options and All-in-One`() {
        val modes = InStoreScanBehavior.entries
        assertEquals(5, modes.size)
        assertTrue(modes.contains(InStoreScanBehavior.ALL_IN_ONE))
        assertTrue(modes.contains(InStoreScanBehavior.CHECK_OWNED))
        assertTrue(modes.contains(InStoreScanBehavior.INSTANT_RESTOCK))
        assertTrue(modes.contains(InStoreScanBehavior.CART_BATCH))
        assertTrue(modes.contains(InStoreScanBehavior.NUTRI_DETAIL))
    }

    @Test
    fun `InStoreCartItem holds expected barcode, quantity and metadata`() {
        val cartItem = InStoreCartItem(
            barcode = "3017620422003",
            name = "Nutella 400g",
            categoryId = "cuisine",
            unit = "pot",
            location = "Placard",
            quantity = 2,
            existingInventoryId = 10L
        )
        assertEquals("3017620422003", cartItem.barcode)
        assertEquals(2, cartItem.quantity)
        assertEquals(10L, cartItem.existingInventoryId)
    }
}
