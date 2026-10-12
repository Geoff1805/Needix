// Needix Web App Engine (File System Access API & Wasm/HTML5)
// Gestionnaire d'inventaire 100% local, interopérable avec Needix Android (JSON & CSV)

const CATEGORY_MAP = {
  cuisine: "Cuisine & Épices",
  aliments: "Aliments & Frais",
  menager: "Produits Ménagers",
  hygiene: "Hygiène & Soins",
  pharmacie: "Pharmacie & Santé",
  bricolage: "Bricolage & Maison",
  autre: "Divers"
};

const STORE_MAP = {
  supermarket: "Supermarché",
  pharmacy: "Pharmacie",
  online: "Internet / En ligne",
  bio: "Marché / Bio",
  brico: "Bricolage"
};

const DEFAULT_CATEGORIES = [
  { id: "cuisine", name: "Cuisine & Épices", iconEmoji: "🥫", colorHex: 0xFFF59E0B, orderIndex: 0 },
  { id: "aliments", name: "Aliments & Frais", iconEmoji: "🍎", colorHex: 0xFF10B981, orderIndex: 1 },
  { id: "menager", name: "Produits Ménagers", iconEmoji: "🧴", colorHex: 0xFF06B6D4, orderIndex: 2 },
  { id: "hygiene", name: "Hygiène & Soins", iconEmoji: "🧼", colorHex: 0xFF8B5CF6, orderIndex: 3 },
  { id: "pharmacie", name: "Pharmacie & Santé", iconEmoji: "💊", colorHex: 0xFFEF4444, orderIndex: 4 },
  { id: "bricolage", name: "Bricolage & Maison", iconEmoji: "🔨", colorHex: 0xFF64748B, orderIndex: 5 },
  { id: "autre", name: "Divers", iconEmoji: "📦", colorHex: 0xFF6B7280, orderIndex: 6 }
];

const DEFAULT_STORE_TYPES = [
  { id: "supermarket", name: "Supermarché", iconEmoji: "🛒", colorHex: 0xFF3B82F6, orderIndex: 0 },
  { id: "pharmacy", name: "Pharmacie", iconEmoji: "💊", colorHex: 0xFFEF4444, orderIndex: 1 },
  { id: "online", name: "Internet / En ligne", iconEmoji: "🌐", colorHex: 0xFF8B5CF6, orderIndex: 2 },
  { id: "bio", name: "Marché / Bio", iconEmoji: "🥦", colorHex: 0xFF10B981, orderIndex: 3 },
  { id: "brico", name: "Bricolage", iconEmoji: "🔨", colorHex: 0xFFF59E0B, orderIndex: 4 }
];

let inventoryData = {
  app: "Needix",
  appVersion: "1.0.0",
  version: "1.0.0",
  exportDate: new Date().toISOString(),
  items: [],
  shoppingItems: [],
  shoppingList: [],
  categories: DEFAULT_CATEGORIES,
  storeTypes: DEFAULT_STORE_TYPES
};

let currentFileHandle = null;
let hasUnsavedChanges = false;
let activeTab = "inventory"; // "inventory" or "shopping"
let activeLocationFilter = "all";
let searchQuery = "";
let editingItemId = null;

function resolveCategoryId(catOrId) {
  if (!catOrId) return "autre";
  const raw = String(catOrId).trim();
  if (CATEGORY_MAP[raw]) return raw;
  const lower = raw.toLowerCase();
  if (lower.includes("cuisine") || lower.includes("condiment") || lower.includes("boisson")) return "cuisine";
  if (lower.includes("aliment") || lower.includes("lait") || lower.includes("fruit") || lower.includes("viande") || lower.includes("épicerie")) return "aliments";
  if (lower.includes("ménag") || lower.includes("menag") || lower.includes("entretien")) return "menager";
  if (lower.includes("hygi")) return "hygiene";
  if (lower.includes("pharma") || lower.includes("sant")) return "pharmacie";
  if (lower.includes("brico")) return "bricolage";
  return "autre";
}

function resolveCategoryLabel(catIdOrName) {
  if (!catIdOrName) return "Divers";
  const id = resolveCategoryId(catIdOrName);
  return CATEGORY_MAP[id] || String(catIdOrName);
}

function resolveStoreId(storeOrId) {
  if (!storeOrId) return "supermarket";
  const raw = String(storeOrId).trim();
  if (STORE_MAP[raw]) return raw;
  const lower = raw.toLowerCase();
  if (lower.includes("pharma")) return "pharmacy";
  if (lower.includes("ligne") || lower.includes("internet") || lower.includes("web")) return "online";
  if (lower.includes("bio") || lower.includes("marché") || lower.includes("marche")) return "bio";
  if (lower.includes("brico")) return "brico";
  return raw;
}

function resolveStoreLabel(storeIdOrName) {
  if (!storeIdOrName) return "Supermarché";
  return STORE_MAP[storeIdOrName] || String(storeIdOrName);
}

function normalizeItem(it, idx) {
  const categoryId = resolveCategoryId(it.categoryId || it.category);
  const category = CATEGORY_MAP[categoryId] || it.category || "Divers";
  const quantity = Math.max(0, Math.round(Number(it.quantity ?? 1)));
  const minThreshold = Math.max(0, Math.round(Number(it.minThreshold ?? it.minQuantity ?? 1)));
  const preferredStoreId = resolveStoreId(it.preferredStoreId || it.store || "supermarket");
  const store = resolveStoreLabel(it.store || preferredStoreId);

  return {
    id: Number(it.id) || (idx + 1),
    name: String(it.name || "").trim(),
    categoryId,
    category,
    quantity,
    minThreshold,
    minQuantity: minThreshold,
    unit: String(it.unit || "pièce").trim(),
    preferredStoreId,
    store,
    barcode: it.barcode ? String(it.barcode).trim() : null,
    location: String(it.location || "Placard").trim(),
    expirationDate: it.expirationDate || "",
    notes: String(it.notes || "").trim(),
    updatedAt: Number(it.updatedAt) || Date.now()
  };
}

function normalizeShoppingItem(sh, idx) {
  const storeId = resolveStoreId(sh.storeId || sh.store || "supermarket");
  const quantityNeeded = Math.max(1, Math.round(Number(sh.quantityNeeded ?? sh.quantity ?? 1)));
  const isPurchased = Boolean(sh.isPurchased ?? sh.checked ?? false);
  return {
    id: Number(sh.id) || (idx + 1),
    inventoryItemId: sh.inventoryItemId != null ? Number(sh.inventoryItemId) : null,
    name: String(sh.name || "").trim(),
    categoryId: resolveCategoryId(sh.categoryId || sh.category || "autre"),
    storeId,
    store: resolveStoreLabel(sh.store || storeId),
    quantityNeeded,
    quantity: quantityNeeded,
    unit: String(sh.unit || "pièce").trim(),
    isPurchased,
    checked: isPurchased,
    isOnHold: Boolean(sh.isOnHold ?? false),
    barcode: sh.barcode ? String(sh.barcode).trim() : null,
    addedAt: Number(sh.addedAt) || Date.now()
  };
}

function normalizeBackupObject(raw) {
  const base = Array.isArray(raw) ? { items: raw } : (raw || {});
  const items = (Array.isArray(base.items) ? base.items : []).map((it, idx) => normalizeItem(it, idx));
  const rawShop = Array.isArray(base.shoppingItems)
    ? base.shoppingItems
    : (Array.isArray(base.shoppingList) ? base.shoppingList : []);
  const shoppingItems = rawShop.map((sh, idx) => normalizeShoppingItem(sh, idx));

  return {
    app: "Needix",
    appVersion: base.appVersion || base.version || "1.0.0",
    version: base.version || base.appVersion || "1.0.0",
    exportDate: base.exportDate || new Date().toISOString(),
    items,
    shoppingItems,
    shoppingList: shoppingItems,
    categories: Array.isArray(base.categories) && base.categories.length > 0 ? base.categories : DEFAULT_CATEGORIES,
    storeTypes: Array.isArray(base.storeTypes) && base.storeTypes.length > 0 ? base.storeTypes : DEFAULT_STORE_TYPES
  };
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
  initEventListeners();
  loadInitialState();
});

