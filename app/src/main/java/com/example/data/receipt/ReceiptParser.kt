package com.example.data.receipt

import com.example.data.model.InventoryItemEntity

data class ParsedReceiptLine(
    val rawLine: String,
    val productName: String,
    val quantity: Int,
    val unit: String = "pièce",
    val suggestedCategory: String = "aliments",
    val matchedExistingItem: InventoryItemEntity? = null,
    val isSelected: Boolean = true
)

object ReceiptParser {

    private val ignoredKeywords = listOf(
        "total", "sous-total", "subtotal", "tva", "dont tva", "montant", "carte bancaire",
        "cb ", "especes", "espèces", "rendu", "monnaie", "ticket", "caisse", "caissier",
        "merci de votre visite", "a bientot", "à bientôt", "siret", "siren", " RCS ",
        "tel :", "tél :", "horaires", "facture", "paiement", "autorisation", " sans contact",
        "fidelite", "fidélité", "avantage", "remise", "reduction", "réduction", "bon d'achat",
        "solde", "points", "ouverture", "bienvenue", "http://", "https://", "www."
    )

    // Matches leading quantity like "2 x ", "3 X ", "2x ", "QTE 2 "
    private val leadingQtyRegex = Regex("""^\s*(\d{1,2})\s*[xX*]\s+(.+)$""")
    // Matches trailing multiplier like "LAIT BIO 1L   2 x 1,25" or "PAIN DE MIE 3 X 1.50 €"
    private val inlineQtyPriceRegex = Regex("""^(.+?)\s+(\d{1,2})\s*[xX*]\s*\d+[.,]\d{2}\s*[€EUR]*.*$""")
    // Matches trailing price at end of line like "HUILE D'OLIVE 1L   6,49 € A" or "PATES BARILLA 500G 1.29"
    private val trailingPriceRegex = Regex("""^(.+?)\s+-?\d{1,4}[.,]\d{2}\s*(?:€|EUR)?\s*[A-Z*]?\s*$""", RegexOption.IGNORE_CASE)
    // Pure quantity/price multiplier line following a product line: "   2 x 1,45 €   2,90 €"
    private val standaloneMultiplierRegex = Regex("""^\s*(\d{1,2})\s*[xX*]\s*\d+[.,]\d{2}.*$""")
    // Date/time or dashed separator lines
    private val separatorOrDateRegex = Regex("""^[\s\-=_*#/0-9:.,€]+$""")

    /**
     * Analyse le texte brut d'un ticket de caisse numérique (e-mail, texte OCR, HTML converti)
     * et retourne la liste des produits détectés avec rapprochement sur l'inventaire existant.
     */
    fun parseReceiptText(
        rawText: String,
        existingInventory: List<InventoryItemEntity> = emptyList()
    ): List<ParsedReceiptLine> {
        if (rawText.isBlank()) return emptyList()

        // Strip HTML tags if user pasted/imported an HTML email receipt
        val cleanText = rawText
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|div|tr|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")

        val rawLines = cleanText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val aggregated = mutableListOf<ParsedReceiptLine>()

        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i]

            if (shouldIgnoreLine(line)) {
                i++
                continue
            }

            var qty = 1
            var candidateName = line

            // Case 1: Inline quantity + price: "YAOURT NATURE   2 x 1,50   3,00 €"
            val inlineMatch = inlineQtyPriceRegex.matchEntire(candidateName)
            if (inlineMatch != null) {
                candidateName = inlineMatch.groupValues[1].trim()
                qty = inlineMatch.groupValues[2].toIntOrNull()?.coerceAtLeast(1) ?: 1
            } else {
                // Strip trailing price if present
                val priceMatch = trailingPriceRegex.matchEntire(candidateName)
                if (priceMatch != null) {
                    candidateName = priceMatch.groupValues[1].trim()
                }
                // Check leading quantity: "2 x LAIT DEMI-ECREME"
                val leadMatch = leadingQtyRegex.matchEntire(candidateName)
                if (leadMatch != null) {
                    qty = leadMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                    candidateName = leadMatch.groupValues[2].trim()
                }
            }

            // Look ahead: is the next line a standalone multiplier like "2 x 1,45 €"?
            if (i + 1 < rawLines.size) {
                val nextLine = rawLines[i + 1]
                val multMatch = standaloneMultiplierRegex.matchEntire(nextLine)
                if (multMatch != null) {
                    qty = multMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: qty
                    i++ // consume multiplier line
                }
            }

