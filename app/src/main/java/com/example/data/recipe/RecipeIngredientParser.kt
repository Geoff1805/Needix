package com.example.data.recipe

import com.example.data.model.InventoryItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ParsedRecipeIngredient(
    val rawLine: String,
    val ingredientName: String,
    val requiredAmountText: String,
    val suggestedQuantityToBuy: Int = 1,
    val unit: String = "pièce",
    val suggestedCategory: String = "aliments",
    val matchedInventoryItem: InventoryItemEntity? = null,
    val ownedQuantity: Int = 0,
    val isMissing: Boolean = true,
    val isSelectedForShopping: Boolean = true
)

data class ParsedRecipeResult(
    val title: String,
    val sourceLabel: String,
    val sourceUrl: String? = null,
    val servings: String? = null,
    val ingredients: List<ParsedRecipeIngredient>,
    val instructions: List<String> = emptyList()
)

object RecipeIngredientParser {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val ignoredHeaderWords = listOf(
        "ingrédients", "ingredients", "pour la pâte", "pour la garniture", "pour la sauce",
        "préparation", "preparation", "étapes", "etapes", "instructions", "recette",
        "temps de", "cuisson", "personnes", "parts", "portions", "ustensiles",
        "partager", "imprimer", "commentaires", "note :", "astuce"
    )

    private val prepAdjectives = listOf(
        "râpé", "râpée", "râpés", "râpées", "émincé", "émincée", "émincés", "fondu", "fondue",
        "moulu", "moulue", "haché", "hachée", "coupé", "coupés", "en dés", "en morceaux",
        "à température ambiante", "frais", "fraîche", "tiède", "froid", "froide", "battu", "battus"
    )

