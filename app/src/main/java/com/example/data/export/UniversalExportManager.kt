package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class FullBackupData(
    val exportDate: String,
    val appVersion: String,
    val items: List<InventoryItemEntity>,
    val shoppingItems: List<ShoppingItemEntity>,
    val categories: List<CategoryEntity>,
    val storeTypes: List<StoreTypeEntity>
)

internal data class FlexibleItemJson(
    val id: Long = 0L,
    val name: String = "",
    val categoryId: String? = null,
    val category: String? = null,
    val quantity: Double = 1.0,
    val minThreshold: Double? = null,
    val minQuantity: Double? = null,
    val unit: String = "pièce",
    val preferredStoreId: String? = null,
    val store: String? = null,
    val barcode: String? = null,
    val location: String = "Placard",
    val notes: String = "",
    val updatedAt: Long? = null
)

internal data class FlexibleShoppingItemJson(
    val id: Long = 0L,
    val inventoryItemId: Long? = null,
    val name: String = "",
    val categoryId: String? = null,
    val category: String? = null,
    val storeId: String? = null,
    val store: String? = null,
    val quantityNeeded: Double? = null,
    val quantity: Double? = null,
    val unit: String = "pièce",
    val isPurchased: Boolean? = null,
    val checked: Boolean? = null,
    val isOnHold: Boolean = false,
    val barcode: String? = null,
    val addedAt: Long? = null
)

internal data class FlexibleBackupJson(
    val exportDate: String = "",
    val appVersion: String? = null,
    val version: String? = null,
    val items: List<FlexibleItemJson> = emptyList(),
    val shoppingItems: List<FlexibleShoppingItemJson>? = null,
    val shoppingList: List<FlexibleShoppingItemJson>? = null,
    val categories: List<CategoryEntity>? = null,
    val storeTypes: List<StoreTypeEntity>? = null
)

object UniversalExportManager {
    private const val CSV_HEADER = "ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(FullBackupData::class.java).indent("  ")
    private val flexibleAdapter = moshi.adapter(FlexibleBackupJson::class.java)

    /**
     * Génère un fichier CSV standardisé compatible avec tous les tableurs (Excel, LibreOffice, OnlyOffice) et la Web App.
     */
    fun generateStandardCsv(items: List<InventoryItemEntity>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        for (item in items) {
            sb.append(escapeCsv(item.id.toString())).append(",")
                .append(escapeCsv(item.name)).append(",")
                .append(escapeCsv(item.categoryId)).append(",")
                .append(item.quantity).append(",")
                .append(item.minThreshold).append(",")
                .append(escapeCsv(item.unit)).append(",")
                .append(escapeCsv(item.preferredStoreId)).append(",")
                .append(escapeCsv(item.location)).append(",")
                .append(escapeCsv(item.barcode ?: "")).append(",")
                .append(escapeCsv(item.notes))
                .append("\n")
        }
        return sb.toString()
    }

    /**
     * Génère une sauvegarde intégrale JSON sans aucune perte de données.
     */
    fun generateJsonBackup(
        items: List<InventoryItemEntity>,
        shoppingItems: List<ShoppingItemEntity>,
        categories: List<CategoryEntity>,
        storeTypes: List<StoreTypeEntity>
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val backup = FullBackupData(
            exportDate = timestamp,
            appVersion = "1.0.0",
            items = items,
            shoppingItems = shoppingItems,
            categories = categories,
            storeTypes = storeTypes
        )
        return backupAdapter.toJson(backup)
    }

