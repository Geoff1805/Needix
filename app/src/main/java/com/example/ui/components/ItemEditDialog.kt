package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.StoreTypeEntity

@Composable
fun ItemEditDialog(
    itemToEdit: InventoryItemEntity? = null,
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>,
    prefilledBarcode: String? = null,
    onDismiss: () -> Unit,
    onSave: (InventoryItemEntity) -> Unit,
    onDeleteCategory: ((CategoryEntity) -> Unit)? = null,
    onDeleteStoreType: ((StoreTypeEntity) -> Unit)? = null,
    onAddCategory: ((String, String, Long) -> Unit)? = null,
    onAddStoreType: ((String, String, Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(itemToEdit?.categoryId ?: categories.firstOrNull()?.id ?: "autre")
    }
    var quantity by remember { mutableIntStateOf(itemToEdit?.quantity ?: 1) }
    var minThreshold by remember { mutableIntStateOf(itemToEdit?.minThreshold ?: 1) }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "pièce") }
    var preferredStoreId by remember {
        mutableStateOf(itemToEdit?.preferredStoreId ?: storeTypes.firstOrNull()?.id ?: "supermarket")
    }
    var location by remember { mutableStateOf(itemToEdit?.location ?: "Maison") }
    var barcode by remember { mutableStateOf(itemToEdit?.barcode ?: prefilledBarcode ?: "") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    var showCategoryPickerDialog by remember { mutableStateOf(false) }
    var showStorePickerDialog by remember { mutableStateOf(false) }

    val commonUnits = listOf("pièce", "bouteille", "boîte", "paquet", "kg", "g", "L", "flacon", "rouleau")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (itemToEdit == null) "Ajouter un article" else "Modifier l'article",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit *") },
                    placeholder = { Text("Ex: Huile d'olive, Lessive...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                // Category selector
                val selectedCategory = categories.find { it.id == selectedCategoryId }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Catégorie", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCategoryPickerDialog = true }
                            .testTag("category_selector_field"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(selectedCategory?.iconEmoji ?: "📦", fontSize = 20.sp)
                                Text(
                                    text = selectedCategory?.name ?: "Choisir une catégorie",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choisir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick category chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text("${cat.iconEmoji} ${cat.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Quantity and Unit row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantity.toString(),
                        onValueChange = { quantity = it.toIntOrNull() ?: 0 },
                        label = { Text("Quantité en stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Minimum Alert Threshold (Notification trigger)
                OutlinedTextField(
                    value = minThreshold.toString(),
                    onValueChange = { minThreshold = (it.toIntOrNull() ?: 1).coerceAtLeast(0) },
                    label = { Text("Seuil minimum d'alerte (seuil de notification)") },
                    supportingText = { Text("Une alerte et notification proactive sont envoyées dès que le stock est <= ce seuil.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_min_threshold_input")
                )

                // Quick unit suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonUnits.take(4).forEach { u ->
                        TextButton(
                            onClick = { unit = u },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(u, fontSize = 11.sp)
                        }
                    }
                }

                // Preferred store selector (destination pour la liste de courses)
                val selectedStore = storeTypes.find { it.id == preferredStoreId }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Magasin préféré (destination courses)", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showStorePickerDialog = true }
                            .testTag("store_selector_field"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(selectedStore?.iconEmoji ?: "🛒", fontSize = 20.sp)
                                Text(
                                    text = selectedStore?.name ?: "Choisir un magasin",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choisir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick store chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(storeTypes) { st ->
                            FilterChip(
                                selected = preferredStoreId == st.id,
                                onClick = { preferredStoreId = st.id },
                                label = { Text("${st.iconEmoji} ${st.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Location in the house
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Emplacement dans la maison") },
                    placeholder = { Text("Ex: Placard cuisine, Buanderie...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Barcode
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Code-barres (facultatif)") },
                    placeholder = { Text("EAN-13, ex: 3014260020123") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Détails (facultatif)") },
                    placeholder = { Text("Marque préférée, spécificités...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalItem = (itemToEdit ?: InventoryItemEntity(
                            name = name.trim(),
                            categoryId = selectedCategoryId
                        )).copy(
                            name = name.trim(),
                            categoryId = selectedCategoryId,
                            quantity = quantity.coerceAtLeast(0),
                            minThreshold = minThreshold.coerceAtLeast(0),
                            unit = unit.ifBlank { "pièce" },
                            preferredStoreId = preferredStoreId,
                            location = location.ifBlank { "Maison" },
                            barcode = barcode.trim().takeIf { it.isNotBlank() },
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(finalItem)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    // Category Picker Dialog
    if (showCategoryPickerDialog) {
        CategoryPickerDialog(
            categories = categories,
            selectedId = selectedCategoryId,
            onDismiss = { showCategoryPickerDialog = false },
            onSelect = { selectedCategoryId = it },
            onDeleteCategory = onDeleteCategory,
            onAddCategory = onAddCategory
        )
    }

    // Store Picker Dialog
    if (showStorePickerDialog) {
        StorePickerDialog(
            storeTypes = storeTypes,
            selectedId = preferredStoreId,
            onDismiss = { showStorePickerDialog = false },
            onSelect = { preferredStoreId = it },
            onDeleteStoreType = onDeleteStoreType,
            onAddStoreType = onAddStoreType
        )
    }
}

@Composable
fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDeleteCategory: ((CategoryEntity) -> Unit)? = null,
    onAddCategory: ((String, String, Long) -> Unit)? = null
) {
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("🏷️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choisir une catégorie",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedId == cat.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(cat.id)
                                onDismiss()
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = cat.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sélectionné",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (onDeleteCategory != null && categories.size > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { categoryToDelete = cat }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer la catégorie ${cat.name}",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (onAddCategory != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nouvelle catégorie")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )

    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Supprimer « ${cat.name} » ?") },
            text = { Text("Les articles associés à cette catégorie seront automatiquement réassignés à la catégorie Divers.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory?.invoke(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouvelle catégorie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newCatName.isNotBlank(),
                    onClick = {
                        onAddCategory?.invoke(newCatName, newCatEmoji, 0xFF4F46E5)
                        showAddDialog = false
                        newCatName = ""
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun StorePickerDialog(
    storeTypes: List<StoreTypeEntity>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDeleteStoreType: ((StoreTypeEntity) -> Unit)? = null,
    onAddStoreType: ((String, String, Long) -> Unit)? = null
) {
    var storeToDelete by remember { mutableStateOf<StoreTypeEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var newStoreEmoji by remember { mutableStateOf("🏪") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choisir le magasin / lieu d'achat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                storeTypes.forEach { st ->
                    val isSelected = selectedId == st.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(st.id)
                                onDismiss()
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = st.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = st.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sélectionné",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (onDeleteStoreType != null && storeTypes.size > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { storeToDelete = st }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer le magasin ${st.name}",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (onAddStoreType != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nouveau magasin")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )

    if (storeToDelete != null) {
        val st = storeToDelete!!
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Supprimer le magasin « ${st.name} » ?") },
            text = { Text("Les articles associés à ce magasin seront automatiquement réassignés au Supermarché.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStoreType?.invoke(st)
                        storeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouveau magasin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newStoreEmoji,
                        onValueChange = { newStoreEmoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newStoreName.isNotBlank(),
                    onClick = {
                        onAddStoreType?.invoke(newStoreName, newStoreEmoji, 0xFF059669)
                        showAddDialog = false
                        newStoreName = ""
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
