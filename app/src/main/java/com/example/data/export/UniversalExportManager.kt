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

data class FullBackupData(
    val exportDate: String,
    val appVersion: String,
    val items: List<InventoryItemEntity>,
    val shoppingItems: List<ShoppingItemEntity>,
    val categories: List<CategoryEntity>,
    val storeTypes: List<StoreTypeEntity>
)

object UniversalExportManager {
    private const val CSV_HEADER = "ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(FullBackupData::class.java).indent("  ")

    /**
     * Génère un fichier CSV standardisé compatible avec tous les tableurs (Excel, LibreOffice, OnlyOffice).
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
     * Parse un contenu JSON de sauvegarde complète.
     */
    fun parseJsonBackup(jsonString: String): FullBackupData? {
        return try {
            backupAdapter.fromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Analyse un flux CSV pour réimporter des articles dans l'inventaire.
     */
    fun parseCsv(csvContent: String): List<InventoryItemEntity> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val results = mutableListOf<InventoryItemEntity>()
        // Skip header
        for (i in 1 until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.size >= 2) {
                val id = tokens.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val name = tokens.getOrNull(1)?.trim() ?: continue
                if (name.isBlank()) continue

                val categoryId = tokens.getOrNull(2)?.trim()?.ifBlank { "autre" } ?: "autre"
                val quantity = tokens.getOrNull(3)?.trim()?.toIntOrNull() ?: 1
                val minThreshold = tokens.getOrNull(4)?.trim()?.toIntOrNull() ?: 1
                val unit = tokens.getOrNull(5)?.trim()?.ifBlank { "pièce" } ?: "pièce"
                val preferredStoreId = tokens.getOrNull(6)?.trim()?.ifBlank { "supermarket" } ?: "supermarket"
                val location = tokens.getOrNull(7)?.trim()?.ifBlank { "Placard" } ?: "Placard"
                val barcode = tokens.getOrNull(8)?.trim()?.ifBlank { null }
                val notes = tokens.getOrNull(9)?.trim() ?: ""

                results.add(
                    InventoryItemEntity(
                        id = id,
                        name = name,
                        categoryId = categoryId,
                        quantity = quantity,
                        minThreshold = minThreshold,
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
        return results
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

    private fun parseCsvLine(line: String): List<String> {
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
            } else if (c == ',' && !insideQuotes) {
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
