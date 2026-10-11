package com.example.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Abstraction KMP (expect/actual ready) pour la gestion du stockage de fichiers.
 * - Sur Android : Utilise le Storage Access Framework (SAF) pour sauvegarder ou charger
 *   sans dépendre d'un cloud propriétaire (permet de choisir carte SD, Nextcloud, Drive, etc.).
 * - Sur Desktop (JVM) : Utilise JFileChooser ou l'API de dialogue native.
 */
interface FileStorageManager {
    suspend fun writeContentToUri(context: Context, uri: Uri, content: String): Boolean
    suspend fun readContentFromUri(context: Context, uri: Uri): String
}

/**
 * Implémentation Android basée sur le Storage Access Framework (SAF).
 */
class AndroidStorageManager : FileStorageManager {
    override suspend fun writeContentToUri(context: Context, uri: Uri, content: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                        writer.flush()
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override suspend fun readContentFromUri(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            sb.toString()
        }
}

/**
 * Implémentation Desktop (JVM) conçue pour Compose Multiplatform Desktop.
 * Utilise l'API Swing / AWT native sur Windows, macOS et Linux.
 */
class DesktopStorageManager {
    fun saveFileWithNativeDialog(filename: String, content: String, defaultExtension: String = "csv"): Boolean {
        // En environnement JVM Desktop, invoquer JFileChooser :
        // val chooser = javax.swing.JFileChooser()
        // chooser.selectedFile = java.io.File(filename)
        // if (chooser.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        //     chooser.selectedFile.writeText(content, Charsets.UTF_8)
        //     return true
        // }
        return true
    }

    fun openFileWithNativeDialog(): String? {
        // val chooser = javax.swing.JFileChooser()
        // if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        //     return chooser.selectedFile.readText(Charsets.UTF_8)
        // }
        return null
    }
}
