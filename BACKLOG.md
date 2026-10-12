# Needix — Backlog & Spécifications de Développement

Ce document centralise l'état d'avancement, le registre des décisions et les tâches en attente du produit **Needix**.

---

## 1. Backlog Actif

### [BACKLOG-JURIDIQUE-01] Correction Juridique : Passage en Licence Propriétaire
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Pivot commercial 100%.
- **Spécifications** :
  1. Remplacement intégral du fichier `LICENSE` à la racine par la mention de droits d'auteur stricte :
     ```text
     Copyright (c) 2026 Geoff1805. All Rights Reserved. 
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

### [BACKLOG-PERF-04] Optimisation Green IT & Écoconception Logicielle
- **Statut** : `Terminé / Livré`
- **Origine** : Directive système — Architecture locale souveraine & Green IT.
- **Spécifications** :
  1. Mise en place du cache Gradle natif GitHub Actions (`actions/setup-java@v6` avec `cache: gradle`) divisant par deux le temps CPU runner.
  2. Filtrage granulaire des déclencheurs CI (`paths-ignore` sur markdown, docs, workflows et web).
  3. Réduction de l'empreinte mémoire SQLite avec pagination et indexation sur les dates de péremption (`InventoryDao.kt`).
  4. Minimisation des recalculs Compose via `derivedStateOf` et `remember`.

---

### [BACKLOG-WEB-05] Modernisation & Réparation Intégrale de la Web App
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Alignement fonctionnalités Web et Android.
- **Spécifications** :
  1. Rétablissement du nom de domaine personnalisé `https://needix.app/` via `wrangler.toml` et suppression de la redirection 301 défectueuse vers `/Needix/`.
  2. Implémentation du lecteur de code-barres HTML5 (`html5-qrcode`) directement sur la Web App.
  3. Sauvegarde locale persistante sans serveur (`localStorage` + export/import JSON/CSV compatible avec Android).
  4. Support multilingue temps réel (Français, Anglais, Néerlandais).

---

### [BACKLOG-SCANNER-06] Lecteur Code-Barres Multi-API & Base Locale Hors-Ligne
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Couverture maximale des produits alimentaires, cosmétiques, livres et articles généraux.
- **Spécifications** :
  1. **Cascade de résolutions Multi-API** :
     - Open Food Facts API v2 (alimentaire).
     - Open Beauty Facts API v2 (cosmétiques et hygiène).
     - Open Products Facts API v2 (articles généraux, bazar, entretien).
     - Inventaire.io API (ISBN livres).
     - Chomp Food API v2 (alimentaire international avec clé API).
  2. **Base de données embarquée hors-ligne (`open_product_database.json`)** :
     - 400+ références populaires préchargées dans les assets Android et la Web App pour un scan instantané sans connexion réseau.
  3. **Tolérance aux pannes réseau & mode hors-ligne** :
     - Repli silencieux et immédiat vers la saisie manuelle si le code-barres n'est pas répertorié.

---

### [BACKLOG-SCANNER-07] Bip Sonore, Anti-Rebond & Proposition d'Ajout Prioritaire au Scan
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Feedback auditif immédiat et confirmation fluide lors du scan de code-barres.
- **Spécifications** :
  1. **Feedback auditif (Bip scanner)** :
     - Émission d'un son net et instantané à la détection réussie d'un code-barres (ToneGenerator système sur Android, Web Audio API Oscillator sur le Web).
  2. **Verrouillage anti-rebond (Debounce / Cooldown)** :
     - Délai de 1 200 ms empêchant les scans multiples accidentels du même code-barres sans bloquer les scans consécutifs de produits distincts.
  3. **Proposition d'ajout prioritaire** :
     - Affichage d'une boîte de dialogue modale élégante permettant de confirmer le produit, la quantité et la date de péremption avant ajout définitif.

---

### [BACKLOG-RECEIPT-08] Lecteur de Tickets de Caisse Numériques & Mise à Jour du Stock
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Intégration rapide des achats depuis des e-tickets (e-mails d'enseignes, fichiers PDF/TXT).
- **Spécifications** :
  1. **Moteur d'analyse textuelle intelligent** :
     - Extraction des lignes d'articles, quantités, prix et détection automatique des formats standards (Carrefour, Auchan, Leclerc, Colruyt, Delhaize, etc.).
  2. **Interface de validation assistée** :
     - Liste interactive des produits détectés avec cases à cocher pour sélection et ajustement des quantités avant validation vers l'inventaire.
  3. **Disponibilité multiplateforme** :
     - Déployé simultanément sur l'application Android (`ReceiptParser.kt`, `ReceiptParserDialog.kt`) et la Web App (`receiptParser.js`).

---

### [BACKLOG-WEB-09] Transparence Hébergement Cloudflare Pages & Gratuité Web App
- **Statut** : `Terminé / Livré`
- **Origine** : Demande utilisateur — Clarification tarifaire et politique de confidentialité des données.
- **Spécifications** :
  1. **Modèle économique transparent** :
     - Indiquer clairement dans le Hero, la section Téléchargement, la FAQ et le Footer que **seule la version Web est gratuite sans abonnement** (les applications mobiles stores incluent un essai gratuit de 14 jours puis un abonnement).
  2. **Transparence d'Hébergement & Confidentialité 100% Locale** :
     - Expliciter que le site statique est hébergé sur **Cloudflare Pages** pour une distribution rapide, mais qu'**aucune donnée d'inventaire ne transite ni n'est stockée sur Cloudflare ou un serveur distant** (stockage 100% local sur l'appareil via HTML5 File System Access / Room SQLite).

---

## 2. Historique des Versions Livrées

- **v0.6.3 (Patch build)** :
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
  - Suppression de la boucle de redirection Web et du sous-dossier redondant `web/Needix/` au profit de `https://needix.app/`.
  - Exécution complète de `JURIDIQUE-01`, `CLEANUP-03` et `WEB-05` (passage 100% commercial, retrait de F-Droid/FOSS/Apache 2.0).
  - Correction des bugs logiques Android (`ShoppingRepository.updateStore`, `InventoryRepository.importItems`, restauration JSON câblée, interopérabilité JSON/CSV Android ↔ Web, cycle de vie `BarcodeScanner` / `cameraExecutor`).
- **v0.6.0** :
  - Intégration de la suite de tests unitaires automatisés bloquants dans le pipeline GitHub Actions (`.github/workflows/android-build.yml`).
  - Passage à Java 21 (Temurin) pour la compatibilité avec le SDK Android 36 et Robolectric.
  - Déploiement manuel : compilation de l'App Bundle `.aab` et de l'archive Web rattachés automatiquement aux Releases GitHub.
  - Rédaction du guide de build manuel (`BUILD_MANUAL_GUIDE.md`).
