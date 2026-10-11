# Needix V4 — Compose Multiplatform (Desktop, Android, iOS), Export Standard, Monétisation Hybride & Pipeline Green IT

Plan stratégique et architectural pour faire évoluer Needix en une application multiplateforme moderne (Android, Desktop JVM Windows/macOS/Linux et préparation iOS), complètement indépendante des services cloud propriétaires, traduisible, optimisée pour l'éco-conception CI/CD (Green IT) et distribuée selon un modèle économique hybride.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Synthèse des décisions et arbitrages confirmés pour la V4** :
> - **Optimisation GitHub Actions (Green IT & Release) [Nouveau]** :
>   - Définition d'une règle `paths-ignore` stricte sur les déclencheurs de build push / PR (`**.md`, `.github/workflows/**`, `docs/**`, `LICENSE`) afin d'éviter le démarrage superflu de VM cloud lors de simples modifications de documentation ou du workflow lui-même.
>   - Génération du package lourd **Android App Bundle (.aab)** restreinte **exclusivement** à l'événement de publication d'une nouvelle "Release" (`on: release: types: [published]`). Les pushes courants ne compilent que les APK de vérification pour économiser quotas et empreinte carbone.
> - **Alignement du Plugin KSP & Éradication du Bug NullPointer [Nouveau]** :
>   - Vérification et alignement strict de la version de `ksp` avec la version exacte du compilateur Kotlin (`kotlin = "2.2.10"` dans `libs.versions.toml`).
>   - Résolution définitive de l'avertissement / NullPointerException lors du traitement des annotations Room via KSP causé par une discordance de métadonnées de bytecode.
> - **Multiplateforme (Objectif 1 V4)** : Structuration en **Compose Multiplatform (KMP)** avec `commonMain` (modèles d'inventaire, logique métier, UI Compose partagée), `androidMain` (spécificités Android, SAF, Caméra) et `desktopMain` (application JVM pour Windows/macOS/Linux, sélecteur de fichiers de bureau).
> - **Export & Import Universel FOSS (Objectif 2 V4)** : Suppression de toute dépendance propriétaire cloud (Google Sheets API supprimée). Implémentation `expect/actual` d'un gestionnaire d'export/import **CSV** (éditable sur tout tableur) et **JSON** (sauvegarde & restauration complète sans perte) :
>   - `androidMain` : Utilisation du **Storage Access Framework (SAF)** natif (`ACTION_CREATE_DOCUMENT` et `ACTION_OPEN_DOCUMENT`).
>   - `desktopMain` : Utilisation de boîtes de dialogue de sélection de fichiers natives (`JFileChooser`).
> - **Modèle Économique Hybride (Objectif 3 V4)** :
>   - Variante `foss` (F-Droid) : 100% libre et gratuite, sans code propriétaire, avec écran "Soutenir le projet" (liens vers GitHub Sponsors, Liberapay, Ko-fi / Buy Me a Coffee, PayPal).
>   - Variante `commercial` (Google Play / Amazon) : Intégration RevenueCat, Paywall avec **14 jours d'essai gratuit** puis abonnement mensuel ou annuel, écran bloquant après expiration.
>   - Interface `BillingManager` isolant l'UI de la variante active.
> - **Internationalisation (i18n)** : Français par défaut, avec traductions exhaustives en Anglais (`values-en`) et Néerlandais (`values-nl`).
> - **Signature Persistante & Versioning** : Configuration Gradle avec Secret GitHub Actions Base64 et variables d'environnement, avec fallback automatique sur `debug.keystore`.
> - **Licence & Nommage** : Licence Apache 2.0 (`LICENSE`) et renommage complet en **Needix**.

## 1. Overview & Core Concept

- **What It Does**: Needix est un gestionnaire d'inventaire et de listes de courses domestique intelligent et multi-écrans. Il permet de suivre ses stocks en temps réel (alimentaire, entretien, pharmacie), de scanner les codes-barres, de recevoir des alertes préventives de seuil critique, de basculer automatiquement les ruptures en liste de courses, et d'exporter/importer ses données librement au format standard CSV/JSON sur ordinateur ou smartphone.
- **Target Audience / Persona**: Particuliers, familles, collocations et utilisateurs soucieux de la souveraineté de leurs données et de la réduction du gaspillage, souhaitant organiser leur maison sur mobile ou grand écran sans être enfermés dans un écosystème cloud propriétaire.
- **Key Value**: Indépendance totale des données (format CSV/JSON universel), disponibilité simultanée sur PC/Mac et smartphone Android/iOS, modèle open source transparent doublé d'une distribution commerciale pérenne, et chaîne de build sobre en ressources (Green IT).

## 2. User Experience & Visual Design

- **Key User Flows**:
  - *Flux Inventaire & Alertes (Mobile & Desktop)* : Consultation de la liste avec jauges de stock colorées, incrémentation/décrémentation rapide, alerte proactive dès qu'un seuil minimal est atteint avec proposition d'ajout aux courses.
  - *Flux Sauvegarde & Restauration Cloud-Agnostic* : Clic sur "Exporter CSV" ou "Sauvegarde JSON" -> Ouverture du sélecteur natif (SAF sur Android, explorateur de fichiers sur Desktop) -> Enregistrement local ou sur le cloud choisi par l'utilisateur (Drive, Nextcloud, NAS local).
  - *Flux FOSS "Soutenir le projet"* : Accès au menu dédié dans la variante F-Droid, affichage de boutons stylisés vers les plateformes de dons (GitHub Sponsors, Liberapay, Ko-fi, PayPal).
  - *Flux Paywall Commercial* : Suivi du décompte d'essai de 14 jours, affichage du mur de paiement RevenueCat avec options mensuelle/annuelle, déblocage instantané après souscription.
- **Visual Identity & Theme**:
  - *Aesthetic Direction* : Interface moderne, épurée et fonctionnelle respectant Material Design 3, avec forte lisibilité des indicateurs de stock et adaptation fluide aux écrans larges (Desktop).
  - *Color Palette & Mood* : Teintes primaires bleu ardoise / indigo profond (`#1E293B`, `#4F46E5`), accents émeraude pour les stocks suffisants (`#10B981`), ambre pour les niveaux bas (`#F59E0B`), rouge vif pour les ruptures (`#EF4444`).
  - *Typography & Hierarchy* : Typographie système claire, contrastes élevés pour les quantités et compteurs, badges de statut concis.
  - *Component Styling & Layout* : Cartes produit aérées, filtres par catégorie à défilement horizontal (mobile) ou panneau latéral (desktop), formulaires de saisie réactifs.
- **Interactive Feedback & Motion**: Animations douces d'ajustement de quantité, notifications snackbar lors des exports/imports réussis, dialogue de confirmation lors des transferts vers la liste de courses.

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 : Optimisation GitHub Actions (Green IT) & Stratégie de Distribution**
  - *Chosen Approach* : Filtrage systématique `paths-ignore` (`.github/workflows/**`, `**.md`, `LICENSE`, `docs/**`) pour les déclencheurs de build et séparation des tâches : build d'APK léger sur push régulier, et déclencheur spécifique `on: release: types: [published]` pour la compilation du bundle Google Play (`.aab`).
  - *Why* : Évite le gaspillage de dizaines de minutes de calcul serveur cloud, réduit l'empreinte carbone et accélère le feedback développeur.
  - *Alternatives Considered* : Générer systématiquement l'AAB et l'APK à chaque commit (rejeté car consommateur d'énergie et inutile avant une release publique).
- **Decision 2 : Alignement KSP et Résolution de l'avertissement NullPointer**
  - *Chosen Approach* : Calage explicite de la version de KSP sur le canal de release officiel correspondant à la version de Kotlin installée (`kotlin = "2.2.10"`).
  - *Why* : KSP nécessite une synchronisation stricte avec l'AST du compilateur Kotlin. Une incompatibilité de version entraîne des exceptions internes (NPE) ou des avertissements de compatibilité de symboles lors de l'analyse Room.
  - *Alternatives Considered* : Ignorer l'avertissement (rejeté car risque de corruption de génération de code DAO lors des futures compilations).
- **Decision 3 : Architecture KMP / Compose Multiplatform avec cibles Android et Desktop JVM**
  - *Chosen Approach* : Structuration en code partagé `commonMain` pour la logique métier, la gestion d'inventaire et les écrans Compose, complété par `androidMain` (fonctionnalités mobiles Android) et `desktopMain` (lanceur JVM pour PC).
  - *Why* : Permet de réutiliser 100% de la logique d'inventaire et des règles métier tout en fournissant une application native de bureau sans latence web.
  - *Alternatives Considered* : Développement d'une web app séparée (rejeté car nécessite un hébergement web et dilue l'approche locale/hors-ligne).
- **Decision 4 : Export CSV et JSON via expect/actual (SAF Android & JFileChooser Desktop)**
  - *Chosen Approach* : Création d'une interface abstraite `FileStorageManager` implémentée via le Storage Access Framework sur Android et `JFileChooser` sur Desktop.
  - *Why* : Élimine le vendor lock-in Google Sheets tout en permettant à l'utilisateur d'enregistrer où il le souhaite (localement, Google Drive, Nextcloud, Nextcloud/Synology via SAF).
  - *Alternatives Considered* : API Google Sheets propriétaire (rejeté conformément au cahier des charges FOSS et indépendance cloud).
- **Decision 5 : Isolation des Flavors FOSS et Commerciale**
  - *Chosen Approach* : Flavors Gradle `foss` et `commercial` avec dépendance RevenueCat exclusive à `commercial`, masquée derrière l'interface commune `BillingManager`.
  - *Why* : Respecte les critères stricts d'éligibilité F-Droid (zéro binaire propriétaire) tout en monétisant légitimement l'application sur Google Play et Amazon.
  - *Alternatives Considered* : Deux dépôts de code distincts (rejeté car source de divergences et maintenance lourde).

## 4. Technical Architecture & Data Strategy (Technical Reference)

- **Architecture & Component Diagram**:
```
┌────────────────────────────────────────────────────────────────────────┐
│                        Needix Architecture (KMP)                       │
└────────────────────────────────────────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    ▼                               ▼                               ▼
┌──────────────────────┐┌──────────────────────┐┌──────────────────────┐
│     androidMain      ││     desktopMain      ││       iosMain        │
│ • Android Activity   ││ • Desktop JVM Window ││ • Préparation UIKit  │
│ • CameraX / Barcode  ││ • JFileChooser Dialog││ • Swift bridge stub  │
│ • SAF Storage (act.) ││ • Native Storage(act)││                      │
└──────────┬───────────┘└──────────┬───────────┘└──────────┬───────────┘
           │                       │                       │
           └───────────────────────┼───────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          commonMain (Partagé)                          │
│                                                                        │
│  ┌───────────────────────┐  ┌───────────────────────┐                  │
│  │     Domaine / UI      │  │     Moteur Export     │                  │
│  │ • InventoryViewModel  │  │ • CsvExportEngine     │                  │
│  │ • ShoppingViewModel   │  │ • JsonBackupEngine    │                  │
│  │ • Compose UI Screens  │  │ • FileStorageManager  │                  │
│  └───────────────────────┘  └───────────────────────┘                  │
│                                  ▲                                     │
│                                  │ (expect/actual)                     │
│  ┌──────────────────────────────────────────────────┐                  │
│  │       Abstraction de Monétisation Hybride        │                  │
│  │ • BillingManager (Interface)                     │                  │
│  │   ├── foss: FossBillingManager (Donations UI)    │                  │
│  │   └── commercial: RevenueCatBilling (Paywall 14j)│                  │
│  └──────────────────────────────────────────────────┘                  │
└────────────────────────────────────────────────────────────────────────┘
                                   ▲
                                   │
┌────────────────────────────────────────────────────────────────────────┐
│                   CI/CD & Toolchain (Green IT)                         │
│  • GitHub Actions: paths-ignore (.github, *.md, docs)                  │
│  • Release Trigger: Génération .aab conditionnée aux Releases          │
│  • KSP & Kotlin 2.2.10: Alignement strict des versions (zéro NPE)      │
└────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `InventoryItem` : Identifiant, nom, catégorie, quantité, unité, seuil minimal d'alerte, code-barres, emplacement, magasin préféré, date de mise à jour.
  - `ShoppingItem` : Identifiant, nom de produit, quantité à acheter, statut (à acheter, en attente, acheté), magasin cible.
  - `Category` & `StoreType` : Métadonnées de classement avec libellé et émoji.
  - `ExportFormat` : Sérialisation CSV (colonnes normalisées) et JSON (arbre complet des données).
- **Interactive Component & State Mapping**:
  - `InventoryScreen` : Gestion de l'état des filtres, recherche plein texte, déclenchement de la boîte de dialogue d'édition et boutons rapides de gestion de stock.
  - `ShoppingScreen` : Gestion des onglets par magasin, marquage des achats effectués, réintégration en inventaire.
  - `ExportDialog` : Sélection du format (CSV lisible ou sauvegarde JSON complète) et invocation de `FileStorageManager.exportFile()`.
  - `ImportDialog` : Sélection de fichier et prévisualisation avant réinjection des éléments dans la base.
  - `PaywallScreen` (variante commercial) : Affichage des formules mensuelles/annuelles et blocage après la période de 14 jours.
  - `SupportProjectScreen` (variante foss) : Boutons d'ouverture des liens de don externes.
- **Key Sequences & Integrations**:
  - `Export Sequence` : Déclenchement UI -> Génération de la chaîne CSV ou JSON par le moteur `commonMain` -> Appel de `FileStorageManager.saveFile(data, mimeType, defaultName)` -> Prise en charge native par SAF (Android) ou dialogue Swing (Desktop) -> Enregistrement sécurisé.
  - `Import Sequence` : Déclenchement UI -> Appel de `FileStorageManager.pickFile(mimeType)` -> Lecture du flux -> Parsing CSV/JSON -> Validation et insertion avec gestion des conflits.
  - `CI/CD Release Sequence` : Push sur tag/release -> Compilation `.aab` signée -> Publication dans les assets GitHub / store. Pour tout autre push de code -> simple vérification APK. Si modification markdown/workflow -> exécution ignorée.
