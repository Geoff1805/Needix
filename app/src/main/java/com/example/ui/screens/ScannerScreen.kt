package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
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
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.InventoryItemEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.StoreTypeEntity
import com.example.data.network.ProductLookupResult
import com.example.data.network.ProductLookupService
import com.example.data.receipt.ReceiptParser
import com.example.data.recipe.RecipeIngredientParser
import com.example.ui.components.StockBadge
import com.example.ui.viewmodel.InStoreScanBehavior
import com.example.ui.viewmodel.ScannerUiState
import com.example.ui.viewmodel.ScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        ProductLookupService.ensureOfflineDatabaseLoaded(context)
    }

    // Bip sonore + retour haptique dès qu'un code-barres ou une recette est lu
    LaunchedEffect(viewModel) {
        viewModel.beepEvents.collect {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {
            }
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 160)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(uiState.scannedBarcode, uiState.isSearching) {
        if (uiState.scannedBarcode != null && !uiState.isSearching) {
            scrollState.animateScrollTo(0)
        }
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

    val activeMode = uiState.activeTabMode
    var manualCodeInput by remember { mutableStateOf("") }
    var digitalReceiptInput by remember { mutableStateOf("") }
    var recipeUrlOrTextInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val receiptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    digitalReceiptInput = content
                    viewModel.analyzeDigitalReceipt(content)
                }
            } catch (_: Exception) {
            }
        }
    }

    // Sélecteur de capture d'écran (Screenshot d'ingrédients de recette) avec ML Kit OCR 100% local
    val recipeScreenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extracted = visionText.text
                        if (extracted.isNotBlank()) {
                            recipeUrlOrTextInput = extracted
                            viewModel.analyzeRecipeTextOrOcr(
                                rawText = extracted,
                                sourceLabel = "📸 Capture d'écran d'ingrédients (OCR ML Kit)"
                            )
                        }
                        recognizer.close()
                    }
                    .addOnFailureListener {
                        recognizer.close()
                    }
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Column {
            Text(
                text = "Courses en Magasin & Recettes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Scannez au magasin (testez les 4 options en direct), importez un screenshot/lien de recette pour ajouter les ingrédients manquants, ou lisez un ticket numérique.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sélecteur des 3 grands modes : Scanner Magasin / Recettes (Screenshot & Lien) / Ticket numérique
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeMode == "barcode",
                onClick = { viewModel.setActiveTabMode("barcode") },
                label = { Text("📷 Magasin", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeMode == "recipe",
                onClick = { viewModel.setActiveTabMode("recipe") },
                label = { Text("🍳 Recettes", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = activeMode == "receipt",
                onClick = { viewModel.setActiveTabMode("receipt") },
                label = { Text("🧾 Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
        }

        if (activeMode == "barcode") {
            // =========================================================================
            // SÉLECTEUR EN DIRECT DES 4 OPTIONS DE SCAN AU MAGASIN (+ MODE TOUT-EN-UN)
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎛️ Testez les 4 expériences de scan en magasin :",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(InStoreScanBehavior.entries) { behavior ->
                            val isSelected = uiState.scanBehavior == behavior
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setScanBehavior(behavior) },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = behavior.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = behavior.badge,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (uiState.isSearching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔊 Bip ! Lecture du code ${uiState.scannedBarcode ?: ""} en cours...")
                }
            }

            // =========================================================================
            // AFFICHAGE ADAPTATIF SELON L'OPTION SÉLECTIONNÉE (1, 2, 3, 4 ou 5)
            // =========================================================================
            if (uiState.existingItem != null || uiState.onlineProduct != null) {
                InStoreScanResultSection(
                    uiState = uiState,
                    categories = categories,
                    storeTypes = storeTypes,
                    onIncrementExisting = { item, delta ->
                        viewModel.quickIncrementExisting(item, storeTypes, delta)
                    },
                    onUndoExpress = {
                        viewModel.undoLastExpressRestock(storeTypes)
                    },
                    onAddToCart = {
                        viewModel.addCurrentScannedToCart()
                    },
                    onAddNewToInventory = { name, cat, qty, unit, store, loc, notes ->
                        viewModel.addScannedItemToInventory(name, cat, qty, unit, store, loc, notes)
                    },
                    onReset = { viewModel.reset() }
                )
            }

            // TIROIR PANIER CADDIE EN COURS (Option 3 ou Tout-en-un)
            if (uiState.scanBehavior == InStoreScanBehavior.CART_BATCH || uiState.inStoreCart.isNotEmpty()) {
                InStoreCartDrawerCard(
                    cartItems = uiState.inStoreCart,
                    onUpdateQty = { barcode, delta -> viewModel.updateCartItemQuantity(barcode, delta) },
                    onCheckout = { viewModel.checkoutInStoreCart(storeTypes) },
                    onClear = { viewModel.clearInStoreCart() }
                )
            }

            // Résultat 3 : Code-barres lu mais inconnu des bases -> Proposition de création immédiate
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
                            text = "🔊 Code-barres détecté : ${uiState.scannedBarcode}",
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

            // Camera viewfinder or Permission Banner
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
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
                                                    viewModel.onBarcodeDetected(
                                                        barcode = code,
                                                        forceNewScan = false,
                                                        storeTypes = storeTypes
                                                    )
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
                                    boundCamera = cameraProvider.bindToLifecycle(
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

                    // HUD Mode Indicator in Top-Left + Torch Button in Top-Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = uiState.scanBehavior.label,
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = if (isTorchOn) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable {
                                isTorchOn = !isTorchOn
                                boundCamera?.cameraControl?.enableTorch(isTorchOn)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flash",
                                    tint = if (isTorchOn) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isTorchOn) "Flash ON" else "Flash",
                                    color = if (isTorchOn) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Laser scan frame overlay
                    Box(
                        modifier = Modifier
                            .size(210.dp, 125.dp)
                            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
                    )

                    // Red scanning guide beam
                    Box(
                        modifier = Modifier
                            .size(190.dp, 2.dp)
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
                            text = "Activer la caméra pour scanner en magasin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "L'accès à la caméra permet de scanner vos produits au magasin avec bip sonore et mise à jour directe du stock.",
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

            // Quick Testing Barcodes (Rayon virtuel pour tester les 4 options en 1 tap)
            Column {
                Text(
                    text = "🏪 Rayon de test (touchez un produit pour simuler le scan avec l'option choisie) :",
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
                                viewModel.onBarcodeDetected(demo.code, forceNewScan = true, storeTypes = storeTypes)
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
                            viewModel.onBarcodeDetected(manualCodeInput, forceNewScan = true, storeTypes = storeTypes)
                        }
                    },
                    enabled = manualCodeInput.isNotBlank(),
                    modifier = Modifier.testTag("manual_barcode_search_btn")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                }
            }
        } else if (activeMode == "recipe") {
            // =========================================================================
            // MODE ASSISTANT RECETTES : SCREENSHOT D'INGRÉDIENTS (OCR) & LIEN DE RECETTE
            // =========================================================================
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Assistant Recettes (Screenshot & Lien Web)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Envoyez une capture d'écran d'ingrédients ou collez un lien de recette : Needix détecte ce que vous possédez déjà et pré-coche les ingrédients manquants !",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Bouton principal : Envoyer un Screenshot d'ingrédients (Galerie -> ML Kit OCR)
                    Button(
                        onClick = { recipeScreenshotPicker.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_recipe_screenshot_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📸 Envoyer un Screenshot d'ingrédients (OCR)")
                    }

                    // Exemples rapides en 1 tap pour tester immédiatement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                recipeUrlOrTextInput = RecipeIngredientParser.SAMPLE_CREPES_RECIPE_OCR.trimIndent()
                                viewModel.analyzeRecipeTextOrOcr(
                                    rawText = recipeUrlOrTextInput,
                                    sourceLabel = "📸 Exemple Screenshot OCR : Crêpes Maison"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🥞 Démo Screenshot Crêpes", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = {
                                recipeUrlOrTextInput = RecipeIngredientParser.SAMPLE_CARBONARA_RECIPE.trimIndent()
                                viewModel.analyzeRecipeTextOrOcr(
                                    rawText = recipeUrlOrTextInput,
                                    sourceLabel = "🔗 Exemple Lien Recette : Pâtes Carbonara"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🍝 Démo Lien Carbonara", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = recipeUrlOrTextInput,
                        onValueChange = { recipeUrlOrTextInput = it },
                        label = { Text("Lien URL de recette complète ou liste d'ingrédients") },
                        placeholder = { Text("Collez https://marmiton.org/... ou le texte des ingrédients") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_url_or_text_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val trimmed = recipeUrlOrTextInput.trim()
                            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                viewModel.analyzeRecipeUrl(trimmed)
                            } else {
                                viewModel.analyzeRecipeTextOrOcr(trimmed)
                            }
                        },
                        enabled = recipeUrlOrTextInput.isNotBlank() && !uiState.isAnalyzingRecipe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_recipe_btn")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔍 Analyser les ingrédients et comparer au stock")
                    }
                }
            }

            if (uiState.isAnalyzingRecipe) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extraction des ingrédients et comparaison avec votre stock...")
                }
            }

            // Carte de validation des ingrédients de la recette (Manquants pré-cochés vs En stock décochés)
            if (uiState.recipeResult != null) {
                val recipe = uiState.recipeResult
                val missingCount = recipe.ingredients.count { it.isMissing }
                val ownedCount = recipe.ingredients.size - missingCount
                val selectedCount = recipe.ingredients.count { it.isSelectedForShopping }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = recipe.sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // Résumé Possédés vs Manquants
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✅ $ownedCount en stock",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Décochés par défaut",
                                        fontSize = 10.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🚨 $missingCount manquant(s)",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF991B1B),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Pré-cochés pour achat",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Validez les ingrédients à ajouter à votre liste de courses ($selectedCount sélectionné(s)) :",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        recipe.ingredients.forEachIndexed { idx, ing ->
                            Surface(
                                color = if (ing.isSelectedForShopping) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.toggleRecipeIngredientSelection(idx) }
                                    ) {
                                        Checkbox(
                                            checked = ing.isSelectedForShopping,
                                            onCheckedChange = { viewModel.toggleRecipeIngredientSelection(idx) }
                                        )
                                        Column {
                                            Text(
                                                text = ing.ingredientName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val statusBadge = if (!ing.isMissing) {
                                                "✅ En stock (${ing.ownedQuantity} ${ing.unit}) • Requis : ${ing.requiredAmountText}"
                                            } else {
                                                "🚨 Manquant (0 en stock) • Requis : ${ing.requiredAmountText}"
                                            }
                                            Text(
                                                text = statusBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (!ing.isMissing) Color(0xFF059669) else Color(0xFFDC2626),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        TextButton(onClick = { viewModel.updateRecipeIngredientQuantity(idx, -1) }) {
                                            Text("-", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text(
                                            text = "${ing.suggestedQuantityToBuy}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { viewModel.updateRecipeIngredientQuantity(idx, 1) }) {
                                            Text("+", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addSelectedRecipeIngredientsToShoppingList(storeTypes) },
                                enabled = selectedCount > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_recipe_to_shopping_btn")
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter ($selectedCount) aux courses")
                            }
                            TextButton(onClick = { viewModel.clearRecipeResult() }) {
                                Text("Fermer")
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // MODE LECTURE DE TICKET DE CAISSE NUMÉRIQUE ([BACKLOG-RECEIPT-08])
            // =========================================================================
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Lecteur de Ticket Numérique",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Importez un fichier de ticket de caisse numérique (.txt, .html, .eml) ou collez son contenu ci-dessous pour identifier les articles achetés et mettre à jour votre stock automatiquement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                receiptFilePicker.launch(arrayOf("text/*", "message/rfc822", "application/octet-stream"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📂 Fichier ticket")
                        }
                        FilledTonalButton(
                            onClick = {
                                digitalReceiptInput = ReceiptParser.SAMPLE_DIGITAL_RECEIPT.trimIndent()
                                viewModel.analyzeDigitalReceipt(digitalReceiptInput)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✨ Exemple ticket")
                        }
                    }

                    OutlinedTextField(
                        value = digitalReceiptInput,
                        onValueChange = { digitalReceiptInput = it },
                        label = { Text("Contenu du ticket numérique (e-mail, texte, OCR)") },
                        placeholder = { Text("Collez ici les lignes de votre ticket de caisse...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("digital_receipt_input")
                    )

                    Button(
                        onClick = { viewModel.analyzeDigitalReceipt(digitalReceiptInput) },
                        enabled = digitalReceiptInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_receipt_btn")
                    ) {
                        Text("🔍 Analyser le ticket et proposer la mise à jour")
                    }
                }
            }

            if (uiState.receiptLines.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Produits détectés sur le ticket (${uiState.receiptLines.count { it.isSelected }}/${uiState.receiptLines.size} sélectionnés)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        uiState.receiptLines.forEachIndexed { index, line ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = line.isSelected,
                                            onCheckedChange = { viewModel.toggleReceiptLineSelection(index) }
                                        )
                                        Column {
                                            Text(
                                                text = line.productName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val badgeText = if (line.matchedExistingItem != null) {
                                                "🔄 En stock (${line.matchedExistingItem.quantity}) → +${line.quantity}"
                                            } else {
                                                "✨ Nouvel article (${line.suggestedCategory})"
                                            }
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (line.matchedExistingItem != null) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.secondary
                                                }
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(onClick = { viewModel.updateReceiptLineQuantity(index, -1) }) {
                                            Text("-", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text(
                                            text = "${line.quantity}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { viewModel.updateReceiptLineQuantity(index, 1) }) {
                                            Text("+", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.applyReceiptToInventory(storeTypes) },
                                enabled = uiState.receiptLines.any { it.isSelected },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("✓ Mettre à jour l'inventaire")
                            }
                            TextButton(onClick = { viewModel.clearReceipt() }) {
                                Text("Annuler")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InStoreScanResultSection(
    uiState: ScannerUiState,
    categories: List<CategoryEntity>,
    storeTypes: List<StoreTypeEntity>,
    onIncrementExisting: (InventoryItemEntity, Int) -> Unit,
    onUndoExpress: () -> Unit,
    onAddToCart: () -> Unit,
    onAddNewToInventory: (name: String, categoryId: String, quantity: Int, unit: String, storeId: String, location: String, notes: String) -> Unit,
    onReset: () -> Unit
) {
    val existing = uiState.existingItem
    val online = uiState.onlineProduct
    val shopMatch = uiState.matchingShoppingItem
    val behavior = uiState.scanBehavior

    // Option 1 (Anti-Doublon Bullix/Grocy) ou Mode 5 (Tout-en-un)
    if (behavior == InStoreScanBehavior.CHECK_OWNED || behavior == InStoreScanBehavior.ALL_IN_ONE) {
        if (existing != null) {
            val isOut = existing.quantity <= 0
            val isLow = existing.quantity in 1..existing.minThreshold
            val containerBg = when {
                isOut -> Color(0xFFDC2626)
                isLow -> Color(0xFFD97706)
                else -> Color(0xFF059669)
            }
            val headerText = when {
                isOut -> "🚨 RUPTURE DE STOCK (0 CHEZ VOUS)"
                isLow -> "⚠️ STOCK BAS (${existing.quantity} ${existing.unit} RESTANT)"
                else -> "✅ DÉJÀ EN STOCK (${existing.quantity} ${existing.unit})"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = containerBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerText,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        if (!online?.nutriScore.isNullOrBlank()) {
                            NutriScorePill(online?.nutriScore!!)
                        }
                    }

                    Text(
                        text = existing.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = buildString {
                            append("📍 Rangé dans : ${existing.location}")
                            if (shopMatch != null) {
                                append(" • 🛒 Sur votre liste (${shopMatch.quantityNeeded} ${shopMatch.unit})")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )

                    if (!online?.allergens.isNullOrBlank()) {
                        Text(
                            text = "⚠️ Allergènes : ${online?.allergens}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onIncrementExisting(existing, 1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1 Stock Direct", fontWeight = FontWeight.ExtraBold)
                        }

                        if (behavior == InStoreScanBehavior.ALL_IN_ONE) {
                            FilledTonalButton(
                                onClick = onAddToCart,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🛒 Au Caddie", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            FilledTonalButton(onClick = { onIncrementExisting(existing, -1) }) {
                                Text("-1")
                            }
                        }
                    }
                }
            }
            return
        }
    }

    // Option 2 : Validation Express 1-Tap (Style Bring!)
    if (behavior == InStoreScanBehavior.INSTANT_RESTOCK && existing != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚡ VALIDATION EXPRESS 1-TAP • AUTO-COCHÉ",
                    color = Color(0xFF6EE7B7),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${existing.name} → Stock : ${existing.quantity} ${existing.unit}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (shopMatch != null) {
                    Text(
                        text = "✓ Rayé automatiquement de votre liste de courses !",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(onClick = { onIncrementExisting(existing, -1) }) {
                            Text("-1")
                        }
                        Button(onClick = { onIncrementExisting(existing, 1) }) {
                            Text("+1")
                        }
                    }
                    if (uiState.lastExpressUndo != null) {
                        OutlinedButton(
                            onClick = onUndoExpress,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                        ) {
                            Text("↩ Annuler")
                        }
                    }
                }
            }
        }
        return
    }

    // Option 4 (Fiche Nutri Open Food Facts) ou Produit non encore possédé
    if (online != null) {
        NewScannedProductForm(
            barcode = online.barcode,
            defaultName = online.name,
            suggestedCategory = online.suggestedCategory,
            suggestedUnit = online.suggestedUnit,
            suggestedNotes = online.suggestedNotes,
            sourceName = online.source,
            nutriScore = online.nutriScore,
            allergens = online.allergens,
            imageUrl = online.imageUrl,
            categories = categories,
            storeTypes = storeTypes,
            onAdd = onAddNewToInventory,
            onCancel = onReset
        )
    } else if (existing != null) {
        // Fallback standard pour un article existant
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(existing.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StockBadge(quantity = existing.quantity, minThreshold = existing.minThreshold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onIncrementExisting(existing, 1) }, modifier = Modifier.weight(1f)) {
                        Text("+1 au stock")
                    }
                    FilledTonalButton(onClick = onAddToCart) {
                        Text("🛒 Caddie")
                    }
                }
            }
        }
    }
}

@Composable
private fun InStoreCartDrawerCard(
    cartItems: List<com.example.ui.viewmodel.InStoreCartItem>,
    onUpdateQty: (String, Int) -> Unit,
    onCheckout: () -> Unit,
    onClear: () -> Unit
) {
    val totalUnits = cartItems.sumOf { it.quantity }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "🛒 Mon Caddie en cours ($totalUnits art.)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Vider")
                    }
                }
            }

            if (cartItems.isEmpty()) {
                Text(
                    text = "Scannez vos articles au fur et à mesure que vous les déposez dans votre chariot, puis validez tout votre caddie en 1 clic à la caisse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                cartItems.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "${item.location}${if (item.wasOnShoppingList) " • ✓ Sur la liste" else ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onUpdateQty(item.barcode, -1) }) {
                                    Text("-", fontWeight = FontWeight.ExtraBold)
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.ExtraBold)
                                TextButton(onClick = { onUpdateQty(item.barcode, 1) }) {
                                    Text("+", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_instore_cart_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Valider mon passage en caisse ($totalUnits art.)", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun NutriScorePill(nutriScore: String) {
    val grade = nutriScore.uppercase()
    val nutriColor = when (grade) {
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
            text = "Nutri-Score $grade",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (grade == "C") Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
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
                            text = "🔊 Code-barres lu • Source : $sourceName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (!nutriScore.isNullOrBlank()) {
                        NutriScorePill(nutriScore)
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
                        text = "Voulez-vous ajouter ce produit au stock ?",
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
                    Text("Scanner un autre")
                }
            }
        }
    }
}
