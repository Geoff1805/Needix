package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.StoreBadge
import com.example.ui.viewmodel.ShoppingUiState
import com.example.ui.viewmodel.ShoppingViewModel

@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    uiState: ShoppingUiState,
    onOpenStoreScanner: (() -> Unit)? = null,
    onOpenRecipeAssistant: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToChangeStore by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var showOnHoldSection by remember { mutableStateOf(true) }
    var showPurchasedSection by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("shopping_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Listes de courses",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Organisées par enseigne & lieu d'achat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.purchasedItems.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearPurchased() },
                                modifier = Modifier.testTag("clear_purchased_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Vider les articles achetés",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (onOpenStoreScanner != null || onOpenRecipeAssistant != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenStoreScanner != null) {
                                Button(
                                    onClick = onOpenStoreScanner,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_store_scanner_from_shopping")
                                ) {
                                    Text("📷 Scanner en magasin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (onOpenRecipeAssistant != null) {
                                OutlinedButton(
                                    onClick = onOpenRecipeAssistant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_recipe_assistant_from_shopping")
                                ) {
                                    Text("🍳 Recette / Screenshot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Store Tabs / Filters (Supermarché, Pharmacie, Internet, etc.)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedStoreId == null,
                            onClick = { viewModel.onStoreSelect(null) },
                            label = { Text("Toutes les listes (${uiState.totalActiveCount})") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    items(uiState.storeTypes) { store ->
                        val countForStore = uiState.activeItems.count { it.storeId == store.id }
                        FilterChip(
                            selected = uiState.selectedStoreId == store.id,
                            onClick = {
                                viewModel.onStoreSelect(if (uiState.selectedStoreId == store.id) null else store.id)
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(store.iconEmoji)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${store.name} ($countForStore)")
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

            // Active Items Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "À acheter (${uiState.activeItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.activeItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aucun article à acheter pour le moment !",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Les produits à stock 0 y seront automatiquement ajoutés.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.activeItems,
                    key = { it.id }
                ) { item ->
                    val category = uiState.categories.find { it.id == item.categoryId }
                    val store = uiState.storeTypes.find { it.id == item.storeId }

                    ShoppingItemCard(
                        item = item,
                        category = category,
                        store = store,
                        onTogglePurchased = { viewModel.togglePurchased(item) },
                        onToggleOnHold = { viewModel.toggleOnHold(item) },
                        onChangeStore = { itemToChangeStore = item },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }

            // On Hold Items Section ("En attente / Pas besoin immédiatement")
            if (uiState.onHoldItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showOnHoldSection = !showOnHoldSection },
                        color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "En attente (${uiState.onHoldItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "— différés",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Icon(
                                imageVector = if (showOnHoldSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF92400E)
                            )
                        }
                    }
                }

                if (showOnHoldSection) {
                    items(
                        items = uiState.onHoldItems,
                        key = { it.id }
                    ) { item ->
                        val category = uiState.categories.find { it.id == item.categoryId }
                        val store = uiState.storeTypes.find { it.id == item.storeId }

                        ShoppingItemCard(
                            item = item,
                            category = category,
                            store = store,
                            onTogglePurchased = { viewModel.togglePurchased(item) },
                            onToggleOnHold = { viewModel.toggleOnHold(item) },
                            onChangeStore = { itemToChangeStore = item },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }

            // Purchased Items Section
            if (uiState.purchasedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showPurchasedSection = !showPurchasedSection },
                        color = Color(0xFFD1FAE5).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Articles achetés (${uiState.purchasedItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            Icon(
                                imageVector = if (showPurchasedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF065F46)
                            )
                        }
                    }
                }

                if (showPurchasedSection) {
                    items(
                        items = uiState.purchasedItems,
                        key = { it.id }
                    ) { item ->
                        PurchasedItemCard(
                            item = item,
                            onRestock = { viewModel.restockItem(item, item.quantityNeeded) },
                            onDelete = { viewModel.deleteItem(item) },
                            onUncheck = { viewModel.togglePurchased(item) }
                        )
                    }
                }
            }
        }

        // FAB to add ad-hoc shopping item
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_shopping_item_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter aux courses")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Add Ad-Hoc Shopping Item Dialog
    if (showAddDialog) {
        AddShoppingItemDialog(
            categories = uiState.categories,
            storeTypes = uiState.storeTypes,
            defaultStoreId = uiState.selectedStoreId ?: "supermarket",
            onDismiss = { showAddDialog = false },
            onAdd = { name, cat, store, qty, unit ->
                viewModel.addItem(name, cat, store, qty, unit)
                showAddDialog = false
            }
        )
    }

    // Change Store Dialog ("Je dois pouvoir changer les produit de type d'endroit où je les achete")
    if (itemToChangeStore != null) {
        ChangeStoreDialog(
            item = itemToChangeStore!!,
            storeTypes = uiState.storeTypes,
            onDismiss = { itemToChangeStore = null },
            onSelectStore = { newStoreId ->
                viewModel.changeStore(itemToChangeStore!!, newStoreId)
                itemToChangeStore = null
            }
        )
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItemEntity,
    category: CategoryEntity?,
    store: StoreTypeEntity?,
    onTogglePurchased: () -> Unit,
    onToggleOnHold: () -> Unit,
    onChangeStore: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("shopping_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.isOnHold) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                modifier = Modifier.testTag("check_item_${item.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isOnHold) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clickable store badge to switch store type!
                    StoreBadge(
                        storeName = store?.name ?: "Magasin",
                        iconEmoji = store?.iconEmoji ?: "🛒",
                        onClick = onChangeStore
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.quantityNeeded} ${item.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.isOnHold) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "En attente",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions: Put On Hold & 3-dots Menu
            IconButton(
                onClick = onToggleOnHold,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (item.isOnHold) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                    contentDescription = if (item.isOnHold) "Réactiver" else "Mettre en attente",
                    tint = if (item.isOnHold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Changer de magasin") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onChangeStore()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isOnHold) "Réactiver l'article" else "Mettre en attente") },
                        leadingIcon = {
                            Icon(
                                if (item.isOnHold) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleOnHold()
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
    }
}

@Composable
fun PurchasedItemCard(
    item: ShoppingItemEntity,
    onRestock: () -> Unit,
    onDelete: () -> Unit,
    onUncheck: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFECFDF5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUncheck) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Décocher",
                    tint = Color(0xFF059669)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.LineThrough,
                    color = Color(0xFF065F46)
                )
                Text(
                    text = "${item.quantityNeeded} ${item.unit} acheté",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF047857)
                )
            }

            // Quick restock button: "Restocker (+qté)"
            Button(
                onClick = onRestock,
                modifier = Modifier.padding(end = 4.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669)
                )
            ) {
                Text("Restocker", fontSize = 12.sp)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ChangeStoreDialog(
    item: ShoppingItemEntity,
    storeTypes: List<StoreTypeEntity>,
    onDismiss: () -> Unit,
    onSelectStore: (String) -> Unit
) {
    var selectedStoreId by remember { mutableStateOf(item.storeId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changer le lieu d'achat") },
        text = {
            Column {
                Text(
                    text = "Où souhaitez-vous acheter « ${item.name} » ?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                storeTypes.forEach { store ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStoreId = store.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStoreId == store.id,
                            onClick = { selectedStoreId = store.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = store.iconEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedStoreId == store.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelectStore(selectedStoreId) }) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun AddShoppingItemDialog(
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>,
    defaultStoreId: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, categoryId: String, storeId: String, quantity: Int, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "autre") }
    var selectedStoreId by remember { mutableStateOf(defaultStoreId) }
    var quantity by remember { mutableIntStateOf(1) }
    var unit by remember { mutableStateOf("pièce") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à la liste de courses") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'article *") },
                    placeholder = { Text("Ex: Lait, Savon, Aspirine...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity.toString(),
                        onValueChange = { quantity = it.toIntOrNull() ?: 1 },
                        label = { Text("Quantité") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Store selection chips
                Text("Magasin / Destination :", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(storeTypes) { st ->
                        FilterChip(
                            selected = selectedStoreId == st.id,
                            onClick = { selectedStoreId = st.id },
                            label = { Text("${st.iconEmoji} ${st.name}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, selectedCategoryId, selectedStoreId, quantity, unit)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
