// Needix Web App Engine (File System Access API & Wasm/HTML5)
// Gestionnaire d'inventaire 100% local, sans serveur ni vendor lock-in

let inventoryData = {
  app: "Needix",
  version: "1.0.0",
  exportDate: new Date().toISOString(),
  items: [],
  shoppingList: []
};

let currentFileHandle = null;
let hasUnsavedChanges = false;
let activeTab = "inventory"; // "inventory" or "shopping"
let activeLocationFilter = "all";
let searchQuery = "";
let editingItemId = null;

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
  initEventListeners();
  loadInitialState();
});

function initEventListeners() {
  // Tab buttons
  const tabInv = document.getElementById("tab-btn-inv");
  const tabShop = document.getElementById("tab-btn-shop");
  if (tabInv && tabShop) {
    tabInv.addEventListener("click", () => switchTab("inventory"));
    tabShop.addEventListener("click", () => switchTab("shopping"));
  }

  // File action buttons
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

  // Modal form
  const modalForm = document.getElementById("item-modal-form");
  const modalCancel = document.getElementById("modal-cancel-btn");
  const modalClose = document.getElementById("modal-close-btn");
  if (modalForm) modalForm.addEventListener("submit", handleSaveItemForm);
  if (modalCancel) modalCancel.addEventListener("click", closeModal);
  if (modalClose) modalClose.addEventListener("click", closeModal);

  // Hidden file input for fallback
  const fallbackInput = document.getElementById("fallback-file-input");
  if (fallbackInput) {
    fallbackInput.addEventListener("change", handleFallbackFileInput);
  }
}

async function loadInitialState() {
  // Check if there is saved data in localStorage from previous session
  const cached = localStorage.getItem("needix_cached_inventory");
  if (cached) {
    try {
      inventoryData = JSON.parse(cached);
    } catch (e) {
      console.warn("Could not parse cached inventory", e);
    }
  }

  // If items empty, load sample automatically for pleasant first visit
  if (!inventoryData.items || inventoryData.items.length === 0) {
    await loadSampleData();
  } else {
    renderWebApp();
  }
}

// -------------------------------------------------------------
// File System Access API (Standard HTML5)
// -------------------------------------------------------------

async function handleOpenFile() {
  if ("showOpenFilePicker" in window) {
    try {
      const [handle] = await window.showOpenFilePicker({
        types: [{
          description: "Fichier Inventaire Needix (*.json)",
          accept: { "application/json": [".json"] }
        }],
        multiple: false
      });
      const file = await handle.getFile();
      const text = await file.text();
      const parsed = JSON.parse(text);

      if (Array.isArray(parsed.items) || Array.isArray(parsed)) {
        inventoryData = Array.isArray(parsed) ? { app: "Needix", version: "1.0.0", items: parsed, shoppingList: [] } : parsed;
        currentFileHandle = handle;
        hasUnsavedChanges = false;
        updateFileStatusBadge(file.name);
        persistLocalBackup();
        renderWebApp();
        showToast(t("file_connected") + " " + file.name, "success");
      } else {
        showToast("Format JSON invalide. Structure attendue : { items: [...] }", "error");
      }
    } catch (err) {
      if (err.name !== "AbortError") {
        console.error("Erreur ouverture fichier:", err);
        showToast("Impossible d'ouvrir le fichier : " + err.message, "error");
      }
    }
  } else {
    // Fallback using standard HTML input
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
      const parsed = JSON.parse(event.target.result);
      inventoryData = Array.isArray(parsed) ? { app: "Needix", version: "1.0.0", items: parsed, shoppingList: [] } : parsed;
      currentFileHandle = null;
      hasUnsavedChanges = false;
      updateFileStatusBadge(file.name + " (Navigateur)");
      persistLocalBackup();
      renderWebApp();
      showToast(t("file_connected") + " " + file.name, "success");
    } catch (err) {
      showToast("Erreur de lecture du fichier JSON", "error");
    }
  };
  reader.readAsText(file);
}

