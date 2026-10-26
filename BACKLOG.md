# Needix — Backlog & Spécifications de Développement

Ce document centralise l'état d'avancement, le registre des décisions et les tâches en attente du produit **Needix**.

---

## 1. Backlog Actif

### [BACKLOG-JURIDIQUE-16] Retour sous Licence MIT
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur du 26/10/2026 — ensemble de l'écosystème passé sous MIT.
- **Spécifications** :
  1. Remplacement du fichier `LICENSE` propriétaire (`All Rights Reserved`, cf. JURIDIQUE-01, conservé ci-dessous à titre historique) par la licence **MIT** : « Copyright (c) 2026 Geoff1805 ».
  2. Le code de Needix est désormais utilisable, modifiable et redistribuable selon les termes MIT ; le modèle commercial (essai 14 jours + abonnement RevenueCat, version Web gratuite) reste inchangé.

### [BACKLOG-JURIDIQUE-01] Correction Juridique : Passage en Licence Propriétaire
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Pivot commercial 100%.
- **Spécifications** :
  1. Remplacement intégral du fichier `LICENSE` à la racine par la mention de droits d'auteur stricte :
     ```text
     Copyright (c) 2026 Geoffroy Alsteens. All Rights Reserved. 
     This software is proprietary and confidential. Unauthorized copying, modifying, merging, publishing, distributing, or selling of this software, via any medium, is strictly prohibited.
     ```
  2. Suppression de toutes les mentions de la licence "Apache 2.0" dans l'ensemble du projet :
     - Landing page web (`web/index.html` : footer, liens de licence).
     - Dictionnaire d'internationalisation (`web/js/i18n.js` : `footer_text` en FR, EN, NL).
     - Documentation associée (`BUILD_MANUAL_GUIDE.md`, etc.).

---

