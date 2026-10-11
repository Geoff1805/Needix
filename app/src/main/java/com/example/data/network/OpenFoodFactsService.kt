package com.example.data.network

import android.content.Context
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OffProductResponse(
    val status: Int? = null,
    val product: OffProductDetails? = null
)

@JsonClass(generateAdapter = true)
data class OffProductDetails(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_fr") val productNameFr: String? = null,
    @Json(name = "generic_name_fr") val genericNameFr: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @Json(name = "image_front_small_url") val imageUrl: String? = null,
    @Json(name = "image_url") val fallbackImageUrl: String? = null,
    @Json(name = "categories_tags") val categories: List<String>? = null,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null,
    val allergens: String? = null,
    @Json(name = "ingredients_text_fr") val ingredientsTextFr: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null
)

@JsonClass(generateAdapter = true)
data class ChompBarcodeResponse(
    val items: List<ChompFoodItem>? = null
)

@JsonClass(generateAdapter = true)
data class ChompFoodItem(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    @Json(name = "package_size") val packageSize: String? = null,
    @Json(name = "ingredient_list") val ingredientList: String? = null,
    val allergens: List<String>? = null,
    val categories: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class OfflineProductEntry(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val category: String = "aliments",
    val nutriScore: String? = null,
    val allergens: String? = null,
    val notes: String? = null,
    val imageUrl: String? = null
)

data class ProductLookupResult(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val suggestedCategory: String = "aliments",
    val suggestedUnit: String = "pièce",
    val imageUrl: String? = null,
    val source: String = "Open Food Facts",
    val nutriScore: String? = null,
    val allergens: String? = null,
    val ingredientsSummary: String? = null,
    val suggestedNotes: String = ""
)

interface OpenFactsV2Api {
    @GET("api/v2/product/{barcode}")
    suspend fun getProductV2(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = OPEN_FACTS_FIELDS
    ): OffProductResponse

    companion object {
        const val OPEN_FACTS_FIELDS =
            "product_name,product_name_fr,generic_name_fr,brands,quantity,image_front_small_url,image_url,categories_tags,nutriscore_grade,allergens_tags,allergens,ingredients_text_fr,ingredients_text"
    }
}

interface InventaireApi {
    @GET("api/entities/by-uris")
    suspend fun getByUri(@Query("uris") uris: String): ResponseBody
}

interface ChompFoodApi {
    @GET("api/v2/food/branded/barcode.php")
    suspend fun getByBarcode(
        @Query("code") barcode: String,
        @Query("api_key") apiKey: String
    ): ChompBarcodeResponse
}

object ProductLookupService {
    const val SOURCE_OFFLINE_DB = "Open-Product-Database (Hors-ligne)"
    const val SOURCE_OPEN_FOOD_FACTS = "Open Food Facts"
    const val SOURCE_OPEN_BEAUTY_FACTS = "Open Beauty Facts"
    const val SOURCE_OPEN_PRODUCTS_FACTS = "Open Products Facts"
    const val SOURCE_INVENTAIRE_IO = "Inventaire.io (ISBN)"
    const val SOURCE_CHOMP_API = "Chomp Food API"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Needix/1.0 (https://needix.alsteens.net)")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val openFoodFactsNetApi: OpenFactsV2Api by lazy {
        buildOpenFactsClient("https://world.openfoodfacts.net/")
    }

    private val openFoodFactsOrgApi: OpenFactsV2Api by lazy {
        buildOpenFactsClient("https://world.openfoodfacts.org/")
    }

    private val openBeautyFactsApi: OpenFactsV2Api by lazy {
        buildOpenFactsClient("https://world.openbeautyfacts.org/")
    }

    private val openProductsFactsApi: OpenFactsV2Api by lazy {
        buildOpenFactsClient("https://world.openproductsfacts.org/")
    }

    private val inventaireApi: InventaireApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.inventaire.io/")
            .client(okHttpClient)
            .build()
            .create(InventaireApi::class.java)
    }

    private val chompFoodApi: ChompFoodApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://chompthis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChompFoodApi::class.java)
    }

    private fun buildOpenFactsClient(baseUrl: String): OpenFactsV2Api {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFactsV2Api::class.java)
    }

    private val sessionCache = ConcurrentHashMap<String, ProductLookupResult>()

    private val offlineCatalog = ConcurrentHashMap<String, OfflineProductEntry>().apply {
        listOf(
            OfflineProductEntry("3250390001001", "Huile d'olive vierge extra", "Puget", "1 L", "L", "cuisine", "C", null, "Première pression à froid • 1 L"),
            OfflineProductEntry("3017620422003", "Pâte à tartiner aux noisettes et cacao", "Nutella", "400 g", "pot", "aliments", "E", "Lait, Fruits à coque (noisettes), Soja", "Pot 400 g • Nutri-Score E • Allergènes : Lait, Noisettes, Soja"),
            OfflineProductEntry("8076809513753", "Pâtes Penne Rigate n°73", "Barilla", "500 g", "paquet", "aliments", "A", "Gluten (blé)", "Semoule de blé dur 500 g • Nutri-Score A"),
            OfflineProductEntry("3014260020123", "Lessive liquide Original", "Ariel", "1.8 L", "bidon", "menager", null, null, "Bidon 36 lavages (1,8 L)"),
            OfflineProductEntry("3400936081498", "Doliprane Paracétamol 1000 mg", "Sanofi", "8 comprimés", "boîte", "pharmacie", null, null, "Boîte de 8 comprimés • Antalgique et antipyrétique"),
            OfflineProductEntry("3600523724451", "Gel Douche Surgras Hydratant", "Mixa", "400 ml", "flacon", "hygiene", null, null, "Peaux sensibles • Flacon 400 ml • Sans savon"),
            OfflineProductEntry("3337875597388", "Gel Moussant Purifiant Effaclar", "La Roche-Posay", "400 ml", "flacon", "hygiene", null, null, "Cosmétique & Soin dermatologique • 400 ml"),
            OfflineProductEntry("8718951262454", "Liquide Vaisselle Ultra Dégraissant Citron", "Paic", "750 ml", "flacon", "menager", null, null, "Entretien vaisselle • Flacon 750 ml"),
            OfflineProductEntry("3165140892681", "Coffret d'embouts de vissage Impact Control", "Bosch", "36 pièces", "coffret", "bricolage", null, null, "Outillage & Bricolage • 36 pièces"),
            OfflineProductEntry("9782070612758", "Le Petit Prince", "Antoine de Saint-Exupéry (Gallimard)", "1 livre", "livre", "autre", null, null, "ISBN 978-2-07-061275-8 • Éditions Gallimard Folio"),
            OfflineProductEntry("9782070360024", "L'Étranger", "Albert Camus (Gallimard)", "1 livre", "livre", "autre", null, null, "ISBN 978-2-07-036002-4 • Collection Folio"),
            OfflineProductEntry("3228857000166", "Pain de mie Complet sans croûte", "Harrys", "500 g", "paquet", "aliments", "A", "Gluten (blé)", "100% Mie • 500 g • Nutri-Score A"),
            OfflineProductEntry("3068320114453", "Eau minérale naturelle", "Evian", "1.5 L", "bouteille", "aliments", "A", null, "Bouteille 1,5 L • Nutri-Score A"),
            OfflineProductEntry("3250390112345", "Lait demi-écrémé Bio UHT", "Bio Village", "1 L", "L", "aliments", "A", "Lait", "Bouteille 1 L • Origine France • Nutri-Score A")
        ).forEach { put(it.barcode, it) }
    }

    @Volatile
    private var assetsLoaded = false

    fun ensureOfflineDatabaseLoaded(context: Context) {
        if (assetsLoaded) return
        try {
            val json = context.assets.open("open_product_database.json").bufferedReader().use { it.readText() }
            loadOfflineEntriesFromJson(json)
            assetsLoaded = true
        } catch (_: Exception) {
        }
    }

    fun loadOfflineEntriesFromJson(json: String): Int {
        return try {
            val listType = Types.newParameterizedType(List::class.java, OfflineProductEntry::class.java)
            val adapter = moshi.adapter<List<OfflineProductEntry>>(listType)
            val entries = adapter.fromJson(json).orEmpty()
            entries.forEach { entry ->
                val clean = normalizeBarcode(entry.barcode)
                if (clean.isNotBlank()) {
                    offlineCatalog[clean] = entry.copy(barcode = clean)
                }
            }
            entries.size
        } catch (_: Exception) {
            0
        }
    }

    fun normalizeBarcode(raw: String): String {
        return raw.trim().replace("-", "").replace(" ", "")
    }

    fun isIsbnBarcode(barcode: String): Boolean {
        val clean = normalizeBarcode(barcode)
        if (clean.length == 13 && (clean.startsWith("978") || clean.startsWith("979"))) {
            return true
        }
        if (clean.length == 10 && clean.substring(0, 9).all { it.isDigit() } && (clean[9].isDigit() || clean[9].uppercaseChar() == 'X')) {
            return true
        }
        return false
    }

    fun lookupOffline(barcode: String): ProductLookupResult? {
        val clean = normalizeBarcode(barcode)
        val entry = offlineCatalog[clean] ?: return null
        val displayName = formatProductTitle(entry.name, entry.brand)
        val unit = entry.unit?.takeIf { it.isNotBlank() } ?: inferUnitFromQuantity(entry.quantity, entry.category)
        val notes = entry.notes?.takeIf { it.isNotBlank() }
            ?: buildSuggestedNotes(entry.quantity, entry.nutriScore, entry.allergens, null)

        return ProductLookupResult(
            barcode = clean,
            name = displayName,
            brand = entry.brand,
            suggestedCategory = entry.category,
            suggestedUnit = unit,
            imageUrl = entry.imageUrl,
            source = SOURCE_OFFLINE_DB,
            nutriScore = entry.nutriScore?.uppercase(),
            allergens = entry.allergens,
            suggestedNotes = notes
        )
    }

    suspend fun lookupBarcode(barcode: String, context: Context? = null): ProductLookupResult? = withContext(Dispatchers.IO) {
        if (context != null) {
            ensureOfflineDatabaseLoaded(context)
        }
        val cleanBarcode = normalizeBarcode(barcode)
        if (cleanBarcode.isBlank()) return@withContext null

        // 0. Session memory cache
        sessionCache[cleanBarcode]?.let { return@withContext it }

        // 1. Offline Open-Product-Database (0 ms network)
        lookupOffline(cleanBarcode)?.let { offlineHit ->
            sessionCache[cleanBarcode] = offlineHit
            return@withContext offlineHit
        }

        // 2. If ISBN (978/979 or ISBN-10), query Inventaire.io first
        if (isIsbnBarcode(cleanBarcode)) {
            queryInventaireIo(cleanBarcode)?.let { bookHit ->
                sessionCache[cleanBarcode] = bookHit
                return@withContext bookHit
            }
        }

        // 3. Open Food Facts API v2 (.net primary, .org fallback)
        queryOpenFactsEndpoint(openFoodFactsNetApi, cleanBarcode, SOURCE_OPEN_FOOD_FACTS, "aliments")?.let { hit ->
            sessionCache[cleanBarcode] = hit
            return@withContext hit
        }
        queryOpenFactsEndpoint(openFoodFactsOrgApi, cleanBarcode, SOURCE_OPEN_FOOD_FACTS, "aliments")?.let { hit ->
            sessionCache[cleanBarcode] = hit
            return@withContext hit
        }

        // 4. Open Beauty Facts API v2 (Cosmetics, perfumes, hygiene, skincare)
        queryOpenFactsEndpoint(openBeautyFactsApi, cleanBarcode, SOURCE_OPEN_BEAUTY_FACTS, "hygiene")?.let { hit ->
            sessionCache[cleanBarcode] = hit
            return@withContext hit
        }

        // 5. Open Products Facts API v2 (Household, cleaning, DIY, electronics, general goods)
        queryOpenFactsEndpoint(openProductsFactsApi, cleanBarcode, SOURCE_OPEN_PRODUCTS_FACTS, "menager")?.let { hit ->
            sessionCache[cleanBarcode] = hit
            return@withContext hit
        }

        // 6. Chomp Food API v2 (Last resort to preserve the 1,000 req/month free tier)
        val chompKey = BuildConfig.CHOMP_API_KEY.trim()
        if (chompKey.isNotBlank() && chompKey != "YOUR_CHOMP_API_KEY") {
            queryChompApi(cleanBarcode, chompKey)?.let { hit ->
                sessionCache[cleanBarcode] = hit
                return@withContext hit
            }
        }

        null
    }

    private suspend fun queryOpenFactsEndpoint(
        api: OpenFactsV2Api,
        barcode: String,
        sourceName: String,
        defaultCategory: String
    ): ProductLookupResult? {
        return try {
            val response = api.getProductV2(barcode)
            parseOpenFactsProduct(barcode, response, sourceName, defaultCategory)
        } catch (_: Exception) {
            null
        }
    }

    fun parseOpenFactsProduct(
        barcode: String,
        response: OffProductResponse,
        sourceName: String = SOURCE_OPEN_FOOD_FACTS,
        defaultCategory: String = "aliments"
    ): ProductLookupResult? {
        val p = response.product ?: return null
        if (response.status != null && response.status != 1) return null

        val rawName = p.productNameFr?.takeIf { it.isNotBlank() }
            ?: p.productName?.takeIf { it.isNotBlank() }
            ?: p.genericNameFr?.takeIf { it.isNotBlank() }
            ?: return null

        val brand = p.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        val fullName = formatProductTitle(rawName.trim(), brand)

        val tags = p.categories?.joinToString(" ")?.lowercase() ?: ""
        val categoryId = when {
            sourceName == SOURCE_OPEN_BEAUTY_FACTS -> {
                if (tags.contains("pharma") || tags.contains("medicin") || tags.contains("health")) "pharmacie" else "hygiene"
            }
            sourceName == SOURCE_OPEN_PRODUCTS_FACTS -> {
                when {
                    tags.contains("bricol") || tags.contains("tool") || tags.contains("hardware") || tags.contains("garden") -> "bricolage"
                    tags.contains("hygiene") || tags.contains("beauty") || tags.contains("cosmetic") -> "hygiene"
                    tags.contains("pharma") || tags.contains("health") -> "pharmacie"
                    tags.contains("book") || tags.contains("electronic") || tags.contains("computing") -> "autre"
                    else -> "menager"
                }
            }
            tags.contains("cleaning") || tags.contains("menage") || tags.contains("detergent") || tags.contains("laundry") -> "menager"
            tags.contains("hygiene") || tags.contains("cosmetic") || tags.contains("beauty") || tags.contains("shampoo") || tags.contains("soap") -> "hygiene"
            tags.contains("health") || tags.contains("pharma") || tags.contains("medicin") -> "pharmacie"
            tags.contains("condiment") || tags.contains("spice") || tags.contains("oil") || tags.contains("epice") || tags.contains("sauce") || tags.contains("coffee") || tags.contains("tea") || tags.contains("cafe") -> "cuisine"
            else -> defaultCategory
        }

        val nutriScore = p.nutriscoreGrade?.trim()?.uppercase()?.takeIf { it in setOf("A", "B", "C", "D", "E") }
        val allergens = formatAllergens(p.allergensTags, p.allergens)
        val ingredients = (p.ingredientsTextFr?.takeIf { it.isNotBlank() } ?: p.ingredientsText?.takeIf { it.isNotBlank() })
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(140)

        val unit = inferUnitFromQuantity(p.quantity, categoryId)
        val notes = buildSuggestedNotes(p.quantity, nutriScore, allergens, ingredients)

        return ProductLookupResult(
            barcode = barcode,
            name = fullName,
            brand = brand,
            suggestedCategory = categoryId,
            suggestedUnit = unit,
            imageUrl = p.imageUrl ?: p.fallbackImageUrl,
            source = sourceName,
            nutriScore = nutriScore,
            allergens = allergens,
            ingredientsSummary = ingredients,
            suggestedNotes = notes
        )
    }

    private suspend fun queryInventaireIo(barcode: String): ProductLookupResult? {
        return try {
            val rawJson = inventaireApi.getByUri("isbn:$barcode").string()
            parseInventaireResponse(barcode, rawJson)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseInventaireResponse(barcode: String, rawJson: String): ProductLookupResult? {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val root = mapAdapter.fromJson(rawJson) as? Map<String, Any?> ?: return null
            val entities = root["entities"] as? Map<String, Any?> ?: return null
            val entity = (entities["isbn:$barcode"] as? Map<String, Any?>)
                ?: (entities.values.firstOrNull() as? Map<String, Any?>)
                ?: return null

            val labels = entity["labels"] as? Map<String, Any?>
            val claims = entity["claims"] as? Map<String, Any?>

            val titleFromClaims = (claims?.get("wdt:P1476") as? List<*>)?.firstOrNull()?.toString()
            val titleFromLabels = (labels?.get("fr") ?: labels?.get("en") ?: labels?.values?.firstOrNull())?.toString()
            val title = (titleFromClaims ?: titleFromLabels)?.trim()?.takeIf { it.isNotBlank() } ?: return null

            val subtitle = (claims?.get("wdt:P1680") as? List<*>)?.firstOrNull()?.toString()?.trim()
            val publisherOrAuthor = (claims?.get("invp:P2") as? List<*>)?.firstOrNull()?.toString()?.trim()
            val pages = (claims?.get("wdt:P1104") as? List<*>)?.firstOrNull()?.toString()?.trim()

            val imageObj = entity["image"] as? Map<String, Any?>
            val rawImageUrl = imageObj?.get("url")?.toString()
            val imageUrl = when {
                rawImageUrl.isNullOrBlank() -> null
                rawImageUrl.startsWith("http") -> rawImageUrl
                else -> "https://inventaire.io$rawImageUrl"
            }

            val fullTitle = if (!subtitle.isNullOrBlank() && !title.contains(subtitle, ignoreCase = true)) {
                "$title — $subtitle"
            } else {
                title
            }

            val noteParts = mutableListOf("ISBN $barcode")
            if (!publisherOrAuthor.isNullOrBlank()) noteParts.add(publisherOrAuthor)
            if (!pages.isNullOrBlank()) noteParts.add("$pages pages")

            ProductLookupResult(
                barcode = barcode,
                name = formatProductTitle(fullTitle, publisherOrAuthor),
                brand = publisherOrAuthor,
                suggestedCategory = "autre",
                suggestedUnit = "livre",
                imageUrl = imageUrl,
                source = SOURCE_INVENTAIRE_IO,
                suggestedNotes = noteParts.joinToString(" • ")
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun queryChompApi(barcode: String, apiKey: String): ProductLookupResult? {
        return try {
            val response = chompFoodApi.getByBarcode(barcode, apiKey)
            parseChompResponse(barcode, response)
        } catch (_: Exception) {
            null
        }
    }

    fun parseChompResponse(barcode: String, response: ChompBarcodeResponse): ProductLookupResult? {
        val item = response.items?.firstOrNull() ?: return null
        val rawName = item.name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val brand = item.brand?.trim()?.takeIf { it.isNotBlank() }
        val fullName = formatProductTitle(rawName, brand)
        val allergens = item.allergens?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
        val ingredients = item.ingredientList?.trim()?.takeIf { it.isNotBlank() }?.take(140)
        val unit = inferUnitFromQuantity(item.packageSize, "aliments")
        val notes = buildSuggestedNotes(item.packageSize, null, allergens, ingredients)

        return ProductLookupResult(
            barcode = barcode,
            name = fullName,
            brand = brand,
            suggestedCategory = "aliments",
            suggestedUnit = unit,
            imageUrl = null,
            source = SOURCE_CHOMP_API,
            nutriScore = null,
            allergens = allergens,
            ingredientsSummary = ingredients,
            suggestedNotes = notes
        )
    }

    private fun formatProductTitle(name: String, brand: String?): String {
        val cleanName = name.trim()
        val cleanBrand = brand?.trim()
        return if (!cleanBrand.isNullOrBlank() && !cleanName.contains(cleanBrand, ignoreCase = true)) {
            "$cleanName ($cleanBrand)"
        } else {
            cleanName
        }
    }

    fun inferUnitFromQuantity(quantity: String?, categoryId: String): String {
        if (categoryId == "pharmacie") return "boîte"
        val q = quantity?.trim()?.lowercase() ?: return "pièce"
        return when {
            q.contains("livre") || q.contains("book") -> "livre"
            Regex("\\b\\d+([.,]\\d+)?\\s*l\\b").containsMatchIn(q) -> "bouteille"
            q.contains("ml") || q.contains("cl") -> "flacon"
            q.contains("kg") || q.contains("g") -> "paquet"
            q.contains("comprim") || q.contains("gélule") || q.contains("gelule") -> "boîte"
            else -> "pièce"
        }
    }

    private fun formatAllergens(tags: List<String>?, rawAllergens: String?): String? {
        val fromTags = tags.orEmpty()
            .map { tag ->
                tag.substringAfter(":").replace("-", " ").trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .filter { it.isNotBlank() }
            .distinct()

        if (fromTags.isNotEmpty()) {
            return fromTags.joinToString(", ")
        }

        val cleanedRaw = rawAllergens
            ?.split(",")
            ?.map { it.substringAfter(":").trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            .orEmpty()

        return cleanedRaw.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun buildSuggestedNotes(
        quantity: String?,
        nutriScore: String?,
        allergens: String?,
        ingredients: String?
    ): String {
        val parts = mutableListOf<String>()
        if (!quantity.isNullOrBlank()) parts.add(quantity.trim())
        if (!nutriScore.isNullOrBlank()) parts.add("Nutri-Score ${nutriScore.uppercase()}")
        if (!allergens.isNullOrBlank()) parts.add("Allergènes : $allergens")
        if (parts.isEmpty() && !ingredients.isNullOrBlank()) {
            parts.add("Ingrédients : $ingredients")
        }
        return parts.joinToString(" • ")
    }
}
