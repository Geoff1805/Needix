package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.network.ProductLookupService
import com.example.ui.components.StockBadge
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        ProductLookupService.ensureOfflineDatabaseLoaded(context)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var manualCodeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen title
        Column {
            Text(
                text = "Scanner universel Multi-API",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Alimentation (Open Food Facts, Chomp), Cosmétiques (Open Beauty Facts), Maison (Open Products Facts), Livres (Inventaire.io) & Base hors-ligne",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Camera viewfinder or Permission Banner
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val barcodeScanner = remember {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_QR_CODE,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_ITF
                        )
                        .build()
                    BarcodeScanning.getClient(options)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        barcodeScanner.close()
                        cameraExecutor.shutdown()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { code ->
                                                viewModel.onBarcodeDetected(code)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser scan frame overlay
                Box(
                    modifier = Modifier
                        .size(200.dp, 130.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                )

                // Red scanning guide beam
                Box(
                    modifier = Modifier
                        .size(180.dp, 2.dp)
                        .background(Color(0xFFF43F5E))
                )
            }
        } else {
            // Permission request card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Activer la caméra pour scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "L'accès à la caméra permet de scanner instantanément les codes EAN, UPC et ISBN de vos produits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Autoriser la caméra")
                    }
                }
            }
        }

        // Quick Testing Barcodes (Indispensable for browser emulator testing!)
        Column {
            Text(
                text = "⚡ Tester un code-barres (simulateur multi-catégories) :",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.demoBarcodes) { demo ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            viewModel.onBarcodeDetected(demo.code)
                        }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = demo.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = demo.code,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Manual Barcode Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualCodeInput,
                onValueChange = { manualCodeInput = it },
                label = { Text("Saisie manuelle code EAN / UPC / ISBN") },
                placeholder = { Text("Ex: 3250390001001 ou 9782070612758") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_barcode_input")
            )
            Button(
                onClick = {
                    if (manualCodeInput.isNotBlank()) {
                        viewModel.onBarcodeDetected(manualCodeInput)
                    }
                },
                enabled = manualCodeInput.isNotBlank(),
                modifier = Modifier.testTag("manual_barcode_search_btn")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Rechercher")
            }
        }

        // Status / Loading
        if (uiState.isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Interrogation des bases produits en cours...")
            }
        }

        // Result 1: Product already in local inventory!
        if (uiState.existingItem != null) {
            val item = uiState.existingItem
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFFEFF6FF)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2563EB)
                        )
                        Text(
                            text = "Article déjà en stock !",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StockBadge(quantity = item.quantity, minThreshold = item.minThreshold)
                        Text(
                            text = "Emplacement : ${item.location}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.quickIncrementExisting(item, storeTypes) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajouter +1 au stock")
                        }

                        FilledTonalButton(onClick = { viewModel.reset() }) {
                            Text("Scanner un autre")
                        }
                    }
                }
            }
        }

        // Result 2: Product found via Offline DB or Multi-API cascade!
        if (uiState.onlineProduct != null) {
            val prod = uiState.onlineProduct
            NewScannedProductForm(
                barcode = prod.barcode,
                defaultName = prod.name,
                suggestedCategory = prod.suggestedCategory,
                suggestedUnit = prod.suggestedUnit,
                suggestedNotes = prod.suggestedNotes,
                sourceName = prod.source,
                nutriScore = prod.nutriScore,
                allergens = prod.allergens,
                imageUrl = prod.imageUrl,
                categories = categories,
                storeTypes = storeTypes,
                onAdd = { name, cat, qty, unit, store, loc, notes ->
                    viewModel.addScannedItemToInventory(name, cat, qty, unit, store, loc, notes)
                },
                onCancel = { viewModel.reset() }
            )
        }

        // Result 3: Product not recognized online or locally -> manual creation with barcode
        if (uiState.notFoundMessage != null && uiState.existingItem == null && uiState.onlineProduct == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Code-barres : ${uiState.scannedBarcode}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = uiState.notFoundMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    NewScannedProductForm(
                        barcode = uiState.scannedBarcode ?: "",
                        defaultName = "",
                        suggestedCategory = "autre",
                        suggestedUnit = "pièce",
                        suggestedNotes = "",
                        sourceName = null,
                        nutriScore = null,
                        allergens = null,
                        imageUrl = null,
                        categories = categories,
                        storeTypes = storeTypes,
                        onAdd = { name, cat, qty, unit, store, loc, notes ->
                            viewModel.addScannedItemToInventory(name, cat, qty, unit, store, loc, notes)
                        },
                        onCancel = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun NewScannedProductForm(
    barcode: String,
    defaultName: String,
    suggestedCategory: String,
    suggestedUnit: String = "pièce",
    suggestedNotes: String = "",
    sourceName: String? = null,
    nutriScore: String? = null,
    allergens: String? = null,
    imageUrl: String?,
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>,
    onAdd: (name: String, categoryId: String, quantity: Int, unit: String, storeId: String, location: String, notes: String) -> Unit,
    onCancel: () -> Unit
) {
    val defaultLocation = when (suggestedCategory) {
        "hygiene" -> "Salle de bain"
        "pharmacie" -> "Pharmacie"
        "bricolage" -> "Garage / Cave"
        "menager" -> "Placard"
        "autre" -> if (suggestedUnit == "livre") "Bibliothèque / Salon" else "Maison"
        else -> "Cuisine"
    }
    val defaultStore = when (suggestedCategory) {
        "pharmacie" -> storeTypes.find { it.id == "pharmacy" }?.id ?: "supermarket"
        "bricolage" -> storeTypes.find { it.id == "brico" }?.id ?: "supermarket"
        else -> storeTypes.firstOrNull()?.id ?: "supermarket"
    }

    var name by remember(barcode, defaultName) { mutableStateOf(defaultName) }
    var selectedCategoryId by remember(barcode, suggestedCategory) { mutableStateOf(suggestedCategory) }
    var quantity by remember(barcode) { mutableIntStateOf(1) }
    var unit by remember(barcode, suggestedUnit) { mutableStateOf(suggestedUnit) }
    var selectedStoreId by remember(barcode, defaultStore) { mutableStateOf(defaultStore) }
    var location by remember(barcode, defaultLocation) { mutableStateOf(defaultLocation) }
    var notes by remember(barcode, suggestedNotes) { mutableStateOf(suggestedNotes) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!sourceName.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ Source : $sourceName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (!nutriScore.isNullOrBlank()) {
                        val nutriColor = when (nutriScore.uppercase()) {
                            "A" -> Color(0xFF038141)
                            "B" -> Color(0xFF85BB2F)
                            "C" -> Color(0xFFFECB02)
                            "D" -> Color(0xFFEE8100)
                            else -> Color(0xFFE63E11)
                        }
                        Surface(
                            color = nutriColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Nutri-Score ${nutriScore.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (nutriScore.uppercase() == "C") Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Photo produit",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Column {
                    Text(
                        text = "Ajouter à votre inventaire",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code : $barcode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!allergens.isNullOrBlank()) {
                        Text(
                            text = "⚠️ Allergènes : $allergens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du produit *") },
                placeholder = { Text("Ex: Huile d'olive...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category selector
            Text("Rayon / Catégorie :", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text("${cat.iconEmoji} ${cat.name}") }
                    )
                }
            }

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

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Emplacement dans la maison") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Nutri-Score / Détails") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name, selectedCategoryId, quantity, unit, selectedStoreId, location, notes)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ajouter au stock")
                }

                TextButton(onClick = onCancel) {
                    Text("Annuler")
                }
            }
        }
    }
}
