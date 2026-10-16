package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.billing.BillingRepository
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import com.example.ui.notification.NotificationHelper
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.InventoryViewModelFactory
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.ScannerViewModelFactory
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.ui.viewmodel.ShoppingViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

enum class AppScreen {
    INVENTORY,
    SHOPPING,
    SCANNER,
    SYNC,
    PAYWALL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize proactive notification channel
        NotificationHelper.createNotificationChannel(this)

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val inventoryDao = database.inventoryDao()
        val shoppingDao = database.shoppingDao()

        val inventoryRepository = InventoryRepository(inventoryDao, shoppingDao)
        val shoppingRepository = ShoppingRepository(shoppingDao, inventoryDao)

        val inventoryFactory = InventoryViewModelFactory(inventoryRepository)
        val shoppingFactory = ShoppingViewModelFactory(shoppingRepository, inventoryRepository)
        val scannerFactory = ScannerViewModelFactory(inventoryRepository)

        setContent {
            MyApplicationTheme {
                val inventoryViewModel: InventoryViewModel = viewModel(factory = inventoryFactory)
                val shoppingViewModel: ShoppingViewModel = viewModel(factory = shoppingFactory)
                val scannerViewModel: ScannerViewModel = viewModel(factory = scannerFactory)

                val inventoryUiState by inventoryViewModel.uiState.collectAsStateWithLifecycle()
                val shoppingUiState by shoppingViewModel.uiState.collectAsStateWithLifecycle()
                val scannerUiState by scannerViewModel.uiState.collectAsStateWithLifecycle()

                val billingManager by BillingRepository.activeManager.collectAsStateWithLifecycle()
                val hasActiveAccess by billingManager.hasActiveAccess.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(AppScreen.INVENTORY) }

                // Handle incoming Android Share Intent (Screenshot image/* or Recipe Link text/plain)
                LaunchedEffect(intent) {
                    if (intent?.action == Intent.ACTION_SEND) {
                        val mime = intent.type ?: ""
                        if (mime.startsWith("text/")) {
                            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                            if (!sharedText.isNullOrBlank()) {
                                currentScreen = AppScreen.SCANNER
                                scannerViewModel.setActiveTabMode("recipe")
                                val urlRegex = Regex("""https?://\S+""")
                                val foundUrl = urlRegex.find(sharedText)?.value
                                if (foundUrl != null) {
                                    scannerViewModel.analyzeRecipeUrl(foundUrl)
                                } else {
                                    scannerViewModel.analyzeRecipeTextOrOcr(
                                        rawText = sharedText,
                                        sourceLabel = "📲 Recette partagée vers Needix"
                                    )
                                }
                                intent.action = null
                            }
                        } else if (mime.startsWith("image/")) {
                            @Suppress("DEPRECATION")
                            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                            } else {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                            }
                            if (imageUri != null) {
                                currentScreen = AppScreen.SCANNER
                                scannerViewModel.setActiveTabMode("recipe")
                                try {
                                    val inputImg = InputImage.fromFilePath(this@MainActivity, imageUri)
                                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    recognizer.process(inputImg)
                                        .addOnSuccessListener { visionText ->
                                            if (visionText.text.isNotBlank()) {
                                                scannerViewModel.analyzeRecipeTextOrOcr(
                                                    rawText = visionText.text,
                                                    sourceLabel = "📸 Screenshot partagé (OCR ML Kit)"
                                                )
                                            }
                                            recognizer.close()
                                        }
                                        .addOnFailureListener { recognizer.close() }
                                } catch (_: Exception) {
                                }
                                intent.action = null
                            }
                        }
                    }
                }

                // Request POST_NOTIFICATIONS runtime permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Listen to proactive threshold alert events and send system notifications
                LaunchedEffect(inventoryUiState.notificationsEnabled) {
                    inventoryViewModel.thresholdAlertEvents.collect { alert ->
                        if (inventoryUiState.notificationsEnabled) {
                            val matchingItem = inventoryUiState.items.find { it.id == alert.itemId }
                            if (matchingItem != null) {
                                NotificationHelper.sendThresholdNotification(
                                    context = this@MainActivity,
                                    item = matchingItem,
                                    currentQuantity = alert.currentQuantity,
                                    minThreshold = alert.minThreshold,
                                    storeName = alert.storeName
                                )
                            }
                        }
                    }
                }

