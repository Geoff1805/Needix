package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.ItemEditDialog
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StockBadge
import com.example.ui.components.StoreBadge
import com.example.ui.viewmodel.InventoryUiState
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.SortOption

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import com.example.ui.notification.ThresholdAlert

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    uiState: InventoryUiState,
    onNavigateToScanner: () -> Unit,
    onTestNotification: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("inventory_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header stats & quick filters
            item {
                InventoryHeader(
                    uiState = uiState,
                    onToggleLowStock = { viewModel.toggleLowStockFilter() },
                    onOpenAlerts = { showAlertsDialog = true }
                )
            }

            // Proactive Threshold Alert Banner
            if (uiState.activeThresholdAlerts.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { showAlertsDialog = true }
                            .testTag("threshold_alerts_banner"),
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Alerte : ${uiState.activeThresholdAlerts.size} article(s) au seuil min",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Niveau de stock bas — Pensez à réapprovisionner pro-activement.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            TextButton(onClick = { showAlertsDialog = true }) {
                                Text("Détails", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Bar
            item {
                SearchAndFilterBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    sortOption = uiState.sortOption,
                    onSortOptionChange = { viewModel.onSortOptionChange(it) },
                    showSortMenu = showSortMenu,
                    onToggleSortMenu = { showSortMenu = it },
                    onNavigateToScanner = onNavigateToScanner
                )
            }

            // Category Chips Row
            item {
                CategoryChipsRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelect = { viewModel.onCategorySelect(it) }
                )
            }

            // Item count & Active filter summary
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.filteredItems.size} article${if (uiState.filteredItems.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (uiState.filterLowStockOnly) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.toggleLowStockFilter() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtre: Stock critique",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer filtre",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Empty state if no items
            if (uiState.filteredItems.isEmpty()) {
                item {
                    EmptyInventoryState(
                        hasFilter = uiState.searchQuery.isNotBlank() || uiState.selectedCategoryId != null || uiState.filterLowStockOnly,
                        onAddClicked = { showAddDialog = true }
                    )
                }
            } else {
                // List of items
                items(
                    items = uiState.filteredItems,
                    key = { it.id }
                ) { item ->
                    val category = uiState.categories.find { it.id == item.categoryId }
                    val store = uiState.storeTypes.find { it.id == item.preferredStoreId }

                    InventoryItemCard(
                        item = item,
                        category = category,
                        store = store,
                        onIncrement = { viewModel.incrementQuantity(item) },
                        onDecrement = { viewModel.decrementQuantity(item) },
                        onEdit = { itemToEdit = item },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_item_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un article")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Add or Edit Dialog
    if (showAddDialog) {
        ItemEditDialog(
            categories = uiState.categories,
            storeTypes = uiState.storeTypes,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                viewModel.saveItem(newItem)
                showAddDialog = false
            },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDeleteStoreType = { viewModel.deleteStoreType(it) },
            onAddCategory = { name, emoji, color -> viewModel.addCategory(name, emoji, color) },
            onAddStoreType = { name, emoji, color -> viewModel.addStoreType(name, emoji, color) }
        )
    }

    if (itemToEdit != null) {
        ItemEditDialog(
            itemToEdit = itemToEdit,
            categories = uiState.categories,
            storeTypes = uiState.storeTypes,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.saveItem(updatedItem)
                itemToEdit = null
            },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDeleteStoreType = { viewModel.deleteStoreType(it) },
            onAddCategory = { name, emoji, color -> viewModel.addCategory(name, emoji, color) },
            onAddStoreType = { name, emoji, color -> viewModel.addStoreType(name, emoji, color) }
        )
    }

    if (showAlertsDialog) {
        ThresholdAlertsDialog(
            alerts = uiState.activeThresholdAlerts,
            onDismiss = { showAlertsDialog = false },
            onIncrementItem = { alert ->
                val item = uiState.items.find { it.id == alert.itemId }
                if (item != null) {
                    viewModel.incrementQuantity(item)
                }
            },
            onTestNotification = onTestNotification
        )
    }
}

@Composable
fun InventoryHeader(
    uiState: InventoryUiState,
    onToggleLowStock: () -> Unit,
    onOpenAlerts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mon Inventaire",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Suivi de stock à domicile & réapprovisionnement",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Notification Bell with Badge
            IconButton(
                onClick = onOpenAlerts,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("notification_alerts_bell")
            ) {
                BadgedBox(
                    badge = {
                        if (uiState.activeThresholdAlerts.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(uiState.activeThresholdAlerts.size.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.activeThresholdAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = "Alertes de stock",
                        tint = if (uiState.activeThresholdAlerts.isNotEmpty()) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Total articles",
                count = uiState.totalItemCount.toString(),
                subtitle = "en base",
                color = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Stock critique",
                count = (uiState.outOfStockCount + uiState.lowStockCount).toString(),
                subtitle = "${uiState.activeThresholdAlerts.size} sous seuil min",
                color = if (uiState.outOfStockCount > 0) Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                textColor = if (uiState.outOfStockCount > 0) Color(0xFFE11D48) else Color(0xFFD97706),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleLowStock() }
            )
        }
    }
}

@Composable
fun ThresholdAlertsDialog(
    alerts: List<ThresholdAlert>,
    onDismiss: () -> Unit,
    onIncrementItem: (ThresholdAlert) -> Unit,
    onTestNotification: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFD97706)
                )
                Text(
                    text = "Alertes de Seuil Minimum",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ces articles ont atteint ou sont passés sous leur seuil d'alerte défini. Le système vous notifie pour éviter la rupture de stock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (alerts.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎉 Tous les stocks sont au-dessus de leur seuil minimal !",
                            color = Color(0xFF065F46),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    alerts.forEach { alert ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (alert.currentQuantity <= 0) Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.itemName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert.currentQuantity <= 0) Color(0xFF9F1239) else Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Stock : ${alert.currentQuantity} ${alert.unit} (seuil : ${alert.minThreshold})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (alert.currentQuantity <= 0) Color(0xFFBE123C) else Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "Lieu d'achat : ${alert.storeName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onIncrementItem(alert) },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+1", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+1")
                                }
                            }
                        }
                    }
                }

                if (onTestNotification != null) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = onTestNotification,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Envoyer une notification de test")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun StatCard(
    title: String,
    count: String,
    subtitle: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onNavigateToScanner: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher (produit, lieu, code)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                } else {
                    IconButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.testTag("scan_shortcut_button")
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner code-barres",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("search_input")
        )

        Box {
            IconButton(
                onClick = { onToggleSortMenu(true) },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Trier",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onToggleSortMenu(false) }
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSortOptionChange(option)
                            onToggleSortMenu(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChipsRow(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("Tous les rayons") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        items(categories) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = {
                    onCategorySelect(if (selectedCategoryId == category.id) null else category.id)
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.iconEmoji)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(category.name)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItemEntity,
    category: CategoryEntity?,
    store: StoreTypeEntity?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.quantity == 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category icon badge
                CategoryIconBadge(
                    emoji = category?.iconEmoji ?: "📦",
                    colorHex = category?.colorHex ?: 0xFF64748BL,
                    size = 44
                )

                // Item info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Location badge
                        Text(
                            text = "📍 ${item.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (item.barcode != null) {
                            Text(
                                text = "• 🏷️ ${item.barcode.takeLast(6)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3-dots Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier l'article") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer row with stock status, store badge, and quantity stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StockBadge(quantity = item.quantity, minThreshold = item.minThreshold)
                    StoreBadge(
                        storeName = store?.name ?: "Courses",
                        iconEmoji = store?.iconEmoji ?: "🛒"
                    )
                }

                // Stepper [ - ] [ count ] [ + ]
                QuantityStepper(
                    quantity = item.quantity,
                    unit = item.unit,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement
                )
            }
        }
    }
}

@Composable
fun EmptyInventoryState(
    hasFilter: Boolean,
    onAddClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasFilter) "Aucun article correspondant" else "Votre inventaire est vide",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasFilter) "Essayez de modifier votre recherche ou de réinitialiser le filtre" else "Commencez par ajouter vos produits de cuisine, d'entretien ou d'hygiène.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onAddClicked) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ajouter un premier article")
        }
    }
}