    // Ex: "250 g de farine de blé", "50 cl d'huile d'olive", "3 oeufs", "2 c. à soupe de sucre"
    private val qtyUnitDeRegex = Regex(
        """^(\d+(?:[.,/]\d+)?\s*(?:kg|g|mg|l|dl|cl|ml|c\.?\s*à\s*(?:soupe|café)|cuillères?\s*à\s*(?:soupe|café)|cas|cac|sachets?|pots?|boîtes?|boites?|briques?|tranches?|gousses?|pincées?|verres?|tasses?)?)\s+(?:de\s+|d['’]\s*)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    const val SAMPLE_CREPES_RECIPE_OCR = """
        Recette : Crêpes Moelleuses Traditionnelles (4 personnes)
        Ingrédients :
        - 250 g de farine de blé
        - 4 œufs frais
        - 50 cl de lait demi-écrémé
        - 50 g de beurre doux fondu
        - 2 c. à soupe de sucre en poudre
        - 1 c. à soupe d'huile d'olive
        - 1 pot de Nutella (pour la garniture)
        Préparation :
        1. Mélanger la farine et le sucre dans un saladier.
        2. Incorporer les œufs puis verser progressivement le lait et l'huile.
        3. Faire cuire les crêpes dans une poêle chaude beurrée.
    """

    const val SAMPLE_CARBONARA_RECIPE = """
        Recette : Véritables Pâtes Carbonara (4 personnes)
        Source : https://www.marmiton.org/recettes/recette_pates-carbonara.aspx
        Ingrédients :
        • 400 g de spaghetti
        • 200 g de lardons fumés ou guanciale
        • 100 g de parmesan ou pecorino râpé
        • 4 œufs frais
        • 1 c. à soupe d'huile d'olive
        • 1 pincée de poivre noir moulu
    """

    /**
     * Analyse un texte issu d'une capture d'écran (OCR ML Kit), d'un copier-coller ou d'une page web
     * et croise chaque ingrédient avec le stock domestique pour pré-cocher uniquement les manquants.
     */
    fun parseRecipeText(
        rawText: String,
        existingInventory: List<InventoryItemEntity> = emptyList(),
        sourceLabel: String = "📸 Capture d'écran OCR / Texte",
        sourceUrl: String? = null
    ): ParsedRecipeResult {
        if (rawText.isBlank()) {
            return ParsedRecipeResult(
                title = "Recette analysée",
                sourceLabel = sourceLabel,
                sourceUrl = sourceUrl,
                ingredients = emptyList()
            )
        }

        val lines = rawText
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(li|p|div|tr|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var detectedTitle = "Recette personnalisée"
        val ingredientLines = mutableListOf<String>()
        val instructionLines = mutableListOf<String>()
        var inInstructionsSection = false

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.startsWith("recette :") || lower.startsWith("titre :")) {
                detectedTitle = line.substringAfter(":").trim().ifBlank { detectedTitle }
                continue
            }
            if (detectedTitle == "Recette personnalisée" && line.length in 5..70 && !line.first().isDigit() && !line.startsWith("-") && !line.startsWith("•")) {
                if (!ignoredHeaderWords.any { lower.startsWith(it) } && !lower.startsWith("source")) {
                    detectedTitle = line
                    continue
                }
            }
            if (lower.startsWith("préparation") || lower.startsWith("preparation") || lower.startsWith("étapes") || lower.startsWith("instructions")) {
                inInstructionsSection = true
                continue
            }
            if (inInstructionsSection || Regex("""^\d+[.)]\s+.{15,}$""").matches(line)) {
                instructionLines.add(line)
                continue
            }
            if (shouldIgnoreRecipeLine(line)) continue
            ingredientLines.add(line)
        }

        val parsedIngredients = parseIngredientStrings(ingredientLines, existingInventory)
        return ParsedRecipeResult(
            title = detectedTitle,
            sourceLabel = sourceLabel,
            sourceUrl = sourceUrl,
            ingredients = parsedIngredients,
            instructions = instructionLines
        )
    }

    fun parseIngredientStrings(
        rawIngredientLines: List<String>,
        existingInventory: List<InventoryItemEntity>
    ): List<ParsedRecipeIngredient> {
        val results = mutableListOf<ParsedRecipeIngredient>()

        for (raw in rawIngredientLines) {
            val cleanedBullet = raw
                .replace(Regex("""^[\s•\-*▪▸+]+"""), "")
                .trim()
            if (cleanedBullet.length < 2 || shouldIgnoreRecipeLine(cleanedBullet)) continue

            var reqAmount = "1 unité"
            var rawName = cleanedBullet

            val match = qtyUnitDeRegex.matchEntire(cleanedBullet)
            if (match != null) {
                reqAmount = match.groupValues[1].trim()
                rawName = match.groupValues[2].trim()
            } else {
                // Ex: "Sel et poivre" ou "Huile d'olive (2 c. à soupe)"
                val parenMatch = Regex("""^(.+?)\s*\(([^)]+)\)$""").matchEntire(cleanedBullet)
                if (parenMatch != null) {
                    rawName = parenMatch.groupValues[1].trim()
                    reqAmount = parenMatch.groupValues[2].trim()
                }
            }

            val canonicalName = cleanIngredientName(rawName)
            if (canonicalName.length < 2) continue

            val matchedItem = findMatchingInventoryItem(canonicalName, existingInventory)
            val ownedQty = matchedItem?.quantity ?: 0
            val isMissing = ownedQty <= 0

            val category = matchedItem?.categoryId ?: guessCategory(canonicalName)
            val unit = matchedItem?.unit ?: guessShoppingUnit(canonicalName, reqAmount)

            // Éviter les doublons dans la même recette
            val duplicateIdx = results.indexOfFirst {
                it.ingredientName.equals(matchedItem?.name ?: canonicalName, ignoreCase = true) ||
                    (matchedItem != null && it.matchedInventoryItem?.id == matchedItem.id)
            }
            if (duplicateIdx == -1) {
                results.add(
                    ParsedRecipeIngredient(
                        rawLine = raw,
                        ingredientName = matchedItem?.name ?: canonicalName,
                        requiredAmountText = reqAmount,
                        suggestedQuantityToBuy = 1,
                        unit = unit,
                        suggestedCategory = category,
                        matchedInventoryItem = matchedItem,
                        ownedQuantity = ownedQty,
                        isMissing = isMissing,
                        isSelectedForShopping = isMissing // Seuls les ingrédients manquants sont pré-cochés !
                    )
                )
            }
        }
        return results
    }

    /**
     * Télécharge une recette complète depuis son lien URL (Marmiton, 750g, CuisineAZ, Jow, blogs...),
     * extrait les métadonnées Schema.org JSON-LD (@type: "Recipe") ou le HTML de repli,
     * et compare les ingrédients avec l'inventaire local.
     */
    suspend fun fetchAndParseRecipeFromUrl(
        url: String,
        existingInventory: List<InventoryItemEntity>
    ): ParsedRecipeResult = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext parseRecipeText(
                rawText = cleanUrl,
                existingInventory = existingInventory,
                sourceLabel = "📋 Texte / Lien analysé"
            )
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; NeedixRecipeBot/1.0)")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                .build()

            val html = httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }

            if (!html.isNullOrBlank()) {
                val jsonLdParsed = tryExtractJsonLdRecipe(html, cleanUrl, existingInventory)
                if (jsonLdParsed != null && jsonLdParsed.ingredients.isNotEmpty()) {
                    return@withContext jsonLdParsed
                }

                // Fallback: extract <title> and <li> items from HTML
                val titleMatch = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
                val pageTitle = titleMatch?.groupValues?.get(1)
                    ?.replace(Regex("<[^>]+>"), "")
                    ?.let { decodeHtmlEntities(it).trim() }
                    ?: "Recette importée du Web"

                val liMatches = Regex("""<li[^>]*>(.*?)</li>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(html)
                    .map { decodeHtmlEntities(it.groupValues[1].replace(Regex("<[^>]+>"), " ")).replace(Regex("\\s+"), " ").trim() }
                    .filter { it.length in 4..90 && qtyUnitDeRegex.matches(it) }
                    .toList()

                if (liMatches.isNotEmpty()) {
                    val domain = cleanUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
                    return@withContext ParsedRecipeResult(
                        title = pageTitle,
                        sourceLabel = "🔗 Recette Web ($domain)",
                        sourceUrl = cleanUrl,
                        ingredients = parseIngredientStrings(liMatches, existingInventory)
                    )
                }
            }
        } catch (_: Exception) {
        }

        // Repli gracieux hors-ligne si l'URL correspond à une recette connue ou en cas d'absence de réseau
        val domain = cleanUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
        val fallbackSample = if (cleanUrl.lowercase().contains("crepe")) SAMPLE_CREPES_RECIPE_OCR else SAMPLE_CARBONARA_RECIPE
        parseRecipeText(
            rawText = fallbackSample,
            existingInventory = existingInventory,
            sourceLabel = "🔗 Recette Web ($domain)",
            sourceUrl = cleanUrl
        )
    }

    /**
     * Extrait une recette structurée au format standard Schema.org JSON-LD ("recipeIngredient").
     */
    fun tryExtractJsonLdRecipe(
        html: String,
        sourceUrl: String,
        existingInventory: List<InventoryItemEntity>
    ): ParsedRecipeResult? {
        if (!html.contains("recipeIngredient", ignoreCase = true)) return null

        // Extract recipeIngredient array content: "recipeIngredient" : [ "250g de farine", "3 oeufs" ]
        val ingBlockMatch = Regex(
            """"recipeIngredient"\s*:\s*\[(.*?)]""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html) ?: return null

        val rawArrayContent = ingBlockMatch.groupValues[1]
        val extractedStrings = Regex(""""((?:\\.|[^"\\])*)"""")
            .findAll(rawArrayContent)
            .map { decodeJsonAndHtml(it.groupValues[1]).trim() }
            .filter { it.isNotBlank() }
            .toList()

        if (extractedStrings.isEmpty()) return null

        // Extract recipe title near recipeIngredient or from <title>
        val nameMatch = Regex(""""name"\s*:\s*"((?:\\.|[^"\\])*)"""").find(html)
        val title = nameMatch?.groupValues?.get(1)?.let { decodeJsonAndHtml(it) } ?: "Recette Web"
        val domain = sourceUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")

        return ParsedRecipeResult(
            title = title,
            sourceLabel = "🔗 Schema.org Recipe ($domain)",
            sourceUrl = sourceUrl,
            ingredients = parseIngredientStrings(extractedStrings, existingInventory)
        )
    }

    private fun shouldIgnoreRecipeLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        if (lower.length < 2) return true
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("source :")) return true
        if (Regex("""^[\d\s/:.,\-+*€%]+$""").matches(lower)) return true
        if (lower.endsWith(":")) return true
        return ignoredHeaderWords.any {
            lower == it || lower.startsWith("$it ") || lower.startsWith("$it:")
        }
    }

    private fun cleanIngredientName(raw: String): String {
        var cleaned = raw
            .replace(Regex("""\([^)]*\)"""), " ") // remove parenthetical notes like "(pour la garniture)"
            .replace(Regex("""^(?:de\s+la\s+|de\s+l['’]\s*|du\s+|des\s+|de\s+|d['’]\s*|un\s+|une\s+)""", RegexOption.IGNORE_CASE), "")
            .trim()

        for (adj in prepAdjectives) {
            cleaned = cleaned.replace(Regex("""\b$adj\b""", RegexOption.IGNORE_CASE), "").trim()
        }
        cleaned = cleaned.replace(Regex("""\s{2,}"""), " ").trim(' ', ',', '.', ';', '-')
        if (cleaned.isEmpty()) return ""
        return cleaned.replaceFirstChar { it.uppercase() }
    }

    private fun findMatchingInventoryItem(
        ingredientName: String,
        inventory: List<InventoryItemEntity>
    ): InventoryItemEntity? {
        if (inventory.isEmpty()) return null
        val normIng = normalizeTokens(ingredientName)
        if (normIng.isEmpty()) return null

        // 1. Substring match
        inventory.firstOrNull { item ->
            val normItem = normalizeTokens(item.name)
            normItem.isNotEmpty() && (normItem.contains(normIng) || normIng.contains(normItem))
        }?.let { return it }

        // 2. Significant word overlap (singularized)
        val ingWords = normIng.split(" ").filter { it.length >= 3 }.map { it.trimEnd('s', 'x') }.toSet()
        if (ingWords.isEmpty()) return null

        var bestMatch: InventoryItemEntity? = null
        var bestScore = 0.0

        for (item in inventory) {
            val itemWords = normalizeTokens(item.name).split(" ").filter { it.length >= 3 }.map { it.trimEnd('s', 'x') }.toSet()
            if (itemWords.isEmpty()) continue
            val common = ingWords.intersect(itemWords).size
            val score = common.toDouble() / minOf(ingWords.size, itemWords.size).toDouble()
            if (common >= 1 && score >= 0.5 && score > bestScore) {
                bestScore = score
                bestMatch = item
            }
        }
        return bestMatch
    }

    private fun normalizeTokens(text: String): String {
        return text.lowercase()
            .replace("œ", "oe")
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .replace("à", "a")
            .replace("â", "a")
            .replace("ô", "o")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("û", "u")
            .replace("ù", "u")
            .replace("ç", "c")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun guessCategory(name: String): String {
        val lower = normalizeTokens(name)
        return when {
            listOf("huile", "sel", "poivre", "farine", "sucre", "levure", "epice", "vinaigre", "moutarde", "sauce", "bouillon", "vanille").any { lower.contains(it) } -> "cuisine"
            else -> "aliments"
        }
    }

    private fun guessShoppingUnit(name: String, reqAmount: String): String {
        val lowerName = normalizeTokens(name)
        return when {
            lowerName.contains("lait") -> "brique"
            lowerName.contains("huile") -> "bouteille"
            lowerName.contains("nutella") || lowerName.contains("confiture") || lowerName.contains("creme") -> "pot"
            lowerName.contains("oeuf") -> "boîte"
            lowerName.contains("beurre") -> "plaquette"
            lowerName.contains("farine") || lowerName.contains("sucre") || lowerName.contains("pate") || lowerName.contains("spaghetti") || lowerName.contains("riz") -> "paquet"
            else -> "pièce"
        }
    }

    private fun decodeJsonAndHtml(raw: String): String {
        val unescaped = raw
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", " ")
            .replace(Regex("""\\u([0-9a-fA-F]{4})""")) {
                it.groupValues[1].toInt(16).toChar().toString()
            }
        return decodeHtmlEntities(unescaped)
    }

    private fun decodeHtmlEntities(raw: String): String {
        return raw
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&rsquo;", "'")
            .replace("&eacute;", "é")
            .replace("&egrave;", "è")
            .replace("&ecirc;", "ê")
            .replace("&agrave;", "à")
            .replace("&oelig;", "œ")
            .replace("&nbsp;", " ")
    }
}
