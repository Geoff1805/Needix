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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    onSave: (InventoryItemEntity) -> Unit
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
            onSelect = { selectedCategoryId = it }
        )
    }

    // Store Picker Dialog
    if (showStorePickerDialog) {
        StorePickerDialog(
            storeTypes = storeTypes,
            selectedId = preferredStoreId,
            onDismiss = { showStorePickerDialog = false },
            onSelect = { preferredStoreId = it }
        )
    }
}

@Composable
fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                        }
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
}

@Composable
fun StorePickerDialog(
    storeTypes: List<StoreTypeEntity>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                        }
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
}