### [BACKLOG-BRANDING-02] Suppression de toute référence à Google AI Code
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Nettoyage d'environnement et de marque.
- **Spécifications** :
  1. Audit complet du codebase pour vérifier l'absence de mentions "Google AI Code" ou dérivés dans les commentaires, codes sources et documentations client.
  2. Préservation stricte de la directive système AI Studio : maintien obligatoire de la clé `"MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"` dans `metadata.json` (sans mentionner Google AI Code dans l'application finale).

---

### [BACKLOG-CLEANUP-03] Suppression des boutons et références à F-Droid, GitHub et Dons FOSS
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Abandon définitif du canal FOSS / F-Droid suite au passage 100% commercial.
- **Spécifications** :
  1. **Landing Page (`web/index.html`)** :
     - Suppression de la carte de téléchargement `F-Droid` (`.store-badge-card.fdroid-card`).
     - Suppression du lien GitHub dans le footer de la page.
     - Suppression de la FAQ n°4 comparant la version F-Droid et la version commerciale.
     - Nettoyage du badge de hero "Modèle Open-Source & FOSS" (`hero_badge_foss`).
  2. **Traductions (`web/js/i18n.js`)** :
     - Suppression des clés et libellés `store_fdroid`, `faq_q4`, `faq_a4`, `hero_badge_foss` pour les 3 langues (FR, EN, NL).
  3. **Application Android (`app/src/main/`)** :
     - Suppression du sélecteur de variante "Tester la variante : F-Droid / Play Store" dans l'écran de synchronisation / sauvegarde (`SyncScreen.kt`).
     - Suppression de l'accès à l'écran de dons FOSS (`SupportProjectScreen.kt`) et des boutons de dons tiers (GitHub Sponsors, Liberapay, Ko-fi, etc.).
     - Unification du modèle économique sur le mode commercial RevenueCat (période d'essai 14 jours, abonnement, restauration).
     - Nettoyage des chaînes de caractères résiduelles associées (`strings.xml` fr, en, nl).

---

### [BACKLOG-BUG-04] Correction du problème des doubles icônes (Lanceur Android / Web)
- **Statut** : `Vérifié / Conforme`
- **Origine** : Demande utilisateur avec capture d'écran — signalement de double icône.
- **Spécifications & Diagnostic technique** :
  1. **Côté Android** :
     - Contrôle d'unicité de l'intent filter `android.intent.category.LAUNCHER` dans `AndroidManifest.xml` (garantir qu'une seule Activity possède l'entrée du lanceur).
     - Vérification de l'absence d'`<activity-alias>` résiduel issu d'anciennes variantes.
     - Harmonisation des icônes adaptatives vectorielles (`res/mipmap-anydpi-v26/ic_launcher.xml` et `ic_launcher_round.xml`) avec les drawables matriciels WebP pour prévenir tout dédoublement sur les launchers d'appareils physiques et émulateurs.
  2. **Côté Web / Landing Page** :
     - Vérifier la barre de navigation et le favicon pour éviter toute redondance d'affichage d'icône.

---

### [BACKLOG-WEB-05] Correction du chemin de base (Base Path) Web & Sous-domaine Dédié (needix.pages.dev)
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Hébergement sur le sous-domaine dédié `https://needix.pages.dev`, suppression du sous-dossier `/Needix/`.
- **Spécifications** :
  1. **Fichiers HTML (`web/index.html`)** :
     - Ajouter / configurer la balise `<base href="/">` dans la section `<head>` pour faire pointer la racine vers `/` au lieu d'un sous-dossier.
     - S'assurer que tous les assets (CSS, JS, images, polices, favicons) sont appelés depuis la racine (`/css/style.css`, `/js/app.js`, `/js/i18n.js`, etc.) sans préfixe `/Needix/`.
  2. **Routing et Code Kotlin (Android & Multiplatform)** :
     - Vérifier la configuration des URLs dans le code Kotlin : mise à jour de l'URL cible de la Web App de `https://needix.pages.dev/Needix/` vers `https://needix.pages.dev/` (dans `SyncScreen.kt`).
     - Nettoyer les mentions de `(/Needix)` dans les libellés (`strings.xml` fr, en, nl : `web_landing_button`, `web_landing_desc`).
  3. **Configuration Cloudflare Pages & Redirections** :
     - Supprimer la redirection `_redirects` et le sous-dossier dupliqué `web/Needix/`.
     - Déployer le contenu web directement à la racine pour servir `index.html` à la racine de `https://needix.pages.dev`.

---

### [BACKLOG-SCANNER-06] Reconnaissance Universelle Multi-API & Base Hors-Ligne (`Open-Product-Database`)
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Maximiser le taux de reconnaissance du lecteur de codes-barres (alimentaire, ménager, cosmétique, livres/culture et mode hors-ligne).
- **Spécifications** :
  1. **Base de données locale embarquée (`Open-Product-Database`)** :
     - Intégration d'une base de données JSON locale embarquée (`app/src/main/assets/open_product_database.json` et `web/data/open-product-database.json`) + cache local en mémoire pour reconnaître instantanément les produits courants **sans aucune connexion réseau** (0 ms de latence, Green IT).
  2. **API Open Food Facts v2 (Alimentation mondiale)** :
     - Endpoint : `https://world.openfoodfacts.net/api/v2/product/{code_barre}` (avec repli sur `world.openfoodfacts.org/api/v2/product/{code_barre}`).
     - Récupération du nom (`product_name_fr` / `product_name`), de la marque (`brands`), de la quantité/unité (`quantity`), de l'image, du **Nutri-Score** (`nutriscore_grade`), des **ingrédients** et des **allergènes**.
  3. **API Open Products Facts v2 (Maison, Ménager, Bricolage, Électronique)** :
     - Endpoint : `https://world.openproductsfacts.org/api/v2/product/{code_barre}`.
     - Architecture unifiée à 98 % avec Open Food Facts ; catégorisation automatique vers `menager`, `bricolage` ou `autre`.
  4. **API Open Beauty Facts v2 (Cosmétiques, Hygiène, Parfums, Soins)** :
     - Endpoint : `https://world.openbeautyfacts.org/api/v2/product/{code_barre}`.
     - Même structure JSON ouverte ; catégorisation automatique vers `hygiene` ou `pharmacie`.
  5. **API Inventaire.io (Livres & Culture par code ISBN)** :
     - Endpoint : `https://api.inventaire.io/api/entities/by-uris?uris=isbn:{code_barre}`.
     - Détection prioritaire des préfixes ISBN-13 (`978`, `979`) et ISBN-10 pour extraire le titre de l'ouvrage, l'auteur/éditeur, la couverture et les métadonnées culturelles.
  6. **Chomp Food API v2 (Nutrition & Aliments emballés — Dernier recours)** :
     - Endpoint : `https://chompthis.com/api/v2/food/branded/barcode.php?code={code_barre}&api_key={CHOMP_API_KEY}`.
     - Appelée uniquement en dernier recours si aucune base ouverte gratuite ni la base locale n'a trouvé le produit (afin de préserver le forfait gratuit de 1 000 requêtes/mois), lorsque `CHOMP_API_KEY` est renseignée dans `.env`.
  7. **Interface Scanner & Enrichissement UI (`ScannerScreen.kt`, `ScannerViewModel.kt` & `web/js/app.js`)** :
     - Affichage d'un badge indiquant la source ayant identifié le produit (`Open Food Facts`, `Open Beauty Facts`, `Open Products Facts`, `Inventaire.io`, `Chomp API`, `Open-Product-Database`).
     - Pré-remplissage intelligent de l'unité/contenance, de l'emplacement, des notes (Nutri-Score, allergènes, auteur/éditeur) et ajout de codes-barres de démonstration multi-catégories.

---

### [BACKLOG-SCANNER-07] Bip Sonore, Verrouillage Anti-Rebond & Proposition Immédiate d'Ajout au Scan
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Émettre un bip lors de la lecture du code-barres et proposer immédiatement l'ajout du produit.
- **Spécifications** :
  1. **Bip Sonore & Signal Haptique** :
     - Émission d'un bip sonore de confirmation acoustique (`ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(ToneGenerator.TONE_PROP_BEEP, 160)`) et vibration haptique sur Android (`HapticFeedbackType.LongPress`).
     - Sur le Web : bip sonore via l'API standard Web Audio (`AudioContext` à `880 Hz`).
  2. **Verrouillage Anti-Rebond de Caméra** :
     - Dès qu'un code-barres est scanné (`scannedBarcode != null`), le processeur caméra ignore les frames suivantes pour ne pas relancer la recherche en boucle tant que l'utilisateur consulte la fiche.
  3. **Proposition Immédiate d'Ajout (Haute Priorité UI)** :
     - Affichage de la proposition d'ajout ou d'incrément `+1` tout en haut de l'écran avec scroll automatique immédiat.

---

### [BACKLOG-RECEIPT-08] Lecteur de Tickets de Caisse Numériques & Mise à Jour Assistée du Stock (Android & Web)
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Lecture des tickets numériques de magasin et mise à jour de l'inventaire.
- **Spécifications** :
  1. **Analyseur Intelligent de Tickets (`ReceiptParser.kt` & `parseWebReceiptText`)** :
     - Filtrage automatique des lignes d'entête magasin, totaux, montants CB, TVA, SIRET, remises et séparateurs.
     - Extraction des noms de produits et détection des multiplicateurs (`2 x 1,50 €`, `3 X`, etc.).
     - Rapprochement automatique (matching flou par tokens) avec les articles existants de l'inventaire pour distinguer les **réapprovisionnements (`+X en stock`)** des **nouveaux articles**.
  2. **Interface de Validation par Lot** :
     - Import de fichier (`.txt`, `.html`, `.eml`, `.csv`) ou copier-coller du ticket numérique avec prévisualisation cochable et ajustement des quantités avant validation en un clic.

---

### [BACKLOG-WEB-09] Clarification Modèle Économique (Web Gratuit sans Abonnement) & Hébergement Cloudflare
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Préciser que seule la version Web est sans abonnement, et clarifier l'hébergement Cloudflare avec données 100% locales.
- **Spécifications** :
  1. **Modèle économique transparent** :
     - Indiquer clairement dans le Hero, la section Téléchargement, la FAQ et le Footer que **seule la version Web est gratuite sans abonnement** (les applications mobiles stores incluent un essai gratuit de 14 jours puis un abonnement).
  2. **Transparence d'Hébergement & Confidentialité 100% Locale** :
     - Expliciter que le site statique est hébergé sur **Cloudflare Pages** pour une distribution rapide, mais qu'**aucune donnée d'inventaire ne transite ni n'est stockée sur Cloudflare ou un serveur distant** (stockage 100% local sur l'appareil via HTML5 File System Access / Room SQLite).

### [BACKLOG-CATEGORIES-13] Suppression et Gestion Complète des Magasins et Catégories (Android & Web)
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Impossibilité de supprimer les magasins et les catégories.
- **Spécifications** :
  1. **Couche Persistance & DAO (`InventoryDao.kt`, `ShoppingDao.kt`, `InventoryRepository.kt`)** :
     - Ajout des méthodes de suppression par entité et par ID (`deleteCategory`, `deleteCategoryById`, `deleteStoreType`, `deleteStoreTypeById`).
     - Requêtes de réassignation automatique sécurisée des articles existants (`reassignInventoryCategory`, `reassignShoppingCategory`, `reassignInventoryStore`, `reassignShoppingStore`) vers une catégorie de repli (ex. « Divers » / `autre`) ou un magasin de repli (ex. « Supermarché » / `supermarket`) pour éviter tout article orphelin.
  2. **ViewModel Android (`InventoryViewModel.kt`)** :
     - Ajout de `deleteCategory(category)` et `deleteStoreType(storeType)` avec contrôle d'intégrité (interdiction de supprimer la toute dernière catégorie ou le tout dernier magasin) et émission d'événements UI informatifs.
  3. **Interface Android (`SyncScreen.kt` & `ItemEditDialog.kt`)** :
     - Écran de sauvegarde / synchronisation : Section « Catégories & Magasins » enrichie d'onglets de bascule (`Catégories`, `Magasins`), liste visuelle complète avec compteurs d'articles associés, boutons de suppression avec dialogue de confirmation explicite, et boutons d'ajout.
     - Sélecteurs d'édition d'article (`CategoryPickerDialog`, `StorePickerDialog`) : possibilité de supprimer directement une catégorie/magasin inutile via un bouton corbeille dédié avec confirmation, et bouton d'ajout rapide.
  4. **Interface & Moteur Web (`web/index.html`, `web/js/app.js`, `web/js/i18n.js`)** :
     - Ajout du bouton « Catégories & Magasins » dans la barre d'outils et de la modale dédiée `#categories-modal`.
     - Gestion complète des suppressions avec réassignation automatique des articles et dialogues de confirmation en français, anglais et néerlandais.
     - Synchronisation dynamique du menu déroulant de sélection de catégorie dans la fiche produit.

### [BACKLOG-SCANNER-STORE-14] Mode Magasin (Labo Live des 4 Options + Tout-en-un)
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Pouvoir scanner les articles directement au magasin pendant les courses et tester en direct les 4 options de comportement.
- **Spécifications** :
  1. **Option 1 (`CHECK_OWNED` — Style Bullix / Grocy)** : Bandeau haute visibilité indiquant immédiatement si l'article est déjà en stock (🟢 quantité + emplacement), en stock bas (🟠) ou en rupture / sur la liste de courses (🔴).
  2. **Option 2 (`INSTANT_RESTOCK` — Style Bring! Express 1-Tap)** : Le scan incrémente automatiquement le stock (`+1` ou quantité demandée sur la liste), coche l'article sur la liste de courses et propose un bouton `↩ Annuler`.
  3. **Option 3 (`CART_BATCH` — Style Scan & Go Caddie)** : Cumul des scans dans un tiroir « Mon Caddie en cours » avec contrôles `-`/`+` et validation groupée au passage en caisse.
  4. **Option 4 (`NUTRI_DETAIL` — Style Open Food Facts)** : Fiche détaillée mettant en avant le Nutri-Score (A–E), les allergènes et la source.
  5. **Mode 5 (`ALL_IN_ONE` — Par défaut)** : Vue unifiée combinant les 4 options sur un seul écran.

### [BACKLOG-RECIPES-15] Assistant Recettes : Screenshots d'Ingrédients (OCR) & Liens Web
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Pouvoir envoyer des captures d'écran d'ingrédients ou des liens de recettes complètes et ajouter automatiquement les ingrédients manquants aux courses (avec validation).
- **Spécifications** :
  1. **OCR Local sur Capture d'Écran (`ML Kit Text Recognition`) & Partage Android (`ACTION_SEND`)** : Import depuis la galerie ou partage direct d'une capture d'écran (`image/*`) ou d'un lien (`text/plain`) vers Needix.
  2. **Extraction Web (`Schema.org/Recipe` JSON-LD + Fallback HTML)** : Analyse directe des URLs de recettes (Marmiton, 750g, CuisineAZ, blogs).
  3. **Croisement Intelligent avec le Stock & Validation 1-Clic** : Séparation visuelle entre ingrédients **🔴 Manquants** (pré-cochés automatiquement) et **🟢 Déjà en stock** (non cochés), avec ajustement des quantités (`-`/`+`) avant ajout à la liste de courses.

---

## 2. Historique des Versions Livrées

- **v0.7.0 (Mode Magasin 4 Options Live + Assistant Recettes OCR & URL)** :
  - Livraison de `[BACKLOG-SCANNER-STORE-14]` : Scanner Mode Magasin avec sélecteur interactif en direct des 4 options (`CHECK_OWNED`, `INSTANT_RESTOCK`, `CART_BATCH`, `NUTRI_DETAIL`) + mode combiné `ALL_IN_ONE`, contrôle du flash caméra et tiroir Caddie sur Android (`ScannerScreen.kt`, `ScannerViewModel.kt`, `ShoppingScreen.kt`) et Web (`web/index.html`, `web/app/index.html`, `web/js/app.js`).
  - Livraison de `[BACKLOG-RECIPES-15]` : Assistant Recettes complet (`RecipeIngredientParser.kt`) supportant l'OCR local sur captures d'écran d'ingrédients (ML Kit Text Recognition), l'extraction de recettes par lien URL (`Schema.org/Recipe` JSON-LD + fallback HTML), la cible de partage Android (`Intent.ACTION_SEND`), et la pré-sélection automatique des ingrédients manquants avec validation vers la liste de courses.
  - Harmonisation du versioning : `versionCode=19`, `versionName=0.7.0`.
- **v0.6.4 (Gestion Catégories & Magasins, Fix Signature CI)** :
  - Livraison de `[BACKLOG-CATEGORIES-13]` : Suppression complète et sécurisée des catégories et magasins (Android & Web) avec réassignation automatique de repli des articles vers `autre` (Divers) et `supermarket` (Supermarché) afin d'éviter tout article orphelin.
  - Garde-fou d'intégrité interdisant la suppression du dernier élément restant.
  - Ajout des boîtes de dialogue de suppression interactive dans `SyncScreen.kt` et `ItemEditDialog.kt` sur Android, ainsi que dans la modale `#categories-modal` sur le Web.
  - Rétablissement de la signature Debug CI avec support direct du secret GitHub `DEBUG_KEYSTORE_BASE64` et régénération de secours via `keytool`.
  - Harmonisation du versioning : `versionCode=10`, `versionName=0.6.4`.
- **v0.6.3 (Patch build)** :
  - Livraison de `[BACKLOG-BUILD-11]` : Hotfix du script Gradle `app/build.gradle.kts` (import `java.util.Base64` et décodage MIME sécurisé), transmission des variables secrètes de signature pour `assembleRelease`, intégration du Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.properties`), ajout du workflow automatisé `.github/workflows/update-gradle-wrapper.yml` et enrichissement des règles `/learn` dans `AGENTS.md`.
  - Livraison de `[BACKLOG-BUILD-10]` : Keystore persistant universel (`debug.keystore.base64`), configuration symétrique de signature Debug/Release dans `app/build.gradle.kts` et `.github/workflows/android-build.yml`, suppression de la régénération aléatoire de clés en CI, incrément du `versionCode=9` et publication conjointe d'APKs installables directement sans nécessiter de désinstaller la version précédente.
  - Livraison de `[BACKLOG-SCANNER-07]` : Bip sonore de confirmation au scan, verrouillage anti-rebond et affichage prioritaire immédiat de la proposition d'ajout au stock.
  - Livraison de `[BACKLOG-RECEIPT-08]` : Lecteur de tickets de caisse numériques (e-mail, texte, fichier) avec extraction intelligente des produits et mise à jour assistée du stock sur Android et Web.
  - Livraison de `[BACKLOG-WEB-09]` : Transparence sur l'hébergement Cloudflare Pages (données 100% locales) et précision que seule la version Web est gratuite sans abonnement.
  - Mise à niveau de l'ensemble des GitHub Actions vers les versions natives Node.js 24 (`checkout@v7`, `upload-artifact@v7`, `setup-java@v6`, `setup-node@v7`, `gradle/actions@v6`).
- **v0.6.2** :
  - Livraison de `[BACKLOG-SCANNER-06]` : Lecteur de codes-barres universel Multi-API (Open Food Facts v2, Open Beauty Facts v2, Open Products Facts v2, Inventaire.io ISBN, Chomp Food API v2) et base locale hors-ligne (`Open-Product-Database`) sur Android et Web.
  - Correctifs de sécurité des dépendances transitives Gradle (`Netty 4.1.139.Final`, `BouncyCastle 1.86`, `Guava 33.7.2-jre`, `JDOM2 2.0.6.1`, `Commons Lang3 3.21.0`, `jose4j 0.9.7`, `HttpClient 4.5.14`, `OkHttp 4.12.0`).
- **v0.6.1** :
  - Correction des pipelines CI/CD (`android-build.yml`, `cloudflare-pages.yml`, signature debug AGP).
  - Suppression de la boucle de redirection Web et du sous-dossier redondant `web/Needix/` au profit de `https://needix.pages.dev/`.
  - Exécution complète de `JURIDIQUE-01`, `CLEANUP-03` et `WEB-05` (passage 100% commercial, retrait de F-Droid/FOSS/Apache 2.0).
  - Correction des bugs logiques Android (`ShoppingRepository.updateStore`, `InventoryRepository.importItems`, restauration JSON câblée, interopérabilité JSON/CSV Android ↔ Web, cycle de vie `BarcodeScanner` / `cameraExecutor`).
- **v0.6.0** :
  - Intégration de la suite de tests unitaires automatisés bloquants dans le pipeline GitHub Actions (`.github/workflows/android-build.yml`).
  - Passage à Java 21 (Temurin) pour la compatibilité avec le SDK Android 36 et Robolectric.
  - Déploiement manuel : compilation de l'App Bundle `.aab` et de l'archive Web rattachés automatiquement aux Releases GitHub.
  - Rédaction du guide de build manuel (`BUILD_MANUAL_GUIDE.md`).

