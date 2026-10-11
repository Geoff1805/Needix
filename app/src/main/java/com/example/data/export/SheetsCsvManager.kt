package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(
    val importedCount: Int,
    val updatedCount: Int,
    val errors: List<String>
)

object SheetsCsvManager {
    private const val CSV_HEADER = "ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes"

    /**
     * Generates a CSV formatted string ready for Google Sheets & Excel
     */
    fun generateCsv(items: List<InventoryItemEntity>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        for (item in items) {
            val escapedName = escapeCsv(item.name)
            val escapedCategory = escapeCsv(item.categoryId)
            val escapedUnit = escapeCsv(item.unit)
            val escapedStore = escapeCsv(item.preferredStoreId)
            val escapedLocation = escapeCsv(item.location)
            val escapedBarcode = escapeCsv(item.barcode ?: "")
            val escapedNotes = escapeCsv(item.notes)

            sb.append("${item.id},")
                .append("$escapedName,")
                .append("$escapedCategory,")
                .append("${item.quantity},")
                .append("${item.minThreshold},")
                .append("$escapedUnit,")
                .append("$escapedStore,")
                .append("$escapedLocation,")
                .append("$escapedBarcode,")
                .append(escapedNotes)
                .append("\n")
        }
        return sb.toString()
    }

    /**
     * Generates a Tab-Separated Values string for direct copy-paste into Google Sheets
     */
    fun generateTsv(items: List<InventoryItemEntity>): String {
        val sb = StringBuilder()
        sb.append("ID\tNom\tCatégorie\tQuantité\tSeuil\tUnité\tMagasin\tEmplacement\tCodeBarre\tNotes\n")
        for (item in items) {
            sb.append("${item.id}\t${item.name}\t${item.categoryId}\t${item.quantity}\t${item.minThreshold}\t${item.unit}\t${item.preferredStoreId}\t${item.location}\t${item.barcode ?: ""}\t${item.notes}\n")
        }
        return sb.toString()
    }

    /**
     * Saves CSV file to app cache directory and returns a sharable FileProvider Uri
     */
    suspend fun createSharableCsvFile(context: Context, items: List<InventoryItemEntity>): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "inventaire_google_sheets_$timestamp.csv")
        file.writeText(generateCsv(items), Charsets.UTF_8)

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Writes CSV content directly to an OutputStream (e.g. for SAF CreateDocument)
     */
    suspend fun writeCsvToUri(context: Context, uri: Uri, items: List<InventoryItemEntity>) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write(generateCsv(items))
                writer.flush()
            }
        }
    }

    /**
     * Parses a CSV file from a Uri (e.g. Google Drive or device file picker)
     */
    suspend fun parseCsvFromUri(context: Context, uri: Uri): List<InventoryItemEntity> = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
        } ?: return@withContext emptyList()
        return@withContext UniversalExportManager.parseCsv(content)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains(";") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        val delChar = delimiter[0]

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delChar && !inQuotes) {
                result.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