function initEventListeners() {
  const tabInv = document.getElementById("tab-btn-inv");
  const tabShop = document.getElementById("tab-btn-shop");
  if (tabInv && tabShop) {
    tabInv.addEventListener("click", () => switchTab("inventory"));
    tabShop.addEventListener("click", () => switchTab("shopping"));
  }

  const btnOpen = document.getElementById("btn-open-file");
  const btnSave = document.getElementById("btn-save-file");
  const btnDownload = document.getElementById("btn-download-json");
  const btnExportCsv = document.getElementById("btn-export-csv");
  const btnSample = document.getElementById("btn-load-sample");
  const btnAdd = document.getElementById("btn-add-item");
  const searchInput = document.getElementById("search-input");
  const locationFilter = document.getElementById("location-filter");

  if (btnOpen) btnOpen.addEventListener("click", handleOpenFile);
  if (btnSave) btnSave.addEventListener("click", handleSaveFile);
  if (btnDownload) btnDownload.addEventListener("click", handleDownloadJson);
  if (btnExportCsv) btnExportCsv.addEventListener("click", handleExportCsv);
  if (btnSample) btnSample.addEventListener("click", loadSampleData);
  if (btnAdd) btnAdd.addEventListener("click", openAddModal);

  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      searchQuery = e.target.value.toLowerCase();
      renderInventory();
    });
  }

  if (locationFilter) {
    locationFilter.addEventListener("change", (e) => {
      activeLocationFilter = e.target.value;
      renderInventory();
    });
  }

  const modalForm = document.getElementById("item-modal-form");
  const modalCancel = document.getElementById("modal-cancel-btn");
  const modalClose = document.getElementById("modal-close-btn");
  const btnLookupBarcode = document.getElementById("btn-lookup-barcode");
  if (modalForm) modalForm.addEventListener("submit", handleSaveItemForm);
  if (modalCancel) modalCancel.addEventListener("click", closeModal);
  if (modalClose) modalClose.addEventListener("click", closeModal);
  if (btnLookupBarcode) btnLookupBarcode.addEventListener("click", handleWebBarcodeLookup);

  const fallbackInput = document.getElementById("fallback-file-input");
  if (fallbackInput) {
    fallbackInput.addEventListener("change", handleFallbackFileInput);
  }

  const btnReceipt = document.getElementById("btn-receipt-scan");
  const btnReceiptClose = document.getElementById("receipt-modal-close");
  const btnReceiptCancel = document.getElementById("receipt-cancel-btn");
  const btnReceiptFile = document.getElementById("btn-load-receipt-file");
  const receiptFileInput = document.getElementById("receipt-file-input");
  const btnReceiptSample = document.getElementById("btn-load-receipt-sample");
  const btnParseReceipt = document.getElementById("btn-parse-receipt");
  const btnReceiptApply = document.getElementById("receipt-apply-btn");

  if (btnReceipt) btnReceipt.addEventListener("click", openReceiptModal);
  if (btnReceiptClose) btnReceiptClose.addEventListener("click", closeReceiptModal);
  if (btnReceiptCancel) btnReceiptCancel.addEventListener("click", closeReceiptModal);
  if (btnReceiptFile && receiptFileInput) {
    btnReceiptFile.addEventListener("click", () => receiptFileInput.click());
    receiptFileInput.addEventListener("change", handleReceiptFileInput);
  }
  if (btnReceiptSample) btnReceiptSample.addEventListener("click", handleReceiptSample);
  if (btnParseReceipt) btnParseReceipt.addEventListener("click", handleAnalyzeReceipt);
  if (btnReceiptApply) btnReceiptApply.addEventListener("click", handleApplyReceipt);

  const btnManageCat = document.getElementById("btn-manage-categories");
  const btnCatClose = document.getElementById("categories-modal-close");
  const btnCatCancel = document.getElementById("categories-modal-cancel");
  const tabCatView = document.getElementById("tab-cat-view");
  const tabStoreView = document.getElementById("tab-store-view");
  const btnAddCatWeb = document.getElementById("btn-add-cat-web");
  const btnAddStoreWeb = document.getElementById("btn-add-store-web");

  if (btnManageCat) btnManageCat.addEventListener("click", openCategoriesModal);
  if (btnCatClose) btnCatClose.addEventListener("click", closeCategoriesModal);
  if (btnCatCancel) btnCatCancel.addEventListener("click", closeCategoriesModal);
  if (tabCatView) tabCatView.addEventListener("click", () => switchManageTab("categories"));
  if (tabStoreView) tabStoreView.addEventListener("click", () => switchManageTab("stores"));
  if (btnAddCatWeb) btnAddCatWeb.addEventListener("click", handleAddCategoryWeb);
  if (btnAddStoreWeb) btnAddStoreWeb.addEventListener("click", handleAddStoreWeb);
}

async function loadInitialState() {
  const cached = localStorage.getItem("needix_cached_inventory");
  if (cached) {
    try {
      inventoryData = normalizeBackupObject(JSON.parse(cached));
    } catch (e) {
      console.warn("Could not parse cached inventory", e);
    }
  }

  if (!inventoryData.items || inventoryData.items.length === 0) {
    await loadSampleData();
  } else {
    renderWebApp();
  }
}

// -------------------------------------------------------------
// File System Access API & CSV/JSON Interoperability
// -------------------------------------------------------------

function parseCsvToItems(csvText) {
  const cleanText = csvText.replace(/^\uFEFF/, "");
  const lines = cleanText.split(/\r?\n/).filter(l => l.trim().length > 0);
  if (lines.length <= 1) return [];

  const headerLine = lines[0];
  const delimiter = headerLine.includes(";") ? ";" : (headerLine.includes("\t") ? "\t" : ",");
  const headers = parseCsvRow(headerLine, delimiter).map(h => h.trim().toLowerCase());
  const isLegacyWebHeader = headers[3] && headers[3].includes("emplacement");

  const parsedItems = [];
  for (let i = 1; i < lines.length; i++) {
    const tokens = parseCsvRow(lines[i], delimiter);
    if (tokens.length < 2) continue;
    const id = parseInt(tokens[0], 10) || i;
    const name = (tokens[1] || "").trim();
    if (!name) continue;

    if (isLegacyWebHeader) {
      // Legacy 11-col: ID;Nom;Catégorie;Emplacement;Quantité;Unité;Seuil_Alerte;Date_Péremption;Magasin;Code_Barres;Notes
      parsedItems.push(normalizeItem({
        id,
        name,
        category: tokens[2] || "autre",
        location: tokens[3] || "Placard",
        quantity: parseFloat(tokens[4]) || 0,
        unit: tokens[5] || "pièce",
        minThreshold: parseFloat(tokens[6]) || 1,
        expirationDate: tokens[7] || "",
        store: tokens[8] || "supermarket",
        barcode: tokens[9] || null,
        notes: tokens[10] || ""
      }, i - 1));
    } else {
      // Standard 10-col (Android & Web unified): ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes
      parsedItems.push(normalizeItem({
        id,
        name,
        categoryId: tokens[2] || "autre",
        quantity: parseFloat(tokens[3]) || 0,
        minThreshold: parseFloat(tokens[4]) || 1,
        unit: tokens[5] || "pièce",
        preferredStoreId: tokens[6] || "supermarket",
        location: tokens[7] || "Placard",
        barcode: tokens[8] || null,
        notes: tokens[9] || ""
      }, i - 1));
    }
  }
  return parsedItems;
}

function parseCsvRow(line, delimiter) {
  const result = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (c === delimiter && !inQuotes) {
      result.push(current);
      current = "";
    } else {
      current += c;
    }
  }
  result.push(current);
  return result;
}

function loadContentFromString(text, fileName, handle = null) {
  const trimmed = text.replace(/^\uFEFF/, "").trim();
  if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
    const parsed = JSON.parse(trimmed);
    if (Array.isArray(parsed.items) || Array.isArray(parsed)) {
      inventoryData = normalizeBackupObject(parsed);
    } else {
      throw new Error("Format JSON invalide. Structure attendue : { items: [...] }");
    }
  } else {
    const items = parseCsvToItems(trimmed);
    if (items.length === 0) {
      throw new Error("Aucun article valide trouvé dans le fichier CSV.");
    }
    inventoryData = normalizeBackupObject({ items });
  }

  currentFileHandle = handle;
  hasUnsavedChanges = false;
  updateFileStatusBadge(fileName);
  persistLocalBackup();
  renderWebApp();
  showToast(t("file_connected") + " " + fileName, "success");
}