    /**
     * Parse un contenu JSON de sauvegarde complète (compatible Android et Web App).
     */
    fun parseJsonBackup(jsonString: String): FullBackupData? {
        val cleaned = jsonString.removePrefix("\uFEFF").trim()
        return try {
            val flex = flexibleAdapter.fromJson(cleaned) ?: return null
            val now = System.currentTimeMillis()
            val mappedItems = flex.items
                .filter { it.name.isNotBlank() }
                .map { raw ->
                    val rawCat = raw.categoryId?.takeIf { it.isNotBlank() } ?: raw.category ?: "autre"
                    val rawStore = raw.preferredStoreId?.takeIf { it.isNotBlank() } ?: raw.store ?: "supermarket"
                    val min = (raw.minThreshold ?: raw.minQuantity ?: 1.0).roundToInt().coerceAtLeast(0)
                    InventoryItemEntity(
                        id = raw.id,
                        name = raw.name.trim(),
                        categoryId = normalizeCategoryId(rawCat),
                        quantity = raw.quantity.roundToInt().coerceAtLeast(0),
                        minThreshold = min,
                        unit = raw.unit.ifBlank { "pièce" },
                        preferredStoreId = normalizeStoreId(rawStore),
                        barcode = raw.barcode?.trim()?.ifBlank { null },
                        location = raw.location.ifBlank { "Placard" },
                        notes = raw.notes,
                        updatedAt = raw.updatedAt ?: now
                    )
                }

            val rawShopping = flex.shoppingItems ?: flex.shoppingList ?: emptyList()
            val mappedShopping = rawShopping
                .filter { it.name.isNotBlank() }
                .map { sh ->
                    val rawCat = sh.categoryId?.takeIf { it.isNotBlank() } ?: sh.category ?: "autre"
                    val rawStore = sh.storeId?.takeIf { it.isNotBlank() } ?: sh.store ?: "supermarket"
                    ShoppingItemEntity(
                        id = sh.id,
                        inventoryItemId = sh.inventoryItemId,
                        name = sh.name.trim(),
                        categoryId = normalizeCategoryId(rawCat),
                        storeId = normalizeStoreId(rawStore),
                        quantityNeeded = (sh.quantityNeeded ?: sh.quantity ?: 1.0).roundToInt().coerceAtLeast(1),
                        unit = sh.unit.ifBlank { "pièce" },
                        isPurchased = sh.isPurchased ?: sh.checked ?: false,
                        isOnHold = sh.isOnHold,
                        barcode = sh.barcode?.trim()?.ifBlank { null },
                        addedAt = sh.addedAt ?: now
                    )
                }

            FullBackupData(
                exportDate = flex.exportDate,
                appVersion = flex.appVersion ?: flex.version ?: "1.0.0",
                items = mappedItems,
                shoppingItems = mappedShopping,
                categories = flex.categories ?: emptyList(),
                storeTypes = flex.storeTypes ?: emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Analyse un flux CSV (Android 10 colonnes ou Web 11 colonnes, séparateur `,`, `;` ou `\t`) pour réimporter des articles.
     */
    fun parseCsv(csvContent: String): List<InventoryItemEntity> {
        val cleaned = csvContent.removePrefix("\uFEFF")
        val lines = cleaned.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val headerLine = lines[0]
        val delimiter = when {
            headerLine.contains(";") -> ';'
            headerLine.contains("\t") -> '\t'
            else -> ','
        }
        val headerTokens = parseCsvLine(headerLine, delimiter).map { it.trim().lowercase() }
        val isLegacyWebFormat = headerTokens.getOrNull(3)?.contains("emplacement") == true

        val results = mutableListOf<InventoryItemEntity>()
        for (i in 1 until lines.size) {
            val tokens = parseCsvLine(lines[i], delimiter)
            if (tokens.size >= 2) {
                val id = tokens.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val name = tokens.getOrNull(1)?.trim() ?: continue
                if (name.isBlank()) continue

                if (isLegacyWebFormat) {
                    // Legacy Web 11 columns: ID;Nom;Catégorie;Emplacement;Quantité;Unité;Seuil_Alerte;Date_Péremption;Magasin;Code_Barres;Notes
                    val categoryId = normalizeCategoryId(tokens.getOrNull(2)?.trim() ?: "autre")
                    val location = tokens.getOrNull(3)?.trim()?.ifBlank { "Placard" } ?: "Placard"
                    val quantity = tokens.getOrNull(4)?.trim()?.toDoubleOrNull()?.roundToInt() ?: 1
                    val unit = tokens.getOrNull(5)?.trim()?.ifBlank { "pièce" } ?: "pièce"
                    val minThreshold = tokens.getOrNull(6)?.trim()?.toDoubleOrNull()?.roundToInt() ?: 1
                    val preferredStoreId = normalizeStoreId(tokens.getOrNull(8)?.trim() ?: "supermarket")
                    val barcode = tokens.getOrNull(9)?.trim()?.ifBlank { null }
                    val notes = tokens.getOrNull(10)?.trim() ?: ""

                    results.add(
                        InventoryItemEntity(
                            id = id,
                            name = name,
                            categoryId = categoryId,
                            quantity = quantity.coerceAtLeast(0),
                            minThreshold = minThreshold.coerceAtLeast(0),
                            unit = unit,
                            preferredStoreId = preferredStoreId,
                            location = location,
                            barcode = barcode,
                            notes = notes,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Standard 10 columns: ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes
                    val categoryId = normalizeCategoryId(tokens.getOrNull(2)?.trim() ?: "autre")
                    val quantity = tokens.getOrNull(3)?.trim()?.toDoubleOrNull()?.roundToInt() ?: 1
                    val minThreshold = tokens.getOrNull(4)?.trim()?.toDoubleOrNull()?.roundToInt() ?: 1
                    val unit = tokens.getOrNull(5)?.trim()?.ifBlank { "pièce" } ?: "pièce"
                    val preferredStoreId = normalizeStoreId(tokens.getOrNull(6)?.trim() ?: "supermarket")
                    val location = tokens.getOrNull(7)?.trim()?.ifBlank { "Placard" } ?: "Placard"
                    val barcode = tokens.getOrNull(8)?.trim()?.ifBlank { null }
                    val notes = tokens.getOrNull(9)?.trim() ?: ""

                    results.add(
                        InventoryItemEntity(
                            id = id,
                            name = name,
                            categoryId = categoryId,
                            quantity = quantity.coerceAtLeast(0),
                            minThreshold = minThreshold.coerceAtLeast(0),
                            unit = unit,
                            preferredStoreId = preferredStoreId,
                            location = location,
                            barcode = barcode,
                            notes = notes,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return results
    }

    internal fun normalizeCategoryId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "autre"
        val lower = trimmed.lowercase()
        return when {
            lower in setOf("cuisine", "aliments", "menager", "hygiene", "pharmacie", "bricolage", "autre") -> lower
            lower.contains("cuisine") || lower.contains("condiment") || lower.contains("boisson") -> "cuisine"
            lower.contains("aliment") || lower.contains("lait") || lower.contains("fruit") || lower.contains("viande") || lower.contains("épicerie") || lower.contains("epicerie") -> "aliments"
            lower.contains("ménag") || lower.contains("menag") || lower.contains("entretien") -> "menager"
            lower.contains("hygi") -> "hygiene"
            lower.contains("pharma") || lower.contains("sant") -> "pharmacie"
            lower.contains("brico") -> "bricolage"
            else -> trimmed
        }
    }

    internal fun normalizeStoreId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "supermarket"
        val lower = trimmed.lowercase()
        return when {
            lower in setOf("supermarket", "pharmacy", "online", "bio", "brico") -> lower
            lower.contains("supermarch") -> "supermarket"
            lower.contains("pharma") -> "pharmacy"
            lower.contains("ligne") || lower.contains("internet") -> "online"
            lower.contains("bio") || lower.contains("marché") || lower.contains("marche") -> "bio"
            lower.contains("brico") -> "brico"
            else -> trimmed
        }
    }

    suspend fun createTempShareFile(context: Context, filename: String, content: String): Uri =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, filename)
            file.writeText(content, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c == delimiter && !insideQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}