                // Listen to combined UI feedback events across ViewModels
                LaunchedEffect(Unit) {
                    merge(
                        inventoryViewModel.uiEvents,
                        shoppingViewModel.uiEvents,
                        scannerViewModel.uiEvents
                    ).collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                // Handle back button
                BackHandler(enabled = currentScreen != AppScreen.INVENTORY) {
                    currentScreen = when (currentScreen) {
                        AppScreen.PAYWALL -> AppScreen.SYNC
                        else -> AppScreen.INVENTORY
                    }
                }

                // Si accès bloqué, forcer l'affichage du Paywall
                if (!hasActiveAccess) {
                    PaywallScreen(
                        billingManager = billingManager,
                        isBlocking = true,
                        onBack = { /* bloquant */ }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (currentScreen != AppScreen.PAYWALL) {
                                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                                    // Tab 1: Inventaire
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.INVENTORY,
                                        onClick = { currentScreen = AppScreen.INVENTORY },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (inventoryUiState.activeThresholdAlerts.isNotEmpty()) {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = MaterialTheme.colorScheme.onError
                                                        ) {
                                                            Text(inventoryUiState.activeThresholdAlerts.size.toString(), fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (currentScreen == AppScreen.INVENTORY) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                                    contentDescription = stringResource(R.string.nav_inventory)
                                                )
                                            }
                                        },
                                        label = { Text(stringResource(R.string.nav_inventory)) },
                                        modifier = Modifier.testTag("nav_inventory")
                                    )

                                    // Tab 2: Courses
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SHOPPING,
                                        onClick = { currentScreen = AppScreen.SHOPPING },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (shoppingUiState.totalActiveCount > 0) {
                                                        Badge {
                                                            Text(shoppingUiState.totalActiveCount.toString(), fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (currentScreen == AppScreen.SHOPPING) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                                    contentDescription = stringResource(R.string.nav_shopping)
                                                )
                                            }
                                        },
                                        label = { Text(stringResource(R.string.nav_shopping)) },
                                        modifier = Modifier.testTag("nav_shopping")
                                    )

                                    // Tab 3: Scanner
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SCANNER,
                                        onClick = { currentScreen = AppScreen.SCANNER },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = stringResource(R.string.nav_scanner)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_scanner)) },
                                        modifier = Modifier.testTag("nav_scanner")
                                    )

                                    // Tab 4: Sauvegarde & Export
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SYNC,
                                        onClick = { currentScreen = AppScreen.SYNC },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == AppScreen.SYNC) Icons.Filled.Backup else Icons.Outlined.Backup,
                                                contentDescription = stringResource(R.string.nav_data_backup)
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_data_backup)) },
                                        modifier = Modifier.testTag("nav_sync")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.INVENTORY -> {
                                    InventoryScreen(
                                        viewModel = inventoryViewModel,
                                        uiState = inventoryUiState,
                                        onNavigateToScanner = { currentScreen = AppScreen.SCANNER },
                                        onTestNotification = {
                                            NotificationHelper.sendTestNotification(this@MainActivity)
                                        }
                                    )
                                }
                                AppScreen.SHOPPING -> {
                                    ShoppingScreen(
                                        viewModel = shoppingViewModel,
                                        uiState = shoppingUiState,
                                        onOpenStoreScanner = {
                                            scannerViewModel.setActiveTabMode("barcode")
                                            currentScreen = AppScreen.SCANNER
                                        },
                                        onOpenRecipeAssistant = {
                                            scannerViewModel.setActiveTabMode("recipe")
                                            currentScreen = AppScreen.SCANNER
                                        }
                                    )
                                }
                                AppScreen.SCANNER -> {
                                    ScannerScreen(
                                        viewModel = scannerViewModel,
                                        uiState = scannerUiState,
                                        categories = inventoryUiState.categories,
                                        storeTypes = inventoryUiState.storeTypes
                                    )
                                }
                                AppScreen.SYNC -> {
                                    SyncScreen(
                                        items = inventoryUiState.items,
                                        shoppingItems = shoppingUiState.activeItems + shoppingUiState.onHoldItems + shoppingUiState.purchasedItems,
                                        categories = inventoryUiState.categories,
                                        storeTypes = inventoryUiState.storeTypes,
                                        notificationsEnabled = inventoryUiState.notificationsEnabled,
                                        onToggleNotifications = { enabled ->
                                            inventoryViewModel.toggleNotifications(enabled)
                                        },
                                        onTestNotification = {
                                            NotificationHelper.sendTestNotification(this@MainActivity)
                                        },
                                        onNavigateToPaywall = { currentScreen = AppScreen.PAYWALL },
                                        onAddCategory = { name, emoji, color ->
                                            inventoryViewModel.addCategory(name, emoji, color)
                                        },
                                        onDeleteCategory = { category ->
                                            inventoryViewModel.deleteCategory(category)
                                        },
                                        onAddStoreType = { name, emoji, color ->
                                            inventoryViewModel.addStoreType(name, emoji, color)
                                        },
                                        onDeleteStoreType = { storeType ->
                                            inventoryViewModel.deleteStoreType(storeType)
                                        },
                                        onImportCsv = { uri, replaceAll ->
                                            inventoryViewModel.importCsv(this@MainActivity, uri, replaceAll) { count ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("$count articles importés !")
                                                }
                                            }
                                        }
                                    )
                                }
                                AppScreen.PAYWALL -> {
                                    PaywallScreen(
                                        billingManager = billingManager,
                                        isBlocking = false,
                                        onBack = { currentScreen = AppScreen.SYNC }
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