async function handleOpenFile() {
  if ("showOpenFilePicker" in window) {
    try {
      const [handle] = await window.showOpenFilePicker({
        types: [{
          description: "Fichier Inventaire Needix (*.json, *.csv)",
          accept: {
            "application/json": [".json"],
            "text/csv": [".csv"]
          }
        }],
        multiple: false
      });
      const file = await handle.getFile();
      const text = await file.text();
      loadContentFromString(text, file.name, file.name.endsWith(".json") ? handle : null);
    } catch (err) {
      if (err.name !== "AbortError") {
        console.error("Erreur ouverture fichier:", err);
        showToast("Impossible d'ouvrir le fichier : " + err.message, "error");
      }
    }
  } else {
    const fallbackInput = document.getElementById("fallback-file-input");
    if (fallbackInput) fallbackInput.click();
  }
}

async function handleFallbackFileInput(e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (event) => {
    try {
      loadContentFromString(event.target.result, file.name + " (Navigateur)", null);
    } catch (err) {
      showToast("Erreur de lecture du fichier : " + err.message, "error");
    }
  };
  reader.readAsText(file);
}

function prepareExportPayload() {
  inventoryData.exportDate = new Date().toISOString().replace("T", " ").slice(0, 19);
  inventoryData.appVersion = "1.0.0";
  inventoryData.items = (inventoryData.items || []).map((it, idx) => normalizeItem(it, idx));
  inventoryData.shoppingItems = (inventoryData.shoppingItems || inventoryData.shoppingList || []).map((sh, idx) => normalizeShoppingItem(sh, idx));
  inventoryData.shoppingList = inventoryData.shoppingItems;
  return JSON.stringify(inventoryData, null, 2);
}

async function handleSaveFile() {
  const content = prepareExportPayload();

  if (currentFileHandle && "createWritable" in currentFileHandle) {
    try {
      const writable = await currentFileHandle.createWritable();
      await writable.write(content);
      await writable.close();
      hasUnsavedChanges = false;
      persistLocalBackup();
      updateSaveStatus();
      showToast("Fichier sauvegardé avec succès sur votre disque !", "success");
      return;
    } catch (err) {
      console.warn("Impossible d'écrire directement avec le handle existant:", err);
    }
  }

  if ("showSaveFilePicker" in window) {
    try {
      const handle = await window.showSaveFilePicker({
        suggestedName: "needix_backup.json",
        types: [{
          description: "Fichier Inventaire Needix (*.json)",
          accept: { "application/json": [".json"] }
        }]
      });
      const writable = await handle.createWritable();
      await writable.write(content);
      await writable.close();
      currentFileHandle = handle;
      hasUnsavedChanges = false;
      const file = await handle.getFile();
      updateFileStatusBadge(file.name);
      persistLocalBackup();
      updateSaveStatus();
      showToast("Inventaire enregistré dans : " + file.name, "success");
    } catch (err) {
      if (err.name !== "AbortError") {
        showToast("Erreur d'enregistrement : " + err.message, "error");
      }
    }
  } else {
    handleDownloadJson();
  }
}