async function handleSaveFile() {
  inventoryData.exportDate = new Date().toISOString();
  const content = JSON.stringify(inventoryData, null, 2);

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

  // If no file handle or save as new file
  if ("showSaveFilePicker" in window) {
    try {
      const handle = await window.showSaveFilePicker({
        suggestedName: "needix_inventaire.json",
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
    // Fallback: standard download
    handleDownloadJson();
  }
}

function handleDownloadJson() {
  inventoryData.exportDate = new Date().toISOString();
  const content = JSON.stringify(inventoryData, null, 2);
  const blob = new Blob([content], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `needix_inventaire_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  hasUnsavedChanges = false;
  updateSaveStatus();
  showToast("Fichier JSON téléchargé !", "success");
}

function handleExportCsv() {
  const items = inventoryData.items || [];
  if (items.length === 0) {
    showToast("Aucun article à exporter", "warning");
    return;
  }

  let csv = "ID;Nom;Catégorie;Emplacement;Quantité;Unité;Seuil_Alerte;Date_Péremption;Magasin;Code_Barres;Notes\n";
  items.forEach(it => {
    const row = [
      it.id || "",
      `"${(it.name || "").replace(/"/g, '""')}"`,
      `"${(it.category || "").replace(/"/g, '""')}"`,
      `"${(it.location || "").replace(/"/g, '""')}"`,
      it.quantity ?? 0,
      `"${(it.unit || "").replace(/"/g, '""')}"`,
      it.minQuantity ?? 1,
      it.expirationDate || "",
      `"${(it.store || "").replace(/"/g, '""')}"`,
      `"${(it.barcode || "").replace(/"/g, '""')}"`,
      `"${(it.notes || "").replace(/"/g, '""')}"`
    ];
    csv += row.join(";") + "\n";
  });

  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `needix_export_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("Export CSV généré avec succès !", "success");
}

async function loadSampleData() {
  try {
    const res = await fetch("/sample-inventory.json");
    if (res.ok) {
      inventoryData = await res.json();
    } else {
      throw new Error("Local sample json not reachable");
    }
  } catch (e) {
    // Fallback embedded sample
    inventoryData = {
      app: "Needix",
      version: "1.0.0",
      exportDate: new Date().toISOString(),
      items: [
        { id: 1, name: "Lait demi-écrémé Bio", category: "Produits laitiers", location: "Frigo", quantity: 2, unit: "L", minQuantity: 2, expirationDate: "2026-10-18", barcode: "3250390112345", store: "BioCoop", notes: "Pack 1L" },
        { id: 2, name: "Yaourt nature fermier", category: "Produits laitiers", location: "Frigo", quantity: 4, unit: "pots", minQuantity: 4, expirationDate: "2026-10-13", barcode: "3155250004321", store: "Supermarché", notes: "Péremption imminente !" },
        { id: 3, name: "Pâtes Penne Rigate", category: "Épicerie salée", location: "Placard", quantity: 3, unit: "paquets", minQuantity: 2, expirationDate: "2027-04-20", barcode: "8001250123456", store: "Supermarché", notes: "500g" },
        { id: 4, name: "Huile d'olive extra", category: "Condiments & Huiles", location: "Placard", quantity: 1, unit: "L", minQuantity: 1, expirationDate: "2027-08-15", barcode: "8410250987654", store: "Épicerie locale", notes: "Première pression" },
        { id: 5, name: "Steaks hachés pur bœuf", category: "Viandes & Poissons", location: "Congélateur", quantity: 6, unit: "pièces", minQuantity: 4, expirationDate: "2026-12-30", barcode: "3560070119988", store: "Boucherie", notes: "15% MG" },
        { id: 6, name: "Café moulu arabica", category: "Boissons & Café", location: "Placard", quantity: 0, unit: "paquet", minQuantity: 1, expirationDate: "2027-01-10", barcode: "3017760000123", store: "Torréfacteur", notes: "En rupture" }
      ],
      shoppingList: [
        { id: 1, name: "Café moulu arabica", quantity: 2, unit: "paquets", checked: false, store: "Torréfacteur" },
        { id: 2, name: "Beurre doux AOP", quantity: 1, unit: "plaquette", checked: false, store: "BioCoop" }
      ]
    };
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
    if (it.quantity <= (it.minQuantity || 1)) lowStock++;
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
    badge.innerHTML = `<span class="status-dot online"></span> ${t("file_connected")} : <strong>${fileName}</strong>`;
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
    // Location filter
    if (activeLocationFilter !== "all" && it.location !== activeLocationFilter) {
      return false;
    }
    // Search query
    if (searchQuery) {
      const matchName = (it.name || "").toLowerCase().includes(searchQuery);
      const matchCat = (it.category || "").toLowerCase().includes(searchQuery);
      const matchStore = (it.store || "").toLowerCase().includes(searchQuery);
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
    // Expiration status
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
        expBadge = `<span class="badge neutral">${it.expirationDate}</span>`;
      }
    } else {
      expBadge = `<span class="badge muted">Non renseignée</span>`;
    }

    // Stock alert
    const isLowStock = it.quantity <= (it.minQuantity || 1);
    const stockClass = isLowStock ? "low-stock" : "";

    html += `
      <div class="item-card ${stockClass}">
        <div class="card-header">
          <div>
            <span class="location-chip ${getLocationClass(it.location)}">${it.location || "Maison"}</span>
            <span class="category-chip">${it.category || "Divers"}</span>
          </div>
          ${expBadge}
        </div>
        <h4 class="item-title">${escapeHtml(it.name)}</h4>
        ${it.notes ? `<p class="item-notes">${escapeHtml(it.notes)}</p>` : ""}
        
        <div class="card-meta">
          ${it.store ? `<span class="meta-item">🏪 ${escapeHtml(it.store)}</span>` : ""}
          ${it.barcode ? `<span class="meta-item">🔍 ${escapeHtml(it.barcode)}</span>` : ""}
        </div>

        <div class="quantity-controller">
          <button class="qty-btn" onclick="updateItemQuantity(${it.id}, -1)" title="Diminuer">-</button>
          <div class="qty-display">
            <span class="qty-num">${it.quantity}</span>
            <span class="qty-unit">${escapeHtml(it.unit || "pièce(s)")}</span>
          </div>
          <button class="qty-btn" onclick="updateItemQuantity(${it.id}, 1)" title="Augmenter">+</button>
        </div>

        <div class="card-footer">
          <span class="min-threshold">Seuil alerte : ${it.minQuantity || 1}</span>
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
    const min = it.minQuantity || 1;
    if (it.quantity <= min) {
      const deficit = Math.max(1, min * 2 - it.quantity);
      needed.push({
        id: it.id,
        name: it.name,
        quantity: deficit,
        unit: it.unit || "pièce(s)",
        store: it.store || "Supermarché",
        currentQty: it.quantity,
        minQty: min,
        checked: false
      });
    }
  });

  // Merge with custom entries in inventoryData.shoppingList
  if (Array.isArray(inventoryData.shoppingList)) {
    inventoryData.shoppingList.forEach(custom => {
      if (!needed.some(n => n.name.toLowerCase() === custom.name.toLowerCase())) {
        needed.push(custom);
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

  // Group by store
  const grouped = {};
  list.forEach(it => {
    const s = it.store || "Divers / Tout magasin";
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
      html += `
        <li class="shopping-item-row ${it.checked ? "checked" : ""}">
          <label class="checkbox-container">
            <input type="checkbox" ${it.checked ? "checked" : ""} onchange="toggleShoppingItem(${it.id})">
            <span class="checkmark"></span>
            <span class="shop-name">${escapeHtml(it.name)}</span>
          </label>
          <div class="shop-qty-badge">
            À acheter : <strong>${it.quantity} ${escapeHtml(it.unit || "")}</strong>
            ${it.currentQty !== undefined ? `<small class="text-muted">(en stock: ${it.currentQty})</small>` : ""}
          </div>
          <button class="btn-restock" onclick="quickRestock(${it.id}, ${it.quantity})" title="Marquer comme acheté et réapprovisionner l'inventaire">
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
  it.quantity = Math.max(0, (it.quantity || 0) + delta);
  hasUnsavedChanges = true;
  persistLocalBackup();
  renderWebApp();
}

function quickRestock(id, qtyToAdd) {
  const it = inventoryData.items.find(x => x.id === id);
  if (it) {
    it.quantity = (it.quantity || 0) + (qtyToAdd || 1);
    hasUnsavedChanges = true;
    persistLocalBackup();
    renderWebApp();
    showToast(`Réapprovisionné : +${qtyToAdd} ${it.name}`, "success");
  }
}

function toggleShoppingItem(id) {
  const custom = (inventoryData.shoppingList || []).find(x => x.id === id);
  if (custom) {
    custom.checked = !custom.checked;
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

function openAddModal() {
  editingItemId = null;
  document.getElementById("modal-title").textContent = t("add_modal_title");
  document.getElementById("modal-item-id").value = "";
  document.getElementById("item-name").value = "";
  document.getElementById("item-category").value = "Épicerie salée";
  document.getElementById("item-location").value = "Placard";
  document.getElementById("item-quantity").value = "1";
  document.getElementById("item-min-quantity").value = "1";
  document.getElementById("item-unit").value = "pièce(s)";
  document.getElementById("item-expiry").value = "";
  document.getElementById("item-store").value = "";
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
  document.getElementById("item-category").value = it.category || "Divers";
  document.getElementById("item-location").value = it.location || "Placard";
  document.getElementById("item-quantity").value = it.quantity ?? 1;
  document.getElementById("item-min-quantity").value = it.minQuantity ?? 1;
  document.getElementById("item-unit").value = it.unit || "pièce(s)";
  document.getElementById("item-expiry").value = it.expirationDate || "";
  document.getElementById("item-store").value = it.store || "";
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

  const category = document.getElementById("item-category").value;
  const location = document.getElementById("item-location").value;
  const quantity = parseFloat(document.getElementById("item-quantity").value) || 0;
  const minQuantity = parseFloat(document.getElementById("item-min-quantity").value) || 1;
  const unit = document.getElementById("item-unit").value.trim() || "pièce(s)";
  const expirationDate = document.getElementById("item-expiry").value || "";
  const store = document.getElementById("item-store").value.trim();
  const barcode = document.getElementById("item-barcode").value.trim();
  const notes = document.getElementById("item-notes").value.trim();

  if (editingItemId) {
    // Edit existing
    const it = inventoryData.items.find(x => x.id === editingItemId);
    if (it) {
      it.name = name;
      it.category = category;
      it.location = location;
      it.quantity = quantity;
      it.minQuantity = minQuantity;
      it.unit = unit;
      it.expirationDate = expirationDate;
      it.store = store;
      it.barcode = barcode;
      it.notes = notes;
    }
  } else {
    // Add new
    const nextId = (inventoryData.items.length > 0) ? Math.max(...inventoryData.items.map(x => x.id || 0)) + 1 : 1;
    inventoryData.items.push({
      id: nextId,
      name,
      category,
      location,
      quantity,
      minQuantity,
      unit,
      expirationDate,
      store,
      barcode,
      notes
    });
  }

  hasUnsavedChanges = true;
  persistLocalBackup();
  closeModal();
  renderWebApp();
  showToast("Article enregistré avec succès !", "success");
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
