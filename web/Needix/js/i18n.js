// Dictionnaire Internationalisation Needix (Français, English, Nederlands)
const I18N = {
  fr: {
    nav_home: "Accueil",
    nav_features: "Concept",
    nav_how: "Mode d'emploi",
    nav_stores: "Télécharger",
    nav_faq: "FAQ",
    nav_app_btn: "Essayer la version Web",
    hero_tag: "100% LOCAL & SANS ABONNEMENT OBLIGATOIRE",
    hero_title: "Reprenez le contrôle absolu de vos stocks domestiques",
    hero_subtitle: "Needix est le gestionnaire d'inventaire maison éco-conçu, respectueux de votre vie privée et anti-gaspillage. Vos placards, votre frigo et vos courses synchronisés en totale indépendance.",
    hero_cta_web: "Essayer la version Web (Sans installation)",
    hero_cta_mobile: "Télécharger l'application Mobile",
    hero_badge_privacy: "Zéro compte requis • Stockage 100% local",
    hero_badge_cloud_free: "Sans vendor lock-in • JSON/CSV ouverts",
    hero_badge_green: "Éco-conçu & Souverain • Green IT",

    // Concept Highlights
    pill_indep: "Indépendance Totale",
    pill_indep_desc: "Aucune obligation de créer un compte cloud. Vos données vous appartiennent exclusivement.",
    pill_stock: "Gestion Multi-Emplacements",
    pill_stock_desc: "Visualisez d'un coup d'œil vos réserves : Frigo, Placard, Congélateur, Cave et Salle de bain.",
    pill_antigaspi: "Anti-Gaspillage Actif",
    pill_antigaspi_desc: "Alertes intelligentes avant la date limite de péremption pour ne plus jamais jeter.",

    // Download Badges
    stores_title: "Disponible sur toutes vos plateformes",
    stores_subtitle: "Installez l'application native sur vos smartphones et tablettes ou profitez de l'application Web sans installation.",
    store_google: "Télécharger sur Google Play",
    store_apple: "Télécharger sur l'App Store",
    store_amazon: "Disponible sur Amazon Appstore",
    store_samsung: "Disponible sur Samsung Galaxy Store",
    store_coming_soon: "Bientôt disponible",

    // How it works
    how_title: "Comment fonctionne Needix ?",
    how_subtitle: "Une prise en main immédiate, fluide et pensée pour le quotidien de votre foyer.",
    step1_title: "1. Scan & Ajout instantané",
    step1_desc: "Scannez vos codes-barres avec la caméra. Grâce à la base Open Food Facts et au cache local, le nom, la photo et les informations de l'article se remplissent automatiquement.",
    step2_title: "2. Suivi des stocks & Alertes",
    step2_desc: "Définissez un stock minimal d'alerte et renseignez la date limite (DLC). L'application calcule les alertes visuelles par code couleur pour consommer en priorité.",
    step3_title: "3. Liste de courses automatique",
    step3_desc: "Dès qu'un article passe sous son seuil minimal, il rejoint automatiquement la liste de courses, classée par magasin habituel pour faire vos courses en un temps record.",
    step4_title: "4. Sauvegarde & Export universel",
    step4_desc: "Exportez et importez à volonté votre fichier JSON ou CSV standard. Compatible avec tous les tableurs (Excel, Calc) et partageable via votre cloud personnel (Nextcloud, Drive, etc.).",

    // FAQ
    faq_title: "Foire Aux Questions",
    faq_subtitle: "Tout ce que vous devez savoir sur la philosophie et la technologie Needix.",
    faq_q1: "Mes données d'inventaire sont-elles envoyées sur des serveurs distants ?",
    faq_a1: "Non, absolument jamais. Needix est conçu selon le principe de la souveraineté numérique. Vos données restent stockées dans la base de données sécurisée de votre téléphone ou directement dans votre fichier local JSON lorsque vous utilisez l'application Web.",
    faq_q2: "Comment fonctionne la version Web (Wasm) sans installation ?",
    faq_a2: "La version Web utilise l'API standard HTML5 'File System Access' (et WebAssembly). Cela permet à votre navigateur d'ouvrir et de modifier directement un fichier JSON sur votre disque dur ou clé USB, sans qu'aucun octet ne transite par un serveur externe.",
    faq_q3: "Puis-je synchroniser mes données entre mon téléphone et mon PC ?",
    faq_a3: "Oui ! Il vous suffit d'exporter votre fichier JSON depuis l'application mobile (via le sélecteur Android SAF vers votre dossier local ou un dossier synchronisé comme Nextcloud / Google Drive) et de l'ouvrir directement dans la version Web.",
    faq_q5: "Comment sauvegarder ou restaurer mon inventaire en cas de changement d'appareil ?",
    faq_a5: "Rendez-vous dans l'écran 'Export / Import' de l'application, cliquez sur 'Exporter mon inventaire en JSON' et enregistrez le fichier. Sur le nouvel appareil, cliquez simplement sur 'Restaurer depuis un fichier JSON' pour tout retrouver instantanément.",

    // Web App Embedded / Modal
    app_title: "Application Web Needix (Sans installation)",
    app_subtitle: "Pilotez votre inventaire directement depuis votre navigateur avec l'API HTML5 standard File System Access.",
    btn_open_file: "📂 Ouvrir mon fichier JSON",
    btn_save_file: "💾 Enregistrer les modifications",
    btn_download_json: "⬇️ Télécharger JSON",
    btn_export_csv: "📊 Exporter CSV",
    btn_load_sample: "✨ Charger un exemple",
    file_connected: "Connecté à",
    file_not_connected: "Aucun fichier ouvert (mode mémoire)",
    stat_total_items: "Articles au total",
    stat_low_stock: "En stock bas",
    stat_expiring: "À consommer d'urgence",
    stat_shopping: "À acheter",
    filter_all_locations: "Tous les emplacements",
    filter_fridge: "Frigo",
    filter_cupboard: "Placard",
    filter_freezer: "Congélateur",
    filter_cellar: "Cave",
    filter_bathroom: "Salle de bain",
    search_placeholder: "Rechercher un produit, une marque, un code-barres...",
    btn_add_item: "+ Ajouter un article",
    tab_inventory: "📦 Inventaire",
    tab_shopping: "🛒 Liste de courses",
    col_name: "Produit",
    col_location: "Emplacement",
    col_qty: "Quantité",
    col_expiry: "Péremption",
    col_actions: "Actions",
    shopping_empty: "Votre liste de courses est vide. Tous vos stocks sont au-dessus du seuil d'alerte !",
    add_modal_title: "Ajouter un article à l'inventaire",
    edit_modal_title: "Modifier l'article",
    lbl_name: "Nom du produit *",
    lbl_category: "Catégorie",
    lbl_location: "Emplacement",
    lbl_quantity: "Quantité actuelle",
    lbl_min_quantity: "Seuil minimal d'alerte",
    lbl_unit: "Unité (pièces, L, kg...)",
    lbl_expiry: "Date de péremption",
    lbl_store: "Magasin habituel",
    lbl_barcode: "Code-barres",
    lbl_notes: "Notes",
    btn_cancel: "Annuler",
    btn_save: "Enregistrer l'article",
    footer_text: "Needix • Application éco-conçue (Green IT) propriétaire. Tous droits réservés."
  },

  en: {
    nav_home: "Home",
    nav_features: "Concept",
    nav_how: "How it works",
    nav_stores: "Download",
    nav_faq: "FAQ",
    nav_app_btn: "Try Web Version",
    hero_tag: "100% LOCAL & NO MANDATORY SUBSCRIPTION",
    hero_title: "Take absolute control over your household inventory",
    hero_subtitle: "Needix is the eco-designed, privacy-focused home inventory manager. Your pantry, fridge, and shopping lists synchronized with zero cloud vendor lock-in.",
    hero_cta_web: "Try Web Version (No install required)",
    hero_cta_mobile: "Download Mobile App",
    hero_badge_privacy: "Zero account required • 100% Local storage",
    hero_badge_cloud_free: "No vendor lock-in • Open JSON/CSV standards",
    hero_badge_green: "Eco-designed & Sovereign • Green IT",

    // Concept Highlights
    pill_indep: "Total Independence",
    pill_indep_desc: "No cloud account required. Your data belongs exclusively to you.",
    pill_stock: "Multi-Location Tracking",
    pill_stock_desc: "See all your reserves at a glance: Fridge, Pantry, Freezer, Cellar, and Bathroom.",
    pill_antigaspi: "Active Waste Prevention",
    pill_antigaspi_desc: "Smart alerts before expiration dates so you never waste food again.",

    // Download Badges
    stores_title: "Available on all your platforms",
    stores_subtitle: "Install the native app on your phone or tablet, or enjoy the web app with zero installation.",
    store_google: "Download on Google Play",
    store_apple: "Download on App Store",
    store_amazon: "Available on Amazon Appstore",
    store_samsung: "Available on Samsung Galaxy Store",
    store_coming_soon: "Coming soon",

    // How it works
    how_title: "How does Needix work?",
    how_subtitle: "Instant, seamless, and tailored for your everyday household routine.",
    step1_title: "1. Scan & Instant Add",
    step1_desc: "Scan barcodes using your camera. With Open Food Facts and local caching, product name, picture, and details are automatically filled.",
    step2_title: "2. Stock Tracking & Alerts",
    step2_desc: "Set minimum threshold levels and expiration dates. The app calculates colored urgency alerts to consume items in optimal order.",
    step3_title: "3. Automatic Shopping List",
    step3_desc: "Whenever an item drops below its threshold, it is automatically added to your shopping list, categorized by store.",
    step4_title: "4. Backup & Universal Export",
    step4_desc: "Export and import your standard JSON or CSV files freely. Compatible with spreadsheet software (Excel, LibreOffice) and syncable via your private cloud.",

    // FAQ
    faq_title: "Frequently Asked Questions",
    faq_subtitle: "Everything you need to know about the Needix philosophy and technology.",
    faq_q1: "Is my inventory data sent to remote servers?",
    faq_a1: "No, never. Needix is built with digital sovereignty in mind. Your data stays securely stored in your phone's database or directly inside your local JSON file.",
    faq_q2: "How does the zero-install Web (Wasm) version work?",
    faq_a2: "The Web version leverages the standard HTML5 File System Access API. It allows your browser to read and edit your local JSON file directly on your hard drive without any backend server.",
    faq_q3: "Can I synchronize my data between my phone and my PC?",
    faq_a3: "Yes! Simply export your JSON file from the mobile app (using Android SAF to a synced folder like Nextcloud / Google Drive) and open it directly in the Web app.",
    faq_q5: "How do I backup or transfer my inventory to a new device?",
    faq_a5: "Go to the 'Export / Import' screen, click 'Export inventory to JSON', and save the file. On your new device, simply click 'Restore from JSON' to recover everything.",

    // Web App Embedded
    app_title: "Needix Web App (No installation)",
    app_subtitle: "Manage your inventory directly from your browser using the standard HTML5 File System Access API.",
    btn_open_file: "📂 Open JSON File",
    btn_save_file: "💾 Save Changes",
    btn_download_json: "⬇️ Download JSON",
    btn_export_csv: "📊 Export CSV",
    btn_load_sample: "✨ Load Sample Data",
    file_connected: "Connected to",
    file_not_connected: "No file opened (in-memory mode)",
    stat_total_items: "Total items",
    stat_low_stock: "Low stock items",
    stat_expiring: "Expiring soon",
    stat_shopping: "To buy",
    filter_all_locations: "All locations",
    filter_fridge: "Fridge",
    filter_cupboard: "Pantry",
    filter_freezer: "Freezer",
    filter_cellar: "Cellar",
    filter_bathroom: "Bathroom",
    search_placeholder: "Search product, brand, barcode...",
    btn_add_item: "+ Add Item",
    tab_inventory: "📦 Inventory",
    tab_shopping: "🛒 Shopping List",
    col_name: "Product",
    col_location: "Location",
    col_qty: "Quantity",
    col_expiry: "Expiration",
    col_actions: "Actions",
    shopping_empty: "Your shopping list is empty. All stocks are above minimum threshold!",
    add_modal_title: "Add item to inventory",
    edit_modal_title: "Edit item",
    lbl_name: "Product Name *",
    lbl_category: "Category",
    lbl_location: "Location",
    lbl_quantity: "Current Quantity",
    lbl_min_quantity: "Minimum threshold",
    lbl_unit: "Unit (pcs, L, kg...)",
    lbl_expiry: "Expiration Date",
    lbl_store: "Preferred Store",
    lbl_barcode: "Barcode",
    lbl_notes: "Notes",
    btn_cancel: "Cancel",
    btn_save: "Save Item",
    footer_text: "Needix • Eco-designed (Green IT) proprietary software. All Rights Reserved."
  },

  nl: {
    nav_home: "Start",
    nav_features: "Concept",
    nav_how: "Handleiding",
    nav_stores: "Downloaden",
    nav_faq: "FAQ",
    nav_app_btn: "Webversie proberen",
    hero_tag: "100% LOKAAL & GEEN VERPLICHT ABONNEMENT",
    hero_title: "Neem de absolute controle over uw huishoudelijke voorraad",
    hero_subtitle: "Needix is het ecologisch ontworpen voorraadbeheersysteem voor thuis, gericht op privacy en tegen voedselverspilling. Uw voorraadkast, koelkast en boodschappenlijstjes zonder vendor lock-in.",
    hero_cta_web: "Webversie proberen (Zonder installatie)",
    hero_cta_mobile: "Download Mobiele App",
    hero_badge_privacy: "Geen account vereist • 100% Lokale opslag",
    hero_badge_cloud_free: "Geen vendor lock-in • Open JSON/CSV standaarden",
    hero_badge_green: "Ecologisch ontworpen & Soeverein • Green IT",

    // Concept Highlights
    pill_indep: "Totale Onafhankelijkheid",
    pill_indep_desc: "Geen cloudaccount nodig. Uw gegevens blijven uitsluitend van u.",
    pill_stock: "Beheer op meerdere locaties",
    pill_stock_desc: "Bekijk uw voorraden in één oogopslag: Koelkast, Voorraadkast, Diepvriezer, Kelder en Badkamer.",
    pill_antigaspi: "Actieve Verspillingspreventie",
    pill_antigaspi_desc: "Slimme waarschuwingen vóór de vervaldatum om nooit meer voedsel te verspillen.",

    // Download Badges
    stores_title: "Beschikbaar op al uw platforms",
    stores_subtitle: "Installeer de app op uw smartphone of tablet, of gebruik de webversie zonder installatie.",
    store_google: "Downloaden via Google Play",
    store_apple: "Downloaden in de App Store",
    store_amazon: "Beschikbaar in Amazon Appstore",
    store_samsung: "Beschikbaar in Samsung Galaxy Store",
    store_coming_soon: "Binnenkort beschikbaar",

    // How it works
    how_title: "Hoe werkt Needix?",
    how_subtitle: "Directe en eenvoudige bediening, ontworpen voor het dagelijks leven van uw gezin.",
    step1_title: "1. Scannen & Direct toevoegen",
    step1_desc: "Scan barcodes met uw camera. Dankzij Open Food Facts en lokale caching worden productnaam, foto en gegevens automatisch aangevuld.",
    step2_title: "2. Voorraadbeheer & Waarschuwingen",
    step2_desc: "Stel een minimale voorraad en houdbaarheidsdatum in. De app berekent kleurgecodeerde urgentiewaarschuwingen.",
    step3_title: "3. Automatische boodschappenlijst",
    step3_desc: "Zodra een artikel onder het minimum daalt, wordt het automatisch toegevoegd aan de boodschappenlijst per winkel.",
    step4_title: "4. Back-up & Universele Export",
    step4_desc: "Exporteer en importeer uw JSON- of CSV-bestanden. Compatibel met Excel/LibreOffice en deelbaar via uw eigen cloud.",

    // FAQ
    faq_title: "Veelgestelde Vragen",
    faq_subtitle: "Alles wat u moet weten over de filosofie en technologie van Needix.",
    faq_q1: "Worden mijn voorraadgegevens naar externe servers verzonden?",
    faq_a1: "Nee, absoluut nooit. Needix is ontworpen met digitale soevereiniteit als kernprincipe. Uw gegevens blijven veilig op uw toestel of in uw lokale JSON-bestand.",
    faq_q2: "Hoe werkt de webversie (Wasm) zonder installatie?",
    faq_a2: "De webversie gebruikt de HTML5 File System Access API. Hierdoor kan uw browser rechtstreeks een JSON-bestand op uw computer openen en bewerken zonder externe server.",
    faq_q3: "Kan ik gegevens synchroniseren tussen telefoon en computer?",
    faq_a3: "Ja! Exporteer eenvoudig uw JSON-bestand vanuit de mobiele app naar een gesynchroniseerde map (bijv. Nextcloud of Drive) en open het in de webapp.",
    faq_q5: "Hoe maak ik een back-up bij verandering van apparaat?",
    faq_a5: "Ga naar het scherm 'Export / Import', klik op 'Voorraad exporteren als JSON' en bewaar het bestand. Op uw nieuwe apparaat klikt u op 'Herstellen via JSON'.",

    // Web App Embedded
    app_title: "Needix Web App (Zonder installatie)",
    app_subtitle: "Beheer uw voorraad rechtstreeks vanuit uw browser via de HTML5 File System Access API.",
    btn_open_file: "📂 JSON-bestand openen",
    btn_save_file: "💾 Wijzigingen opslaan",
    btn_download_json: "⬇️ JSON downloaden",
    btn_export_csv: "📊 CSV exporteren",
    btn_load_sample: "✨ Voorbeeld laden",
    file_connected: "Verbonden met",
    file_not_connected: "Geen bestand geopend (in geheugen)",
    stat_total_items: "Totaal artikelen",
    stat_low_stock: "Lage voorraad",
    stat_expiring: "Binnenkort vervallen",
    stat_shopping: "Te kopen",
    filter_all_locations: "Alle locaties",
    filter_fridge: "Koelkast",
    filter_cupboard: "Voorraadkast",
    filter_freezer: "Diepvriezer",
    filter_cellar: "Kelder",
    filter_bathroom: "Badkamer",
    search_placeholder: "Zoek product, merk, barcode...",
    btn_add_item: "+ Artikel toevoegen",
    tab_inventory: "📦 Voorraad",
    tab_shopping: "🛒 Boodschappenlijst",
    col_name: "Product",
    col_location: "Locatie",
    col_qty: "Aantal",
    col_expiry: "Vervaldatum",
    col_actions: "Acties",
    shopping_empty: "Uw boodschappenlijst is leeg. Alle voorraden zijn op peil!",
    add_modal_title: "Artikel toevoegen aan voorraad",
    edit_modal_title: "Artikel bewerken",
    lbl_name: "Productnaam *",
    lbl_category: "Categorie",
    lbl_location: "Locatie",
    lbl_quantity: "Huidig aantal",
    lbl_min_quantity: "Minimale voorraad",
    lbl_unit: "Eenheid (stuks, L, kg...)",
    lbl_expiry: "Vervaldatum",
    lbl_store: "Vaste winkel",
    lbl_barcode: "Barcode",
    lbl_notes: "Notities",
    btn_cancel: "Annuleren",
    btn_save: "Artikel opslaan",
    footer_text: "Needix • Ecologisch ontworpen (Green IT) propriëtaire software. Alle rechten voorbehouden."
  }
};

let currentLang = localStorage.getItem("needix_lang") || "fr";

function setLanguage(lang) {
  if (!I18N[lang]) lang = "fr";
  currentLang = lang;
  localStorage.setItem("needix_lang", lang);
  document.documentElement.lang = lang;
  
  // Update select if exists
  const langSelect = document.getElementById("lang-select");
  if (langSelect) langSelect.value = lang;

  // Update all data-i18n elements
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (I18N[lang][key]) {
      el.textContent = I18N[lang][key];
    }
  });

  // Update all data-i18n-placeholder elements
  document.querySelectorAll("[data-i18n-placeholder]").forEach(el => {
    const key = el.getAttribute("data-i18n-placeholder");
    if (I18N[lang][key]) {
      el.setAttribute("placeholder", I18N[lang][key]);
    }
  });

  // If web app is rendered, refresh rendering
  if (window.renderWebApp) {
    window.renderWebApp();
  }
}

function t(key) {
  return (I18N[currentLang] && I18N[currentLang][key]) || (I18N["fr"] && I18N["fr"][key]) || key;
}