function handleDownloadJson() {
  const content = prepareExportPayload();
  const blob = new Blob([content], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `needix_backup_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  hasUnsavedChanges = false;
  updateSaveStatus();
  showToast("Fichier JSON téléchargé !", "success");
}

function escapeCsvField(val) {
  const str = String(val ?? "");
  if (str.includes(",") || str.includes('"') || str.includes("\n") || str.includes("\r")) {
    return '"' + str.replace(/"/g, '""') + '"';
  }
  return str;
}

function handleExportCsv() {
  const items = inventoryData.items || [];
  if (items.length === 0) {
    showToast("Aucun article à exporter", "warning");
    return;
  }

  // Standard 10-column header shared with Android UniversalExportManager & SheetsCsvManager
  let csv = "ID,Nom,Categorie,Quantite,SeuilAlerte,Unite,MagasinPrefere,Emplacement,CodeBarre,Notes\n";
  items.forEach((raw, idx) => {
    const it = normalizeItem(raw, idx);
    const row = [
      escapeCsvField(it.id),
      escapeCsvField(it.name),
      escapeCsvField(it.categoryId),
      it.quantity,
      it.minThreshold,
      escapeCsvField(it.unit),
      escapeCsvField(it.preferredStoreId),
      escapeCsvField(it.location),
      escapeCsvField(it.barcode || ""),
      escapeCsvField(it.notes)
    ];
    csv += row.join(",") + "\n";
  });

  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `needix_inventaire_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("Export CSV standard généré avec succès !", "success");
}

async function loadSampleData() {
  try {
    const res = await fetch("/sample-inventory.json");
    if (res.ok) {
      const data = await res.json();
      inventoryData = normalizeBackupObject(data);
    } else {
      throw new Error("Local sample json not reachable");
    }
  } catch (e) {
    inventoryData = normalizeBackupObject({
      items: [
        { id: 1, name: "Lait demi-écrémé Bio", categoryId: "aliments", location: "Frigo", quantity: 2, unit: "L", minThreshold: 2, expirationDate: "2026-10-18", barcode: "3250390112345", preferredStoreId: "bio", notes: "Pack 1L" },
        { id: 2, name: "Yaourt nature fermier", categoryId: "aliments", location: "Frigo", quantity: 4, unit: "pots", minThreshold: 4, expirationDate: "2026-10-13", barcode: "3155250004321", preferredStoreId: "supermarket", notes: "Péremption imminente !" },
        { id: 3, name: "Pâtes Penne Rigate", categoryId: "aliments", location: "Placard", quantity: 3, unit: "paquets", minThreshold: 2, expirationDate: "2027-04-20", barcode: "8001250123456", preferredStoreId: "supermarket", notes: "500g" },
        { id: 4, name: "Huile d'olive extra", categoryId: "cuisine", location: "Placard", quantity: 1, unit: "L", minThreshold: 1, expirationDate: "2027-08-15", barcode: "8410250987654", preferredStoreId: "bio", notes: "Première pression" },
        { id: 5, name: "Steaks hachés pur bœuf", categoryId: "aliments", location: "Congélateur", quantity: 6, unit: "pièces", minThreshold: 4, expirationDate: "2026-12-30", barcode: "3560070119988", preferredStoreId: "supermarket", notes: "15% MG" },
        { id: 6, name: "Café moulu arabica", categoryId: "cuisine", location: "Placard", quantity: 0, unit: "paquet", minThreshold: 1, expirationDate: "2027-01-10", barcode: "3017760000123", preferredStoreId: "supermarket", notes: "En rupture" }
      ],
      shoppingItems: [
        { id: 1, inventoryItemId: 6, name: "Café moulu arabica", categoryId: "cuisine", storeId: "supermarket", quantityNeeded: 2, unit: "paquets", isPurchased: false }
      ]
    });
  }
  currentFileHandle = null;
  hasUnsavedChanges = false;
  updateFileStatusBadge("Exemple chargé (Mode Démo)");
  persistLocalBackup();
  renderWebApp();
  showToast("Exemple d'inventaire chargé !", "info");
}

function persistLocalBackup() {
  try {
    localStorage.setItem("needix_cached_inventory", JSON.stringify(inventoryData));
  } catch (e) {
    console.warn("localStorage quota exceeded", e);
  }
}

// -------------------------------------------------------------
// UI Rendering
// -------------------------------------------------------------

function renderWebApp() {
  updateStats();
  updateSaveStatus();
  if (activeTab === "inventory") {
    renderInventory();
  } else {
    renderShoppingList();
  }
}
window.renderWebApp = renderWebApp;

function updateStats() {
  const items = inventoryData.items || [];
  const total = items.length;
  let lowStock = 0;
  let expiring = 0;
  const now = new Date();

  items.forEach(it => {
    const min = it.minThreshold ?? it.minQuantity ?? 1;
    if (it.quantity <= min) lowStock++;
    if (it.expirationDate) {
      const exp = new Date(it.expirationDate);
      const diffDays = Math.ceil((exp - now) / (1000 * 60 * 60 * 24));
      if (diffDays <= 3) expiring++;
    }
  });

  const shopItems = generateComputedShoppingList();

  const elTotal = document.getElementById("stat-total");
  const elLow = document.getElementById("stat-low");
  const elExp = document.getElementById("stat-exp");
  const elShop = document.getElementById("stat-shop");

  if (elTotal) elTotal.textContent = total;
  if (elLow) elLow.textContent = lowStock;
  if (elExp) elExp.textContent = expiring;
  if (elShop) elShop.textContent = shopItems.length;
}

function updateFileStatusBadge(fileName) {
  const badge = document.getElementById("file-status-badge");
  if (badge) {
    badge.innerHTML = `<span class="status-dot online"></span> ${t("file_connected")} : <strong>${escapeHtml(fileName)}</strong>`;
  }
}

function updateSaveStatus() {
  const indicator = document.getElementById("save-indicator");
  if (!indicator) return;
  if (hasUnsavedChanges) {
    indicator.innerHTML = `<span class="badge warning">● Modifications non enregistrées</span>`;
  } else {
    indicator.innerHTML = `<span class="badge success">✓ Synchronisé</span>`;
  }
}

function switchTab(tab) {
  activeTab = tab;
  const tabInv = document.getElementById("tab-btn-inv");
  const tabShop = document.getElementById("tab-btn-shop");
  const viewInv = document.getElementById("view-inventory");
  const viewShop = document.getElementById("view-shopping");

  if (tab === "inventory") {
    if (tabInv) tabInv.classList.add("active");
    if (tabShop) tabShop.classList.remove("active");
    if (viewInv) viewInv.style.display = "block";
    if (viewShop) viewShop.style.display = "none";
    renderInventory();
  } else {
    if (tabInv) tabInv.classList.remove("active");
    if (tabShop) tabShop.classList.add("active");
    if (viewInv) viewInv.style.display = "none";
    if (viewShop) viewShop.style.display = "block";
    renderShoppingList();
  }
}

function renderInventory() {
  const container = document.getElementById("inventory-items-container");
  if (!container) return;

  const items = inventoryData.items || [];
  const filtered = items.filter(it => {
    if (activeLocationFilter !== "all" && it.location !== activeLocationFilter) {
      return false;
    }
    if (searchQuery) {
      const catLabel = resolveCategoryLabel(it.categoryId || it.category);
      const storeLabel = resolveStoreLabel(it.preferredStoreId || it.store);
      const matchName = (it.name || "").toLowerCase().includes(searchQuery);
      const matchCat = catLabel.toLowerCase().includes(searchQuery);
      const matchStore = storeLabel.toLowerCase().includes(searchQuery);
      const matchBarcode = (it.barcode || "").includes(searchQuery);
      return matchName || matchCat || matchStore || matchBarcode;
    }
    return true;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>Aucun article trouvé</h3>
        <p>Ajustez vos filtres de recherche ou cliquez sur "+ Ajouter un article".</p>
      </div>
    `;
    return;
  }

  const now = new Date();
  let html = `<div class="items-grid">`;

  filtered.forEach(it => {
    let expBadge = "";
    if (it.expirationDate) {
      const exp = new Date(it.expirationDate);
      const diffDays = Math.ceil((exp - now) / (1000 * 60 * 60 * 24));
      if (diffDays < 0) {
        expBadge = `<span class="badge danger">Périmé (${Math.abs(diffDays)}j)</span>`;
      } else if (diffDays <= 3) {
        expBadge = `<span class="badge danger">Périme dans ${diffDays}j</span>`;
      } else if (diffDays <= 7) {
        expBadge = `<span class="badge warning">${diffDays} jours</span>`;
      } else {
        expBadge = `<span class="badge neutral">${escapeHtml(it.expirationDate)}</span>`;
      }
    } else {
      expBadge = `<span class="badge muted">Non renseignée</span>`;
    }

    const min = it.minThreshold ?? it.minQuantity ?? 1;
    const isLowStock = it.quantity <= min;
    const stockClass = isLowStock ? "low-stock" : "";
    const catLabel = resolveCategoryLabel(it.categoryId || it.category);
    const storeLabel = resolveStoreLabel(it.store || it.preferredStoreId);

    html += `
      <div class="item-card ${stockClass}">
        <div class="card-header">
          <div>
            <span class="location-chip ${getLocationClass(it.location)}">${escapeHtml(it.location || "Maison")}</span>
            <span class="category-chip">${escapeHtml(catLabel)}</span>
          </div>
          ${expBadge}
        </div>
        <h4 class="item-title">${escapeHtml(it.name)}</h4>
        ${it.notes ? `<p class="item-notes">${escapeHtml(it.notes)}</p>` : ""}
        
        <div class="card-meta">
          ${storeLabel ? `<span class="meta-item">🏪 ${escapeHtml(storeLabel)}</span>` : ""}
          ${it.barcode ? `<span class="meta-item">🔍 ${escapeHtml(it.barcode)}</span>` : ""}
        </div>

        <div class="quantity-controller">
          <button class="qty-btn" onclick="updateItemQuantity(${it.id}, -1)" title="Diminuer">-</button>
          <div class="qty-display">
            <span class="qty-num">${it.quantity}</span>
            <span class="qty-unit">${escapeHtml(it.unit || "pièce")}</span>
          </div>
          <button class="qty-btn" onclick="updateItemQuantity(${it.id}, 1)" title="Augmenter">+</button>
        </div>

        <div class="card-footer">
          <span class="min-threshold">Seuil alerte : ${min}</span>
          <div class="action-buttons">
            <button class="icon-btn edit-btn" onclick="openEditModal(${it.id})" title="Modifier">✏️</button>
            <button class="icon-btn delete-btn" onclick="deleteItem(${it.id})" title="Supprimer">🗑️</button>
          </div>
        </div>
      </div>
    `;
  });

  html += `</div>`;
  container.innerHTML = html;
}

function getLocationClass(loc) {
  switch (loc) {
    case "Frigo": return "loc-fridge";
    case "Placard": return "loc-cupboard";
    case "Congélateur": return "loc-freezer";
    case "Cave": return "loc-cellar";
    case "Salle de bain": return "loc-bathroom";
    default: return "loc-other";
  }
}

function generateComputedShoppingList() {
  const items = inventoryData.items || [];
  const needed = [];

  items.forEach(it => {
    const min = it.minThreshold ?? it.minQuantity ?? 1;
    if (it.quantity <= min) {
      const deficit = Math.max(1, min * 2 - it.quantity);
      const storeLabel = resolveStoreLabel(it.store || it.preferredStoreId);
      needed.push({
        id: it.id,
        inventoryItemId: it.id,
        name: it.name,
        categoryId: it.categoryId || "autre",
        storeId: it.preferredStoreId || "supermarket",
        store: storeLabel,
        quantityNeeded: deficit,
        quantity: deficit,
        unit: it.unit || "pièce",
        currentQty: it.quantity,
        minQty: min,
        isPurchased: false,
        checked: false
      });
    }
  });

  const savedList = inventoryData.shoppingItems || inventoryData.shoppingList || [];
  if (Array.isArray(savedList)) {
    savedList.forEach(custom => {
      if (!needed.some(n => n.name.toLowerCase() === (custom.name || "").toLowerCase())) {
        needed.push(normalizeShoppingItem(custom, needed.length));
      }
    });
  }

  return needed;
}

function renderShoppingList() {
  const container = document.getElementById("shopping-list-container");
  if (!container) return;

  const list = generateComputedShoppingList();

  if (list.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🎉</div>
        <h3>${t("shopping_empty")}</h3>
        <p>Vos réserves sont pleines. Dès qu'un produit passe sous son seuil d'alerte, il apparaîtra ici.</p>
      </div>
    `;
    return;
  }

  const grouped = {};
  list.forEach(it => {
    const s = resolveStoreLabel(it.store || it.storeId);
    if (!grouped[s]) grouped[s] = [];
    grouped[s].push(it);
  });

  let html = `<div class="shopping-groups">`;

  for (const [store, storeItems] of Object.entries(grouped)) {
    html += `
      <div class="store-group-card">
        <div class="store-group-header">
          <h4>🏪 ${escapeHtml(store)}</h4>
          <span class="badge neutral">${storeItems.length} article(s)</span>
        </div>
        <ul class="shopping-items-list">
    `;

    storeItems.forEach(it => {
      const qty = it.quantityNeeded ?? it.quantity ?? 1;
      const isChecked = it.isPurchased ?? it.checked ?? false;
      html += `
        <li class="shopping-item-row ${isChecked ? "checked" : ""}">
          <label class="checkbox-container">
            <input type="checkbox" ${isChecked ? "checked" : ""} onchange="toggleShoppingItem(${it.id})">
            <span class="checkmark"></span>
            <span class="shop-name">${escapeHtml(it.name)}</span>
          </label>
          <div class="shop-qty-badge">
            À acheter : <strong>${qty} ${escapeHtml(it.unit || "")}</strong>
            ${it.currentQty !== undefined ? `<small class="text-muted">(en stock: ${it.currentQty})</small>` : ""}
          </div>
          <button class="btn-restock" onclick="quickRestock(${it.inventoryItemId || it.id}, ${qty})" title="Marquer comme acheté et réapprovisionner l'inventaire">
            + Restocker
          </button>
        </li>
      `;
    });

    html += `
        </ul>
      </div>
    `;
  }

  html += `</div>`;
  container.innerHTML = html;
}

// -------------------------------------------------------------
// Inventory Item Operations
// -------------------------------------------------------------

function updateItemQuantity(id, delta) {
  const it = inventoryData.items.find(x => x.id === id);
  if (!it) return;
  it.quantity = Math.max(0, Math.round((it.quantity || 0) + delta));
  it.updatedAt = Date.now();
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderWebApp();
}

function quickRestock(id, qtyToAdd) {
  const it = inventoryData.items.find(x => x.id === id);
  if (it) {
    it.quantity = Math.round((it.quantity || 0) + (qtyToAdd || 1));
    it.updatedAt = Date.now();
    hasUnsavedChanges = true;
    persistLocalBackup();
    renderWebApp();
    showToast(`Réapprovisionné : +${qtyToAdd} ${it.name}`, "success");
  }
}

function toggleShoppingItem(id) {
  const list = inventoryData.shoppingItems || inventoryData.shoppingList || [];
  const custom = list.find(x => x.id === id);
  if (custom) {
    custom.isPurchased = !custom.isPurchased;
    custom.checked = custom.isPurchased;
    hasUnsavedChanges = true;
    persistLocalBackup();
    renderShoppingList();
  }
}

function deleteItem(id) {
  if (!confirm("Voulez-vous vraiment supprimer cet article de l'inventaire ?")) return;
  inventoryData.items = (inventoryData.items || []).filter(x => x.id !== id);
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderWebApp();
  showToast("Article supprimé de l'inventaire", "info");
}

function populateCategorySelect() {
  const sel = document.getElementById("item-category");
  if (!sel) return;
  const currentVal = sel.value;
  sel.innerHTML = "";
  const cats = inventoryData.categories || DEFAULT_CATEGORIES;
  cats.forEach(c => {
    const opt = document.createElement("option");
    opt.value = c.id;
    opt.textContent = `${c.iconEmoji || "📦"} ${c.name}`;
    sel.appendChild(opt);
  });
  if (currentVal && cats.some(c => c.id === currentVal)) {
    sel.value = currentVal;
  } else if (cats.length > 0) {
    sel.value = cats[0].id;
  }
}

function openAddModal() {
  editingItemId = null;
  document.getElementById("modal-title").textContent = t("add_modal_title");
  document.getElementById("modal-item-id").value = "";
  document.getElementById("item-name").value = "";
  populateCategorySelect();
  document.getElementById("item-location").value = "Placard";
  document.getElementById("item-quantity").value = "1";
  document.getElementById("item-min-quantity").value = "1";
  document.getElementById("item-unit").value = "pièce";
  document.getElementById("item-expiry").value = "";
  document.getElementById("item-store").value = "Supermarché";
  document.getElementById("item-barcode").value = "";
  document.getElementById("item-notes").value = "";

  document.getElementById("item-modal").style.display = "flex";
}

function openEditModal(id) {
  const it = (inventoryData.items || []).find(x => x.id === id);
  if (!it) return;

  editingItemId = id;
  document.getElementById("modal-title").textContent = t("edit_modal_title");
  document.getElementById("modal-item-id").value = it.id;
  document.getElementById("item-name").value = it.name || "";
  populateCategorySelect();
  document.getElementById("item-category").value = resolveCategoryId(it.categoryId || it.category);
  document.getElementById("item-location").value = it.location || "Placard";
  document.getElementById("item-quantity").value = it.quantity ?? 1;
  document.getElementById("item-min-quantity").value = it.minThreshold ?? it.minQuantity ?? 1;
  document.getElementById("item-unit").value = it.unit || "pièce";
  document.getElementById("item-expiry").value = it.expirationDate || "";
  document.getElementById("item-store").value = resolveStoreLabel(it.store || it.preferredStoreId);
  document.getElementById("item-barcode").value = it.barcode || "";
  document.getElementById("item-notes").value = it.notes || "";

  document.getElementById("item-modal").style.display = "flex";
}

function closeModal() {
  document.getElementById("item-modal").style.display = "none";
}

function handleSaveItemForm(e) {
  e.preventDefault();
  const name = document.getElementById("item-name").value.trim();
  if (!name) {
    showToast("Le nom du produit est obligatoire", "error");
    return;
  }

  const categoryId = resolveCategoryId(document.getElementById("item-category").value);
  const category = resolveCategoryLabel(categoryId);
  const location = document.getElementById("item-location").value;
  const quantity = Math.max(0, Math.round(parseFloat(document.getElementById("item-quantity").value) || 0));
  const minThreshold = Math.max(0, Math.round(parseFloat(document.getElementById("item-min-quantity").value) || 1));
  const unit = document.getElementById("item-unit").value.trim() || "pièce";
  const expirationDate = document.getElementById("item-expiry").value || "";
  const rawStore = document.getElementById("item-store").value.trim() || "Supermarché";
  const preferredStoreId = resolveStoreId(rawStore);
  const store = resolveStoreLabel(rawStore);
  const barcode = document.getElementById("item-barcode").value.trim() || null;
  const notes = document.getElementById("item-notes").value.trim();

  if (editingItemId) {
    const it = inventoryData.items.find(x => x.id === editingItemId);
    if (it) {
      it.name = name;
      it.categoryId = categoryId;
      it.category = category;
      it.location = location;
      it.quantity = quantity;
      it.minThreshold = minThreshold;
      it.minQuantity = minThreshold;
      it.unit = unit;
      it.expirationDate = expirationDate;
      it.preferredStoreId = preferredStoreId;
      it.store = store;
      it.barcode = barcode;
      it.notes = notes;
      it.updatedAt = Date.now();
    }
  } else {
    const nextId = (inventoryData.items.length > 0) ? Math.max(...inventoryData.items.map(x => x.id || 0)) + 1 : 1;
    inventoryData.items.push({
      id: nextId,
      name,
      categoryId,
      category,
      location,
      quantity,
      minThreshold,
      minQuantity: minThreshold,
      unit,
      expirationDate,
      preferredStoreId,
      store,
      barcode,
      notes,
      updatedAt: Date.now()
    });
  }

  hasUnsavedChanges = true;
  persistLocalBackup();
  closeModal();
  renderWebApp();
  showToast("Article enregistré avec succès !", "success");
}

// -------------------------------------------------------------
// Multi-API Barcode Lookup & Offline Open-Product-Database
// -------------------------------------------------------------

let offlineProductCache = null;

async function getOfflineProductDb() {
  if (offlineProductCache) return offlineProductCache;
  try {
    const res = await fetch("/data/open-product-database.json");
    if (res.ok) {
      offlineProductCache = await res.json();
      return offlineProductCache;
    }
  } catch (e) {
    console.warn("Offline product DB fetch skipped", e);
  }
  offlineProductCache = [];
  return offlineProductCache;
}

function isIsbnCode(code) {
  return (code.length === 13 && (code.startsWith("978") || code.startsWith("979"))) ||
         (code.length === 10 && /^[0-9]{9}[0-9Xx]$/.test(code));
}

async function lookupOpenFactsEndpoint(baseUrl, barcode, sourceLabel, defaultCategory) {
  try {
    const fields = "product_name,product_name_fr,generic_name_fr,brands,quantity,categories_tags,nutriscore_grade,allergens_tags";
    const res = await fetch(`${baseUrl}/api/v2/product/${encodeURIComponent(barcode)}?fields=${fields}`);
    if (!res.ok) return null;
    const data = await res.json();
    if (!data || data.status !== 1 || !data.product) return null;
    const p = data.product;
    const rawName = (p.product_name_fr || p.product_name || p.generic_name_fr || "").trim();
    if (!rawName) return null;
    const brand = (p.brands || "").split(",")[0].trim();
    const fullName = brand && !rawName.toLowerCase().includes(brand.toLowerCase())
      ? `${rawName} (${brand})`
      : rawName;

    const tags = (p.categories_tags || []).join(" ").toLowerCase();
    let category = defaultCategory;
    if (sourceLabel === "Open Beauty Facts") {
      category = (tags.includes("pharma") || tags.includes("health")) ? "pharmacie" : "hygiene";
    } else if (sourceLabel === "Open Products Facts") {
      if (tags.includes("bricol") || tags.includes("tool") || tags.includes("hardware")) category = "bricolage";
      else if (tags.includes("hygiene") || tags.includes("beauty")) category = "hygiene";
      else category = "menager";
    } else {
      if (tags.includes("cleaning") || tags.includes("menage") || tags.includes("detergent")) category = "menager";
      else if (tags.includes("hygiene") || tags.includes("cosmetic") || tags.includes("beauty")) category = "hygiene";
      else if (tags.includes("pharma") || tags.includes("health")) category = "pharmacie";
      else if (tags.includes("oil") || tags.includes("spice") || tags.includes("condiment") || tags.includes("coffee")) category = "cuisine";
    }

    const noteParts = [];
    if (p.quantity) noteParts.push(p.quantity);
    if (p.nutriscore_grade && ["a", "b", "c", "d", "e"].includes(p.nutriscore_grade.toLowerCase())) {
      noteParts.push(`Nutri-Score ${p.nutriscore_grade.toUpperCase()}`);
    }
    if (Array.isArray(p.allergens_tags) && p.allergens_tags.length > 0) {
      const allergens = p.allergens_tags.map(a => a.split(":").pop().replace(/-/g, " ")).join(", ");
      noteParts.push(`Allergènes : ${allergens}`);
    }

    return {
      name: fullName,
      category,
      unit: p.quantity || "pièce",
      notes: noteParts.join(" • "),
      source: sourceLabel
    };
  } catch (e) {
    return null;
  }
}

async function lookupInventaireIsbn(barcode) {
  try {
    const res = await fetch(`https://api.inventaire.io/api/entities/by-uris?uris=isbn:${encodeURIComponent(barcode)}`);
    if (!res.ok) return null;
    const data = await res.json();
    const entities = data && data.entities ? data.entities : {};
    const entity = entities[`isbn:${barcode}`] || Object.values(entities)[0];
    if (!entity) return null;
    const claims = entity.claims || {};
    const labels = entity.labels || {};
    const title = (claims["wdt:P1476"] && claims["wdt:P1476"][0]) || labels.fr || labels.en || Object.values(labels)[0];
    if (!title) return null;
    const publisher = (claims["invp:P2"] && claims["invp:P2"][0]) || "";
    const fullName = publisher ? `${title} (${publisher})` : title;
    return {
      name: fullName,
      category: "autre",
      unit: "livre",
      notes: `ISBN ${barcode}${publisher ? " • " + publisher : ""}`,
      source: "Inventaire.io (ISBN)"
    };
  } catch (e) {
    return null;
  }
}

async function handleWebBarcodeLookup() {
  const barcodeInput = document.getElementById("item-barcode");
  if (!barcodeInput) return;
  const cleanCode = barcodeInput.value.trim().replace(/[-\s]/g, "");
  if (!cleanCode) {
    showToast("Veuillez saisir un code-barres EAN ou ISBN à identifier", "warning");
    return;
  }
  barcodeInput.value = cleanCode;
  showToast("Recherche dans les bases produits en cours...", "info");

  // 1. Offline Open-Product-Database
  const offlineList = await getOfflineProductDb();
  const offlineHit = offlineList.find(item => item.barcode === cleanCode);
  let result = null;

  if (offlineHit) {
    const fullName = offlineHit.brand && !offlineHit.name.toLowerCase().includes(offlineHit.brand.toLowerCase())
      ? `${offlineHit.name} (${offlineHit.brand})`
      : offlineHit.name;
    result = {
      name: fullName,
      category: offlineHit.category || "aliments",
      unit: offlineHit.unit || offlineHit.quantity || "pièce",
      notes: offlineHit.notes || "",
      source: "Open-Product-Database (Hors-ligne)"
    };
  }

  // 2. Inventaire.io if ISBN
  if (!result && isIsbnCode(cleanCode)) {
    result = await lookupInventaireIsbn(cleanCode);
  }

  // 3. Open Food Facts v2 (.net then .org)
  if (!result) {
    result = await lookupOpenFactsEndpoint("https://world.openfoodfacts.net", cleanCode, "Open Food Facts", "aliments") ||
             await lookupOpenFactsEndpoint("https://world.openfoodfacts.org", cleanCode, "Open Food Facts", "aliments");
  }

  // 4. Open Beauty Facts v2
  if (!result) {
    result = await lookupOpenFactsEndpoint("https://world.openbeautyfacts.org", cleanCode, "Open Beauty Facts", "hygiene");
  }

  // 5. Open Products Facts v2
  if (!result) {
    result = await lookupOpenFactsEndpoint("https://world.openproductsfacts.org", cleanCode, "Open Products Facts", "menager");
  }

  if (result) {
    playScannerBeep();
    document.getElementById("item-name").value = result.name;
    document.getElementById("item-category").value = result.category;
    document.getElementById("item-unit").value = result.unit;
    if (result.notes) document.getElementById("item-notes").value = result.notes;
    if (result.category === "hygiene") document.getElementById("item-location").value = "Salle de bain";
    else if (result.category === "aliments") document.getElementById("item-location").value = "Placard";
    showToast(`Identifié via ${result.source} : ${result.name}`, "success");
  } else {
    showToast("Produit non trouvé dans les bases mondiales. Complétez manuellement.", "warning");
  }
}

function playScannerBeep() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(880, audioCtx.currentTime); // 880Hz A5 beep
    gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.14);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.15);
  } catch (e) {
    // AudioContext blocked by browser autoplay policy
  }
}