            val cleanedName = normalizeReceiptProductName(candidateName)
            if (cleanedName.length >= 3 && !shouldIgnoreLine(cleanedName)) {
                val matched = findBestInventoryMatch(cleanedName, existingInventory)
                val category = matched?.categoryId ?: guessCategoryFromName(cleanedName)
                val unit = matched?.unit ?: guessUnitFromName(cleanedName)

                // Merge duplicates on the same receipt
                val existingIdx = aggregated.indexOfFirst {
                    it.productName.equals(cleanedName, ignoreCase = true) ||
                        (matched != null && it.matchedExistingItem?.id == matched.id)
                }
                if (existingIdx >= 0) {
                    val prev = aggregated[existingIdx]
                    aggregated[existingIdx] = prev.copy(quantity = prev.quantity + qty)
                } else {
                    aggregated.add(
                        ParsedReceiptLine(
                            rawLine = line,
                            productName = matched?.name ?: cleanedName,
                            quantity = qty,
                            unit = unit,
                            suggestedCategory = category,
                            matchedExistingItem = matched,
                            isSelected = true
                        )
                    )
                }
            }
            i++
        }

        return aggregated
    }

    private fun shouldIgnoreLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        if (lower.length < 3) return true
        if (separatorOrDateRegex.matches(lower)) return true
        if (standaloneMultiplierRegex.matches(lower)) return true
        // Ignore dates like 11/10/2026 or postal codes like 75001 PARIS
        if (Regex("""^\d{2}/\d{2}/\d{2,4}.*$""").matches(lower)) return true
        if (Regex("""^\d{4,5}\s+[a-z\s-]+$""").matches(lower)) return true
        return ignoredKeywords.any { lower.contains(it) }
    }

    private fun normalizeReceiptProductName(raw: String): String {
        // Remove leading article codes/barcodes if present
        val withoutCode = raw.replace(Regex("""^\d{6,14}\s+"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
        if (withoutCode.isEmpty()) return ""
        // Format ALL CAPS receipt names into readable Title Case
        return if (withoutCode == withoutCode.uppercase()) {
            withoutCode.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            withoutCode
        }
    }

    private fun findBestInventoryMatch(
        receiptName: String,
        inventory: List<InventoryItemEntity>
    ): InventoryItemEntity? {
        if (inventory.isEmpty()) return null
        val normalizedReceipt = simplifyTokens(receiptName)
        if (normalizedReceipt.isEmpty()) return null

        // 1. Direct substring match
        inventory.firstOrNull { item ->
            val itemNorm = simplifyTokens(item.name)
            itemNorm.isNotEmpty() && (normalizedReceipt.contains(itemNorm) || itemNorm.contains(normalizedReceipt))
        }?.let { return it }

        // 2. Token overlap score
        val receiptWords = normalizedReceipt.split(" ").filter { it.length >= 3 }.toSet()
        if (receiptWords.isEmpty()) return null

        var bestItem: InventoryItemEntity? = null
        var bestScore = 0.0

        for (item in inventory) {
            val itemWords = simplifyTokens(item.name).split(" ").filter { it.length >= 3 }.toSet()
            if (itemWords.isEmpty()) continue
            val common = receiptWords.intersect(itemWords).size
            val score = common.toDouble() / minOf(receiptWords.size, itemWords.size).toDouble()
            if (common >= 1 && score >= 0.6 && score > bestScore) {
                bestScore = score
                bestItem = item
            }
        }
        return bestItem
    }

    private fun simplifyTokens(text: String): String {
        return text.lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace("â", "a")
            .replace("ô", "o")
            .replace("î", "i")
            .replace("û", "u")
            .replace("ç", "c")
            .replace("œ", "oe")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun guessCategoryFromName(name: String): String {
        val lower = simplifyTokens(name)
        return when {
            listOf("lessive", "vaisselle", "javel", "nettoyant", "eponge", "essuie", "sac poubelle", "adoucissant").any { lower.contains(it) } -> "menager"
            listOf("savon", "shampoing", "shampooing", "douche", "dentifrice", "deodorant", "papier toilette", "coton", "creme").any { lower.contains(it) } -> "hygiene"
            listOf("doliprane", "paracetamol", "pansement", "sirop", "vitamine", "serum").any { lower.contains(it) } -> "pharmacie"
            listOf("huile", "sel", "poivre", "epice", "farine", "sucre", "vinaigre", "moutarde", "sauce", "cafe", "the").any { lower.contains(it) } -> "cuisine"
            listOf("ampoule", "pile", "vis", "colle", "ruban", "outil").any { lower.contains(it) } -> "bricolage"
            else -> "aliments"
        }
    }

    private fun guessUnitFromName(name: String): String {
        val match = Regex("""\b(\d+(?:[.,]\d+)?\s*(?:kg|g|l|ml|cl))\b""", RegexOption.IGNORE_CASE).find(name)
        return match?.value ?: "pièce"
    }

    const val SAMPLE_DIGITAL_RECEIPT = """
        SUPERMARCHÉ BIO & FRAIS
        Ticket de caisse numérique - 11/10/2026 18:42
        -----------------------------------------
        2 x Lait demi-écrémé Bio 1L        2,70 €
        Pâtes Penne Rigate 500g            1,45 €
        Huile d'olive vierge extra 1L      8,90 €
        Gel Douche Surgras Mixa 400ml      3,50 €
        Lessive liquide Ariel 1.8L         9,90 €
        Pommes Golden Bio 1kg              2,99 €
        -----------------------------------------
        TOTAL TTC                         29,44 €
        CARTE BANCAIRE SANS CONTACT       29,44 €
        TVA 5,5% : 1,12 € | TVA 20% : 2,23 €
        Merci de votre visite !
    """
}
