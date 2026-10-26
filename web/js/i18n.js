// Dictionnaire Internationalisation Needix (Français, English, Nederlands)
const I18N = {
  fr: {
    nav_home: "Accueil",
    nav_features: "Concept",
    nav_how: "Mode d'emploi",
    nav_stores: "Télécharger",
    nav_faq: "FAQ",
    nav_app_btn: "Essayer la version Web",
    hero_tag: "VERSION WEB GRATUITE SANS ABONNEMENT • DONNÉES 100% LOCALES",
    hero_title: "Reprenez le contrôle absolu de vos stocks domestiques",
    hero_subtitle: "Needix est le gestionnaire d'inventaire maison éco-conçu et anti-gaspillage. Seule la version Web est accessible gratuitement sans abonnement (les applications mobiles incluent un essai gratuit puis abonnement). Bien que ce site soit hébergé sur Cloudflare Pages, vos données d'inventaire restent 100% locales sur votre appareil.",
    hero_cta_web: "Utiliser la version Web (Sans abonnement)",
    hero_cta_mobile: "Télécharger l'application Mobile",
    hero_badge_privacy: "Version Web sans abonnement • Données 100% locales",
    hero_badge_cloud_free: "Hébergé sur Cloudflare Pages • Zéro stockage serveur",
    hero_badge_green: "Éco-conçu & Souverain • Formats JSON/CSV ouverts",

    // Concept Highlights
    pill_indep: "Données 100% Locales & Hébergement Transparent",
    pill_indep_desc: "L'interface Web statique est hébergée sur Cloudflare Pages, mais aucune donnée d'inventaire ne quitte votre appareil. Seule la version Web est sans abonnement.",
    pill_stock: "Gestion Multi-Emplacements",
    pill_stock_desc: "Visualisez d'un coup d'œil vos réserves : Frigo, Placard, Congélateur, Cave et Salle de bain. Tout est rangé et facile d'accès.",
    pill_antigaspi: "Scan, Tickets Numériques & Anti-Gaspi",
    pill_antigaspi_desc: "Scannez vos codes-barres (avec bip de confirmation), importez vos tickets de caisse numériques de magasin et recevez des alertes avant péremption.",

    // Download Badges
    stores_title: "Disponible sur toutes vos plateformes",
    stores_subtitle: "Seule la version Web ci-dessous est accessible sans abonnement. Les applications mobiles natives (Google Play, App Store, Amazon, Galaxy Store) offrent 14 jours d'essai gratuit puis un abonnement.",
    store_google: "Télécharger sur Google Play",
    store_apple: "Télécharger sur l'App Store",
    store_amazon: "Disponible sur Amazon Appstore",
    store_samsung: "Disponible sur Samsung Galaxy Store",
    store_coming_soon: "Bientôt disponible",

    // How it works
    how_title: "Comment fonctionne Needix ?",
    how_subtitle: "Une prise en main immédiate, fluide et pensée pour le quotidien de votre foyer.",
    step1_title: "1. Scan Bip & Tickets numériques",
    step1_desc: "Scannez vos codes-barres (bip sonore et proposition immédiate d'ajout) ou importez vos tickets de caisse numériques pour mettre à jour votre inventaire automatiquement.",
    step2_title: "2. Suivi des stocks & Alertes",
    step2_desc: "Définissez un stock minimal d'alerte et renseignez la date limite (DLC). L'application calcule les alertes visuelles par code couleur pour consommer en priorité.",
    step3_title: "3. Liste de courses automatique",
    step3_desc: "Dès qu'un article passe sous son seuil minimal, il rejoint automatiquement la liste de courses, classée par magasin habituel pour faire vos courses en un temps record.",
    step4_title: "4. Sauvegarde & Export universel",
    step4_desc: "Exportez et importez à volonté votre fichier JSON ou CSV standard. Compatible avec tous les tableurs (Excel, Calc) et partageable via votre cloud personnel (Nextcloud, Drive, etc.).",

    // FAQ
    faq_title: "Foire Aux Questions",
    faq_subtitle: "Tout ce que vous devez savoir sur la philosophie, l'hébergement Cloudflare et le modèle économique Needix.",
    faq_q1: "Mes données d'inventaire sont-elles envoyées sur Cloudflare ou des serveurs distants ?",
    faq_a1: "Non, jamais. Bien que le site et l'application Web statique soient hébergés sur Cloudflare Pages pour une distribution rapide, vos données d'inventaire restent stockées 100% localement : dans la base de données interne de votre téléphone ou directement dans votre fichier JSON local sur votre ordinateur.",
    faq_q2: "Quelle est la différence d'abonnement entre la version Web et l'application Mobile ?",
    faq_a2: "Seule la version Web (accessible dans votre navigateur via l'API HTML5 File System Access) est gratuite et sans abonnement. Les applications mobiles natives distribuées sur les stores proposent une période d'essai gratuit de 14 jours suivie d'un abonnement.",
    faq_q3: "Puis-je mettre à jour mon stock avec un ticket de caisse numérique ?",
    faq_a3: "Oui ! Sur mobile comme sur la version Web, cliquez sur 'Ticket numérique', importez ou collez votre ticket de magasin : Needix détecte les produits et quantités achetés et vous propose d'actualiser votre inventaire en un clic.",
    faq_q5: "Comment sauvegarder ou synchroniser mon inventaire entre appareils ?",
    faq_a5: "Rendez-vous dans 'Export / Import', exportez votre inventaire en JSON ou CSV sur votre appareil ou dossier partagé (Nextcloud, Drive, clé USB) et ouvrez-le directement sur votre autre appareil.",

    // Web App Embedded / Modal
    app_title: "Application Web Needix (Version gratuite sans abonnement)",
    app_subtitle: "Site hébergé sur Cloudflare Pages • Traitement et stockage 100% locaux sur votre appareil via l'API HTML5 File System Access.",
    btn_open_file: "Ouvrir mon fichier JSON",
    btn_save_file: "Enregistrer les modifications",
    btn_download_json: "Télécharger JSON",
    btn_export_csv: "Exporter CSV",
    btn_receipt_scan: "Ticket numérique",
    btn_manage_categories: "Catégories & Magasins",
    btn_load_sample: "Charger un exemple",
    modal_manage_title: "Gestion des Catégories & Magasins",
    file_connected: "Connecté à",
    file_not_connected: "Aucun fichier ouvert (mode mémoire locale)",
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
    lbl_barcode: "Code-barres (EAN / ISBN)",
    lbl_notes: "Notes",
    btn_cancel: "Annuler",
    btn_save: "Enregistrer l'article",
    footer_text: "Needix • Application éco-conçue sous licence MIT. Site hébergé sur Cloudflare Pages (données d'inventaire 100% locales). Seule la version Web est sans abonnement."
  },

  en: {
    nav_home: "Home",
    nav_features: "Concept",
    nav_how: "How it works",
    nav_stores: "Download",
    nav_faq: "FAQ",
    nav_app_btn: "Try Web Version",
    hero_tag: "SUBSCRIPTION-FREE WEB VERSION • 100% LOCAL DATA",
    hero_title: "Take absolute control over your household inventory",
    hero_subtitle: "Needix is the eco-designed, waste-reducing home inventory manager. Only the Web version is free with no subscription (mobile apps include a free trial then a subscription). While this site is hosted on Cloudflare Pages, your inventory data remains 100% local on your device.",
    hero_cta_web: "Use Web Version (No subscription)",
    hero_cta_mobile: "Download Mobile App",
    hero_badge_privacy: "Subscription-free Web version • 100% Local data",
    hero_badge_cloud_free: "Hosted on Cloudflare Pages • Zero server storage",
    hero_badge_green: "Eco-designed & Sovereign • Open JSON/CSV formats",

    // Concept Highlights
    pill_indep: "100% Local Data & Transparent Hosting",
    pill_indep_desc: "The static Web interface is hosted on Cloudflare Pages, but no inventory data ever leaves your device. Only the Web version requires no subscription.",
    pill_stock: "Multi-Location Tracking",
    pill_stock_desc: "See all your reserves at a glance: Fridge, Pantry, Freezer, Cellar, and Bathroom.",
    pill_antigaspi: "Barcode Beep, Digital Receipts & Waste Prevention",
    pill_antigaspi_desc: "Scan barcodes (with audio beep & instant add prompt), import digital store receipts to update your stock, and get alerts before expiration.",

    // Download Badges
    stores_title: "Available on all your platforms",
    stores_subtitle: "Only the Web version below is subscription-free. Native mobile apps (Google Play, App Store, Amazon, Galaxy Store) include a 14-day free trial followed by a subscription.",
    store_google: "Download on Google Play",
    store_apple: "Download on App Store",
    store_amazon: "Available on Amazon Appstore",
    store_samsung: "Available on Samsung Galaxy Store",
    store_coming_soon: "Coming soon",

    // How it works
    how_title: "How does Needix work?",
    how_subtitle: "Instant, seamless, and tailored for your everyday household routine.",
    step1_title: "1. Beep Scan & Digital Receipts",
    step1_desc: "Scan barcodes (with audio beep and instant add prompt) or import digital store receipts to automatically update your household inventory.",
    step2_title: "2. Stock Tracking & Alerts",
    step2_desc: "Set minimum threshold levels and expiration dates. The app calculates colored urgency alerts to consume items in optimal order.",
    step3_title: "3. Automatic Shopping List",
    step3_desc: "Whenever an item drops below its threshold, it is automatically added to your shopping list, categorized by store.",
    step4_title: "4. Backup & Universal Export",
    step4_desc: "Export and import your standard JSON or CSV files freely. Compatible with spreadsheet software (Excel, LibreOffice) and syncable via your private cloud.",

    // FAQ
    faq_title: "Frequently Asked Questions",
    faq_subtitle: "Everything you need to know about Needix, Cloudflare hosting, and our pricing model.",
    faq_q1: "Is my inventory data sent to Cloudflare or remote servers?",
    faq_a1: "No, never. Although the website and static Web app are hosted on Cloudflare Pages for fast delivery, your inventory data remains 100% local: inside your phone's local database or directly inside your local JSON file on your computer.",
    faq_q2: "What is the subscription difference between the Web version and Mobile app?",
    faq_a2: "Only the Web version (running in your browser via the HTML5 File System Access API) is free without a subscription. Native mobile apps on the app stores offer a 14-day free trial followed by a subscription.",
    faq_q3: "Can I update my inventory using a digital store receipt?",
    faq_a3: "Yes! On both Mobile and Web, click 'Digital Receipt', import or paste your store receipt: Needix extracts purchased items and quantities and proposes a one-click inventory update.",
    faq_q5: "How do I backup or transfer my inventory to another device?",
    faq_a5: "Export your inventory to JSON or CSV and open it directly on your other device or via your own shared folder.",

    // Web App Embedded
    app_title: "Needix Web App (Subscription-free Web version)",
    app_subtitle: "Hosted on Cloudflare Pages • 100% local processing and storage on your device via HTML5 File System Access API.",
    btn_open_file: "Open JSON File",
    btn_save_file: "Save Changes",
    btn_download_json: "Download JSON",
    btn_export_csv: "Export CSV",
    btn_receipt_scan: "Digital Receipt",
    btn_manage_categories: "Categories & Stores",
    btn_load_sample: "Load Sample Data",
    modal_manage_title: "Manage Categories & Stores",
    file_connected: "Connected to",
    file_not_connected: "No file opened (local memory mode)",
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
    lbl_barcode: "Barcode (EAN / ISBN)",
    lbl_notes: "Notes",
    btn_cancel: "Cancel",
    btn_save: "Save Item",
    footer_text: "Needix • Eco-designed software under the MIT license. Site hosted on Cloudflare Pages (100% local inventory data). Only the Web version is subscription-free."
  },

  nl: {
    nav_home: "Start",
    nav_features: "Concept",
    nav_how: "Handleiding",
    nav_stores: "Downloaden",
    nav_faq: "FAQ",
    nav_app_btn: "Webversie proberen",
    hero_tag: "GRATIS WEBVERSIE ZONDER ABONNEMENT • 100% LOKALE GEGEVENS",
    hero_title: "Neem de absolute controle over uw huishoudelijke voorraad",
    hero_subtitle: "Needix is het ecologisch ontworpen voorraadbeheersysteem tegen verspilling. Alleen de webversie is gratis en zonder abonnement (mobiele apps hebben een proefperiode en daarna een abonnement). Hoewel deze site wordt gehost op Cloudflare Pages, blijven uw voorraadgegevens 100% lokaal op uw eigen apparaat.",
    hero_cta_web: "Webversie gebruiken (Zonder abonnement)",
    hero_cta_mobile: "Download Mobiele App",
    hero_badge_privacy: "Webversie zonder abonnement • 100% Lokale opslag",
    hero_badge_cloud_free: "Gehost op Cloudflare Pages • Geen serveropslag",
    hero_badge_green: "Ecologisch ontworpen & Soeverein • Open JSON/CSV",

    // Concept Highlights
    pill_indep: "100% Lokale Gegevens & Transparante Hosting",
    pill_indep_desc: "De statische webinterface wordt gehost op Cloudflare Pages, maar uw voorraadgegevens verlaten nooit uw apparaat. Alleen de webversie is zonder abonnement.",
    pill_stock: "Beheer op meerdere locaties",
    pill_stock_desc: "Bekijk uw voorraden in één oogopslag: Koelkast, Voorraadkast, Diepvriezer, Kelder en Badkamer.",
    pill_antigaspi: "Barcode-piep, Digitale Kassabonnen & Anti-verspilling",
    pill_antigaspi_desc: "Scan barcodes (met geluidssignaal en direct toevoegvoorstel), importeer digitale kassabonnen en ontvang waarschuwingen vóór de vervaldatum.",

    // Download Badges
    stores_title: "Beschikbaar op al uw platforms",
    stores_subtitle: "Alleen de onderstaande webversie is zonder abonnement. De mobiele apps (Google Play, App Store, Amazon, Galaxy Store) bieden 14 dagen gratis op proef gevolgd door een abonnement.",
    store_google: "Downloaden via Google Play",
    store_apple: "Downloaden in de App Store",
    store_amazon: "Beschikbaar in Amazon Appstore",
    store_samsung: "Beschikbaar in Samsung Galaxy Store",
    store_coming_soon: "Binnenkort beschikbaar",

    // How it works
    how_title: "Hoe werkt Needix?",
    how_subtitle: "Directe en eenvoudige bediening, ontworpen voor het dagelijks leven van uw gezin.",
    step1_title: "1. Scannen met piep & Digitale Kassabon",
    step1_desc: "Scan barcodes (met pieptoon en direct toevoegvoorstel) of importeer digitale kassabonnen om uw voorraad automatisch bij te werken.",
    step2_title: "2. Voorraadbeheer & Waarschuwingen",
    step2_desc: "Stel een minimale voorraad en houdbaarheidsdatum in. De app berekent kleurgecodeerde urgentiewaarschuwingen.",
    step3_title: "3. Automatische boodschappenlijst",
    step3_desc: "Zodra een artikel onder het minimum daalt, wordt het automatisch toegevoegd aan de boodschappenlijst per winkel.",
    step4_title: "4. Back-up & Universele Export",
    step4_desc: "Exporteer en importeer uw JSON- of CSV-bestanden. Compatibel met Excel/LibreOffice en deelbaar via uw eigen cloud.",

    // FAQ
    faq_title: "Veelgestelde Vragen",
    faq_subtitle: "Alles wat u moet weten over Needix, Cloudflare-hosting en ons abonnementsmodel.",
    faq_q1: "Worden mijn voorraadgegevens naar Cloudflare of externe servers verzonden?",
    faq_a1: "Nee, nooit. Hoewel de website en statische webapp op Cloudflare Pages worden gehost, blijven uw voorraadgegevens 100% lokaal op uw telefoon of in uw lokale JSON-bestand.",
    faq_q2: "Wat is het verschil in abonnement tussen de Webversie en de Mobiele app?",
    faq_a2: "Alleen de webversie (in uw browser via de HTML5 File System Access API) is gratis zonder abonnement. De mobiele apps bieden een gratis proefperiode van 14 dagen gevolgd door een abonnement.",
    faq_q3: "Kan ik mijn voorraad bijwerken met een digitale kassabon?",
    faq_a3: "Ja! Klik zowel op mobiel als op web op 'Digitale kassabon', importeer of plak uw bon en Needix stelt direct de voorraadupdate voor.",
    faq_q5: "Hoe maak ik een back-up bij verandering van apparaat?",
    faq_a5: "Exporteer uw voorraad als JSON of CSV en open het bestand rechtstreeks op uw andere apparaat.",

    // Web App Embedded
    app_title: "Needix Web App (Gratis webversie zonder abonnement)",
    app_subtitle: "Gehost op Cloudflare Pages • 100% lokale verwerking en opslag op uw apparaat via HTML5 File System Access API.",
    btn_open_file: "JSON-bestand openen",
    btn_save_file: "Wijzigingen opslaan",
    btn_download_json: "JSON downloaden",
    btn_export_csv: "CSV exporteren",
    btn_receipt_scan: "Digitale kassabon",
    btn_manage_categories: "Categorieën & Winkels",
    btn_load_sample: "Voorbeeld laden",
    modal_manage_title: "Beheer van categorieën & winkels",
    file_connected: "Verbonden met",
    file_not_connected: "Geen bestand geopend (lokaal geheugen)",
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
    lbl_barcode: "Barcode (EAN / ISBN)",
    lbl_notes: "Notities",
    btn_cancel: "Annuleren",
    btn_save: "Artikel opslaan",
    footer_text: "Needix • Ecologisch ontworpen software onder de MIT-licentie. Gehost op Cloudflare Pages (100% lokale gegevens). Alleen de webversie is zonder abonnement."
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