// -------------------------------------------------------------
// Digital Receipt Parser & Stock Updater ([BACKLOG-RECEIPT-08])
// -------------------------------------------------------------

let currentReceiptLines = [];

const SAMPLE_WEB_RECEIPT = `SUPERMARCHÉ BIO & FRAIS
Ticket de caisse numérique - 11/10/2026 18:42
-----------------------------------------
2 x Lait demi-écrémé Bio 1L        2,70 €
Pâtes Penne Rigate 500g            1,45 €
Huile d'olive vierge extra 1L      8,90 €
Gel Douche Surgras Mixa 400ml      3,50 €
Lessive liquide Ariel 1.8L         9,90 €
Pommes Golden Bio 1kg              2,99 €
-----------------------------------------
TOTAL TTC                         29,44 €
CARTE BANCAIRE SANS CONTACT       29,44 €
TVA 5,5% : 1,12 € | TVA 20% : 2,23 €
Merci de votre visite !`;

function openReceiptModal() {
  const modal = document.getElementById("receipt-modal");
  if (modal) {
    modal.style.display = "flex";
    currentReceiptLines = [];
    document.getElementById("receipt-raw-text").value = "";
    document.getElementById("receipt-results-container").style.display = "none";
    document.getElementById("receipt-apply-btn").style.display = "none";
  }
}

