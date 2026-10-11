# Guide de Migration iOS & Compose Multiplatform — Needix

Ce document présente l'architecture et la feuille de route technique pour étendre **Needix** vers iOS via **Kotlin Multiplatform (KMP)** et **Compose Multiplatform**, en complément de l'application Android et de la version Web (Wasm) sur Cloudflare Pages.

---

## 1. État Actuel du Projet
- **Langage** : Kotlin 2.2.x
- **Interface Utilisateur** : Jetpack Compose (Material 3)
- **Persistance Locale** : Room Database 2.7.0 (Android SQLite)
- **Réseau** : Retrofit 2.12 + OkHttp + Moshi (Open Food Facts API)
- **Matériel / Caméra** : CameraX + ML Kit Barcode Scanning
- **Stockage de Fichiers** : Storage Access Framework (SAF) Android
- **Web** : Version Web (Wasm/HTML5) sans installation hébergée sur Cloudflare Pages (`/Needix`)

---

## 2. Dépendances à Remplacer pour le Support Multiplateforme

| Composant | Dépendance Android Actuelle | Alternative Kotlin Multiplatform Recommandée |
| :--- | :--- | :--- |
| **Interface Graphique** | `androidx.compose.*` | **Compose Multiplatform** (`org.jetbrains.compose`) |
| **Base de Données** | `androidx.room.*` | **Room Multiplatform (2.7.0+)** ou **SQLDelight** |
| **Réseau / HTTP** | `Retrofit` + `OkHttp` | **Ktor Client** (`io.ktor:ktor-client-core`, `darwin`) |
| **Sérialisation** | `Moshi` | **kotlinx.serialization** (`kotlinx-serialization-json`) |
| **Images** | `Coil` | **Coil 3 (Multiplatform)** (`io.coil-kt.coil3:coil-compose`) |
| **Système de Fichiers** | Android `Uri` & SAF | `kotlinx-io` / `okio` avec abstraction `expect/actual` |
| **Cycle de vie / ViewModel** | AndroidX ViewModel | `androidx.lifecycle.viewmodel.compose` (compatible KMP) |
| **Caméra / Code-Barres** | CameraX + Google ML Kit | `expect/actual` (AVFoundation / Vision sur iOS) |

---

## 3. Structure Cible des Dossiers (KMP)

```
needix/
├── build.gradle.kts
├── settings.gradle.kts
├── composeApp/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/              # Code partagé (95% du projet)
│       │   └── kotlin/
│       │       ├── model/           # Modèles de données
│       │       ├── repository/      # Gestionnaires de données (Room KMP / Ktor)
│       │       ├── export/          # Moteur CSV / JSON
│       │       ├── billing/         # Abstraction BillingManager
│       │       └── ui/              # Écrans Compose partagés
│       ├── androidMain/             # Spécificités Android
│       │   └── kotlin/
│       │       ├── MainActivity.kt
│       │       ├── export/          # Implémentation SAF
│       │       └── camera/          # Implémentation CameraX
│       ├── iosMain/                 # Spécificités iOS
│       │   └── kotlin/
│       │       ├── MainViewController.kt
│       │       ├── export/          # UIDocumentPickerViewController
│       │       └── camera/          # AVFoundation Scanner
│       └── wasmJsMain/              # Spécificités Web (Wasm)
│           └── kotlin/
│               ├── main.kt
│               └── export/          # File System Access API
└── web/                             # Site vitrine & Cloudflare Pages (/Needix)
```

---

## 4. Feuille de Route de Migration iOS

1. **Phase 1 : Extraction du module partagé `commonMain`**
   - Remplacer les entités Room Android par des entités Room Multiplatform ou SQLDelight.
   - Remplacer Retrofit par Ktor Client.
   - Remplacer Moshi par `kotlinx.serialization`.

2. **Phase 2 : Abstraction des fonctionnalités spécifiques à la plateforme**
   - **Sélecteur de fichiers pour export/import** :
     - Android : Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).
     - iOS : `UIDocumentPickerViewController`.
     - Web (Wasm) : File System Access API (`showOpenFilePicker` / `showSaveFilePicker`).
   - **Scanner de code-barres** :
     - Android : CameraX + ML Kit.
     - iOS : `AVCaptureSession` + framework `Vision` natif Apple.

3. **Phase 3 : Interface utilisateur Compose partagée**
   - Adapter les écrans Compose existants (`InventoryScreen`, `ShoppingScreen`, `SupportProjectScreen`, `PaywallScreen`) vers Compose Multiplatform.
   - Intégrer la vue iOS via `ComposeUIViewController { App() }` dans un projet Xcode.
