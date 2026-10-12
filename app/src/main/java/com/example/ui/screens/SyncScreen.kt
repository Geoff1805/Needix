package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.billing.BillingRepository
import com.example.data.export.AndroidStorageManager
import com.example.data.export.UniversalExportManager
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.ui.components.CategoryIconBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(
    items: List<InventoryItemEntity>,
    shoppingItems: List<ShoppingItemEntity> = emptyList(),
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onTestNotification: (() -> Unit)? = null,
    onNavigateToPaywall: () -> Unit = {},
    onAddCategory: (String, String, Long) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit = {},
    onAddStoreType: (String, String, Long) -> Unit,
    onDeleteStoreType: (StoreTypeEntity) -> Unit = {},
    onImportCsv: (Uri, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { AndroidStorageManager() }

    val billingManager by BillingRepository.activeManager.collectAsStateWithLifecycle()
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()
    val trialDays by billingManager.trialDaysRemaining.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var selectedManageTab by remember { mutableIntStateOf(0) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var storeToDelete by remember { mutableStateOf<StoreTypeEntity?>(null) }

    // SAF Document Creator for CSV export
    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csvContent = UniversalExportManager.generateStandardCsv(items)
                val success = storageManager.writeContentToUri(context, uri, csvContent)
                if (success) {
                    Toast.makeText(context, "Export CSV réussi (SAF) !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Erreur lors de l'écriture du fichier.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Document Creator for full JSON backup
    val createJsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val jsonContent = UniversalExportManager.generateJsonBackup(items, shoppingItems, categories, storeTypes)
                val success = storageManager.writeContentToUri(context, uri, jsonContent)
                if (success) {
                    Toast.makeText(context, "Sauvegarde JSON créée avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Erreur lors de la sauvegarde JSON.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF File Picker for Import
    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            showImportConfirmDialog = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // En-tête
        Column {
            Text(
                text = stringResource(R.string.data_backup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.storage_saf_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section Abonnement & Licence Commerciale (RevenueCat)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.commercial_paywall_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPremium) {
                                stringResource(R.string.flavor_commercial_name)
                            } else {
                                stringResource(R.string.trial_active_message, trialDays)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPaywall,
                        modifier = Modifier.testTag("open_paywall_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPremium) "Abonné" else "Essai 14j")
                    }
                }
            }
        }

        // Section Site Vitrine & Web App (needix.app)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().testTag("web_landing_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF2563EB)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.web_landing_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "needix.app • Wasm & File System API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.web_landing_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://needix.app/"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "https://needix.app/", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("open_web_app_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.web_landing_button), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Export Universel & FOSS (CSV & JSON)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF059669)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Formats Universels Ouverts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standard CSV & Sauvegarde JSON sans verrou cloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showTutorialDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Aide formats",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider()

                // Option 1 : Export CSV
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.export_csv_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.export_csv_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createCsvDocumentLauncher.launch("needix_inventaire_$timestamp.csv")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_csv_saf")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.export_csv_button))
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val csvContent = UniversalExportManager.generateStandardCsv(items)
                                    val uri = UniversalExportManager.createTempShareFile(context, "needix_export.csv", csvContent)
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager le fichier CSV"))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_share_csv")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Divider()

                // Option 2 : Sauvegarde Complète JSON
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.export_json_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.export_json_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            createJsonDocumentLauncher.launch("needix_backup_$timestamp.json")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_json_backup")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_json_button))
                    }
                }

                Divider()

                // Option 3 : Import de données
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.import_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.import_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { importDocumentLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_import_document")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sélectionner un fichier (CSV ou JSON)")
                    }
                }
            }
        }

        // Section 2: Notifications proactives
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFD97706)
                            )
                        }
                        Column {
                            Text(
                                text = "Alertes de Stock Bas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Notifications automatiques au seuil critique",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        modifier = Modifier.testTag("switch_notifications")
                    )
                }

                if (onTestNotification != null) {
                    OutlinedButton(
                        onClick = onTestNotification,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_notification")
                    ) {
                        Text("Envoyer une notification de test")
                    }
                }
            }
        }

        // Section 3: Gestion des Catégories & Magasins
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().testTag("manage_categories_and_stores_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Catégories & Magasins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${categories.size} catégories • ${storeTypes.size} magasins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Onglets de bascule Catégories / Magasins
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedManageTab == 0,
                        onClick = { selectedManageTab = 0 },
                        label = { Text("Catégories (${categories.size})") },
                        modifier = Modifier.weight(1f).testTag("tab_manage_categories"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = selectedManageTab == 1,
                        onClick = { selectedManageTab = 1 },
                        label = { Text("Magasins (${storeTypes.size})") },
                        modifier = Modifier.weight(1f).testTag("tab_manage_stores"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Bouton d'ajout selon l'onglet actif
                if (selectedManageTab == 0) {
                    OutlinedButton(
                        onClick = { showAddCategoryDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_add_category_dialog")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter une catégorie")
                    }

                    // Liste des catégories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            val count = items.count { it.categoryId == cat.id }
                            Surface(
                                modifier = Modifier.fillMaxWidth().testTag("manage_cat_${cat.id}"),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CategoryIconBadge(
                                            emoji = cat.iconEmoji,
                                            colorHex = cat.colorHex,
                                            size = 36
                                        )
                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "$count article${if (count > 1) "s" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        enabled = categories.size > 1,
                                        modifier = Modifier.testTag("btn_delete_cat_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer la catégorie ${cat.name}",
                                            tint = if (categories.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddStoreDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_add_store_dialog")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter un magasin")
                    }

                    // Liste des magasins
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        storeTypes.forEach { st ->
                            val invCount = items.count { it.preferredStoreId == st.id }
                            val shopCount = shoppingItems.count { it.storeId == st.id }
                            Surface(
                                modifier = Modifier.fillMaxWidth().testTag("manage_store_${st.id}"),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(st.colorHex).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = st.iconEmoji, fontSize = 18.sp)
                                        }
                                        Column {
                                            Text(
                                                text = st.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "$invCount en inventaire • $shopCount en courses",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { storeToDelete = st },
                                        enabled = storeTypes.size > 1,
                                        modifier = Modifier.testTag("btn_delete_store_${st.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer le magasin ${st.name}",
                                            tint = if (storeTypes.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue d'aide aux formats
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = { Text("Indépendance des Données", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Needix garantit que vos inventaires ne sont jamais enfermés chez un fournisseur cloud :")
                    Text("• Format CSV : Lisible par tout tableur (Excel, LibreOffice, OnlyOffice, Sheets).")
                    Text("• Format JSON : Sauvegarde intégrale sans perte (structures, historiques et paramètres).")
                    Text("• Sélecteur natif SAF : Vous pouvez enregistrer sur votre mémoire interne, carte SD, Nextcloud personnel ou Drive.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text("Compris")
                }
            }
        )
    }

    // Confirmation Import Dialogue
    if (showImportConfirmDialog != null) {
        val targetUri = showImportConfirmDialog!!
        var replaceAll by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = null },
            title = { Text("Importer des données", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Comment souhaitez-vous importer les données de ce fichier ?")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceAll = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = !replaceAll,
                            onClick = { replaceAll = false }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Fusionner / Mettre à jour avec l'inventaire existant")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceAll = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = replaceAll,
                            onClick = { replaceAll = true }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Remplacer complètement l'inventaire actuel")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsv(targetUri, replaceAll)
                        showImportConfirmDialog = null
                    }
                ) {
                    Text("Confirmer l'import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        var categoryEmoji by remember { mutableStateOf("🏷️") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nouvelle catégorie", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Nom de la catégorie") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = categoryEmoji,
                        onValueChange = { categoryEmoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = categoryName.isNotBlank(),
                    onClick = {
                        onAddCategory(categoryName, categoryEmoji, 0xFF4F46E5)
                        showAddCategoryDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Add Store Dialog
    if (showAddStoreDialog) {
        var storeName by remember { mutableStateOf("") }
        var storeEmoji by remember { mutableStateOf("🏪") }

        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("Nouveau magasin", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nom du magasin") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeEmoji,
                        onValueChange = { storeEmoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = storeName.isNotBlank(),
                    onClick = {
                        onAddStoreType(storeName, storeEmoji, 0xFF059669)
                        showAddStoreDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression de Catégorie
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        val count = items.count { it.categoryId == cat.id }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Supprimer « ${cat.name} » ?")
            },
            text = {
                Text(
                    if (count > 0)
                        "Cette catégorie contient $count article(s). Ils seront automatiquement réassignés à la catégorie Divers (« autre »)."
                    else
                        "Êtes-vous sûr de vouloir supprimer définitivement cette catégorie ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_cat_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression de Magasin
    if (storeToDelete != null) {
        val st = storeToDelete!!
        val invCount = items.count { it.preferredStoreId == st.id }
        val shopCount = shoppingItems.count { it.storeId == st.id }
        val total = invCount + shopCount
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Supprimer le magasin « ${st.name} » ?")
            },
            text = {
                Text(
                    if (total > 0)
                        "Ce magasin est associé à $invCount article(s) d'inventaire et $shopCount article(s) de courses. Ils seront automatiquement réassignés au Supermarché (« supermarket »)."
                    else
                        "Êtes-vous sûr de vouloir supprimer définitivement ce magasin ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStoreType(st)
                        storeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_store_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