function closeReceiptModal() {
  const modal = document.getElementById("receipt-modal");
  if (modal) modal.style.display = "none";
}

function handleReceiptSample() {
  document.getElementById("receipt-raw-text").value = SAMPLE_WEB_RECEIPT;
  handleAnalyzeReceipt();
}

function handleReceiptFileInput(e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (event) => {
    document.getElementById("receipt-raw-text").value = event.target.result;
    handleAnalyzeReceipt();
  };
  reader.readAsText(file);
}

function parseWebReceiptText(rawText) {
  if (!rawText || !rawText.trim()) return [];

  const ignored = [
    "total", "sous-total", "subtotal", "tva", "dont tva", "montant", "carte bancaire",
    "cb ", "especes", "espèces", "rendu", "monnaie", "ticket", "caisse", "caissier",
    "merci de votre visite", "a bientot", "à bientôt", "siret", "siren", " rcs ",
    "tel :", "tél :", "horaires", "facture", "paiement", "autorisation", " sans contact",
    "fidelite", "fidélité", "avantage", "remise", "reduction", "réduction", "bon d'achat",
    "solde", "points", "ouverture", "bienvenue", "http://", "https://", "www."
  ];

  const clean = rawText
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|tr|li|h[1-6])>/gi, "\n")
    .replace(/<[^>]+>/g, " ");

  const lines = clean.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
  const items = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const lower = line.toLowerCase();
    if (lower.length < 3 || ignored.some(k => lower.includes(k))) continue;
    if (/^[\s\-=_*#/0-9:.,€]+$/.test(lower)) continue;
    if (/^\d{2}\/\d{2}\/\d{2,4}/.test(lower)) continue;

    let qty = 1;
    let name = line;

    // "YAOURT 2 x 1,50 3,00 €"
    const inlineMatch = name.match(/^(.+?)\s+(\d{1,2})\s*[xX*]\s*\d+[.,]\d{2}.*$/);
    if (inlineMatch) {
      name = inlineMatch[1].trim();
      qty = parseInt(inlineMatch[2], 10) || 1;
    } else {
      // Trailing price "PATES 1,29 €"
      name = name.replace(/\s+-?\d{1,4}[.,]\d{2}\s*(?:€|EUR)?\s*[A-Z*]?\s*$/i, "").trim();
      // Leading qty "2 x LAIT"
      const leadMatch = name.match(/^(\d{1,2})\s*[xX*]\s+(.+)$/);
      if (leadMatch) {
        qty = parseInt(leadMatch[1], 10) || 1;
        name = leadMatch[2].trim();
      }
    }

    // Standalone multiplier on next line "  2 x 1,50 €"
    if (i + 1 < lines.length && /^\s*(\d{1,2})\s*[xX*]\s*\d+[.,]\d{2}/.test(lines[i + 1])) {
      const nextMatch = lines[i + 1].match(/^\s*(\d{1,2})\s*[xX*]/);
      if (nextMatch) {
        qty = parseInt(nextMatch[1], 10) || qty;
        i++;
      }
    }

    // Clean name
    name = name.replace(/^\d{6,14}\s+/, "").trim();
    if (name.length < 3 || ignored.some(k => name.toLowerCase().includes(k))) continue;

    if (name === name.toUpperCase()) {
      name = name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();
    }

    // Match with existing inventory
    const existing = findMatchingInventoryItem(name);
    const category = existing ? (existing.categoryId || "aliments") : guessWebCategory(name);
    const unit = existing ? (existing.unit || "pièce") : guessWebUnit(name);

    const dup = items.find(x => x.name.toLowerCase() === name.toLowerCase());
    if (dup) {
      dup.quantity += qty;
    } else {
      items.push({
        rawLine: line,
        name: existing ? existing.name : name,
        quantity: qty,
        unit,
        category,
        existingItem: existing,
        selected: true
      });
    }
  }

  return items;
}

function findMatchingInventoryItem(receiptName) {
  const norm = receiptName.toLowerCase().replace(/[^a-z0-9]/g, " ").trim();
  const list = inventoryData.items || [];
  return list.find(it => {
    const itemNorm = (it.name || "").toLowerCase().replace(/[^a-z0-9]/g, " ").trim();
    return itemNorm && (norm.includes(itemNorm) || itemNorm.includes(norm));
  }) || null;
}

function guessWebCategory(name) {
  const n = name.toLowerCase();
  if (/lessive|vaisselle|javel|nettoyant|eponge|essuie|poubelle/.test(n)) return "menager";
  if (/savon|shampoing|douche|dentifrice|deodorant|papier toilette|creme/.test(n)) return "hygiene";
  if (/doliprane|paracetamol|pansement|sirop|vitamine/.test(n)) return "pharmacie";
  if (/huile|sel|poivre|epice|farine|sucre|vinaigre|moutarde|cafe|the/.test(n)) return "cuisine";
  if (/ampoule|pile|vis|colle|outil|brico/.test(n)) return "bricolage";
  return "aliments";
}

function guessWebUnit(name) {
  const match = name.match(/\b(\d+(?:[.,]\d+)?\s*(?:kg|g|l|ml|cl))\b/i);
  return match ? match[1] : "pièce";
}

function handleAnalyzeReceipt() {
  const text = document.getElementById("receipt-raw-text").value.trim();
  if (!text) {
    showToast("Veuillez coller ou importer un ticket de caisse", "warning");
    return;
  }
  currentReceiptLines = parseWebReceiptText(text);
  if (currentReceiptLines.length === 0) {
    showToast("Aucun produit reconnu sur ce ticket", "warning");
    document.getElementById("receipt-results-container").style.display = "none";
    document.getElementById("receipt-apply-btn").style.display = "none";
    return;
  }

  playScannerBeep();
  renderReceiptResults();
  document.getElementById("receipt-results-container").style.display = "block";
  document.getElementById("receipt-apply-btn").style.display = "inline-block";
  showToast(`${currentReceiptLines.length} produit(s) détecté(s) sur le ticket !`, "success");
}

function renderReceiptResults() {
  const container = document.getElementById("receipt-items-list");
  if (!container) return;

  container.innerHTML = currentReceiptLines.map((it, idx) => `
    <div style="display: flex; align-items: center; justify-content: space-between; background: rgba(255,255,255,0.05); padding: 8px 12px; border-radius: 8px;">
      <div style="display: flex; align-items: center; gap: 10px;">
        <input type="checkbox" ${it.selected ? "checked" : ""} onchange="toggleWebReceiptItem(${idx})">
        <div>
          <div style="font-weight: 600; font-size: 0.9rem;">${escapeHtml(it.name)}</div>
          <div style="font-size: 0.75rem; color: ${it.existingItem ? "#38bdf8" : "#a78bfa"};">
            ${it.existingItem ? `🔄 En stock (${it.existingItem.quantity}) → +${it.quantity}` : `✨ Nouvel article (${resolveCategoryLabel(it.category)})`}
          </div>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 6px;">
        <button class="qty-btn" style="width: 26px; height: 26px; font-size: 0.8rem;" onclick="updateWebReceiptQty(${idx}, -1)">-</button>
        <span style="font-weight: bold; min-width: 20px; text-align: center;">${it.quantity}</span>
        <button class="qty-btn" style="width: 26px; height: 26px; font-size: 0.8rem;" onclick="updateWebReceiptQty(${idx}, 1)">+</button>
      </div>
    </div>
  `).join("");
}

function toggleWebReceiptItem(idx) {
  if (currentReceiptLines[idx]) {
    currentReceiptLines[idx].selected = !currentReceiptLines[idx].selected;
  }
}

function updateWebReceiptQty(idx, delta) {
  if (currentReceiptLines[idx]) {
    currentReceiptLines[idx].quantity = Math.max(1, currentReceiptLines[idx].quantity + delta);
    renderReceiptResults();
  }
}

function handleApplyReceipt() {
  const selected = currentReceiptLines.filter(x => x.selected);
  if (selected.length === 0) {
    showToast("Veuillez sélectionner au moins un article", "warning");
    return;
  }

  let updated = 0;
  let added = 0;

  selected.forEach(line => {
    if (line.existingItem) {
      line.existingItem.quantity = Math.round((line.existingItem.quantity || 0) + line.quantity);
      line.existingItem.updatedAt = Date.now();
      updated++;
    } else {
      const nextId = (inventoryData.items.length > 0) ? Math.max(...inventoryData.items.map(x => x.id || 0)) + 1 : 1;
      inventoryData.items.push({
        id: nextId,
        name: line.name,
        categoryId: line.category,
        category: resolveCategoryLabel(line.category),
        location: line.category === "hygiene" ? "Salle de bain" : "Placard",
        quantity: line.quantity,
        minThreshold: 1,
        minQuantity: 1,
        unit: line.unit,
        expirationDate: "",
        preferredStoreId: "supermarket",
        store: "Supermarché",
        barcode: null,
        notes: "Importé depuis ticket de caisse numérique",
        updatedAt: Date.now()
      });
      added++;
    }
  });

  hasUnsavedChanges = true;
  persistLocalBackup();
  renderWebApp();
  closeReceiptModal();
  playScannerBeep();
  showToast(`Inventaire mis à jour : ${updated} réapprovisionné(s), ${added} nouveau(x) produit(s) !`, "success");
}

// -------------------------------------------------------------
// Toast & Helpers
// -------------------------------------------------------------

function showToast(msg, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("fade-out");
    setTimeout(() => toast.remove(), 400);
  }, 3000);
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

// -------------------------------------------------------------
// Category & Store Management (Web)
// -------------------------------------------------------------

let currentManageTab = "categories";

function openCategoriesModal() {
  currentManageTab = "categories";
  switchManageTab("categories");
  renderCategoriesModal();
  document.getElementById("categories-modal").style.display = "flex";
}

function closeCategoriesModal() {
  document.getElementById("categories-modal").style.display = "none";
}

function switchManageTab(tab) {
  currentManageTab = tab;
  const catSec = document.getElementById("cat-manage-section");
  const storeSec = document.getElementById("store-manage-section");
  const tabCat = document.getElementById("tab-cat-view");
  const tabStore = document.getElementById("tab-store-view");
  if (tab === "categories") {
    if (catSec) catSec.style.display = "block";
    if (storeSec) storeSec.style.display = "none";
    if (tabCat) tabCat.style.background = "rgba(59,130,246,0.2)";
    if (tabStore) tabStore.style.background = "transparent";
  } else {
    if (catSec) catSec.style.display = "none";
    if (storeSec) storeSec.style.display = "block";
    if (tabCat) tabCat.style.background = "transparent";
    if (tabStore) tabStore.style.background = "rgba(59,130,246,0.2)";
  }
}

function renderCategoriesModal() {
  const catContainer = document.getElementById("categories-list-container");
  const storeContainer = document.getElementById("stores-list-container");

  if (catContainer) {
    catContainer.innerHTML = "";
    const cats = inventoryData.categories || DEFAULT_CATEGORIES;
    cats.forEach(c => {
      const count = (inventoryData.items || []).filter(it => resolveCategoryId(it.categoryId || it.category) === c.id).length;
      const row = document.createElement("div");
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.justifyContent = "space-between";
      row.style.padding = "10px 14px";
      row.style.background = "rgba(255,255,255,0.05)";
      row.style.borderRadius = "10px";
      row.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <span style="font-size: 1.4rem;">${c.iconEmoji || "📦"}</span>
          <div>
            <div style="font-weight: bold; color: #f8fafc;">${escapeHtml(c.name)}</div>
            <div style="font-size: 0.8rem; color: #94a3b8;">${count} article(s)</div>
          </div>
        </div>
        <button class="icon-btn delete-btn" title="Supprimer la catégorie" style="color: #ef4444; background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 6px;" onclick="deleteCategoryWeb('${c.id}')" ${cats.length <= 1 ? "disabled" : ""}>🗑️</button>
      `;
      catContainer.appendChild(row);
    });
  }

  if (storeContainer) {
    storeContainer.innerHTML = "";
    const stores = inventoryData.storeTypes || DEFAULT_STORE_TYPES;
    stores.forEach(s => {
      const invCount = (inventoryData.items || []).filter(it => resolveStoreId(it.store || it.preferredStoreId) === s.id).length;
      const shopCount = (inventoryData.shoppingItems || []).filter(it => resolveStoreId(it.store || it.storeId) === s.id).length;
      const row = document.createElement("div");
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.justifyContent = "space-between";
      row.style.padding = "10px 14px";
      row.style.background = "rgba(255,255,255,0.05)";
      row.style.borderRadius = "10px";
      row.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <span style="font-size: 1.4rem;">${s.iconEmoji || "🛒"}</span>
          <div>
            <div style="font-weight: bold; color: #f8fafc;">${escapeHtml(s.name)}</div>
            <div style="font-size: 0.8rem; color: #94a3b8;">${invCount} en stock • ${shopCount} en courses</div>
          </div>
        </div>
        <button class="icon-btn delete-btn" title="Supprimer le magasin" style="color: #ef4444; background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 6px;" onclick="deleteStoreWeb('${s.id}')" ${stores.length <= 1 ? "disabled" : ""}>🗑️</button>
      `;
      storeContainer.appendChild(row);
    });
  }
}

function deleteCategoryWeb(catId) {
  const cats = inventoryData.categories || DEFAULT_CATEGORIES;
  if (cats.length <= 1) {
    alert("Impossible de supprimer la seule catégorie restante.");
    return;
  }
  const cat = cats.find(c => c.id === catId);
  const name = cat ? cat.name : catId;
  if (!confirm(`Supprimer la catégorie « ${name} » ? Les articles associés seront réassignés à la catégorie Divers.`)) {
    return;
  }

  const remaining = cats.filter(c => c.id !== catId);
  const fallback = remaining.find(c => c.id === "autre")?.id || remaining[0].id;
  const fallbackName = remaining.find(c => c.id === fallback)?.name || "Divers";

  (inventoryData.items || []).forEach(it => {
    if (resolveCategoryId(it.categoryId || it.category) === catId) {
      it.categoryId = fallback;
      it.category = fallbackName;
    }
  });

  (inventoryData.shoppingItems || []).forEach(it => {
    if (resolveCategoryId(it.categoryId || it.category) === catId) {
      it.categoryId = fallback;
      it.category = fallbackName;
    }
  });

  inventoryData.categories = remaining;
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderCategoriesModal();
  renderWebApp();
  showToast(`Catégorie « ${name} » supprimée`, "info");
}

function deleteStoreWeb(storeId) {
  const stores = inventoryData.storeTypes || DEFAULT_STORE_TYPES;
  if (stores.length <= 1) {
    alert("Impossible de supprimer le seul magasin restant.");
    return;
  }
  const st = stores.find(s => s.id === storeId);
  const name = st ? st.name : storeId;
  if (!confirm(`Supprimer le magasin « ${name} » ? Les articles associés seront réassignés au Supermarché.`)) {
    return;
  }

  const remaining = stores.filter(s => s.id !== storeId);
  const fallback = remaining.find(s => s.id === "supermarket")?.id || remaining[0].id;
  const fallbackName = remaining.find(s => s.id === fallback)?.name || "Supermarché";

  (inventoryData.items || []).forEach(it => {
    if (resolveStoreId(it.store || it.preferredStoreId) === storeId) {
      it.preferredStoreId = fallback;
      it.store = fallbackName;
    }
  });

  (inventoryData.shoppingItems || []).forEach(it => {
    if (resolveStoreId(it.store || it.storeId) === storeId) {
      it.storeId = fallback;
      it.store = fallbackName;
    }
  });

  inventoryData.storeTypes = remaining;
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderCategoriesModal();
  renderWebApp();
  showToast(`Magasin « ${name} » supprimé`, "info");
}

function handleAddCategoryWeb() {
  const nameInput = document.getElementById("new-cat-name");
  const emojiInput = document.getElementById("new-cat-emoji");
  const name = (nameInput?.value || "").trim();
  const emoji = (emojiInput?.value || "").trim() || "📦";
  if (!name) return;

  const id = name.toLowerCase().replace(/\s+/g, "_");
  const cats = inventoryData.categories || DEFAULT_CATEGORIES;
  if (cats.some(c => c.id === id)) {
    alert("Cette catégorie existe déjà.");
    return;
  }

  cats.push({ id, name, iconEmoji: emoji, colorHex: 0xFF4F46E5, orderIndex: cats.length });
  inventoryData.categories = cats;
  CATEGORY_MAP[id] = name;
  if (nameInput) nameInput.value = "";
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderCategoriesModal();
  renderWebApp();
  showToast(`Catégorie « ${name} » ajoutée`, "success");
}

function handleAddStoreWeb() {
  const nameInput = document.getElementById("new-store-name");
  const emojiInput = document.getElementById("new-store-emoji");
  const name = (nameInput?.value || "").trim();
  const emoji = (emojiInput?.value || "").trim() || "🛒";
  if (!name) return;

  const id = name.toLowerCase().replace(/\s+/g, "_");
  const stores = inventoryData.storeTypes || DEFAULT_STORE_TYPES;
  if (stores.some(s => s.id === id)) {
    alert("Ce magasin existe déjà.");
    return;
  }

  stores.push({ id, name, iconEmoji: emoji, colorHex: 0xFF059669, orderIndex: stores.length });
  inventoryData.storeTypes = stores;
  STORE_MAP[id] = name;
  if (nameInput) nameInput.value = "";
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderCategoriesModal();
  renderWebApp();
  showToast(`Magasin « ${name} » ajouté`, "success");
}
