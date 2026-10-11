# Architecture Compose Multiplatform Web (Wasm) & Cloudflare Pages — Needix

Ce document détaille l'architecture de la version WebAssembly (Wasm) de **Needix**, conçue pour s'exécuter directement dans le navigateur sans installation ni serveur backend, et déployée sur **Cloudflare Pages** sous le chemin `/Needix/`.

---

## 1. Vue d'Ensemble & Principes Green IT
- **Zéro Serveur Backend** : Aucune base de données distante, aucun coût d'hébergement serveur dynamique.
- **Souveraineté des Données** : Les données restent stockées sur l'ordinateur de l'utilisateur sous forme de fichier `JSON` standard.
- **Hébergement Statique Ultra-Performant** : Fichiers statiques et binaires Wasm servis mondialement via le CDN Cloudflare Pages.
- **Interopérabilité Mobile / Web** : L'inventaire exporté en JSON depuis l'application mobile Android/iOS s'ouvre directement dans la version Web.

---

## 2. Configuration Gradle Kotlin Multiplatform (wasmJs)

Pour compiler l'application via Compose Multiplatform vers WebAssembly, la cible `wasmJs` est déclarée dans le bloc `kotlin` :

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "needix"
        browser {
            val projectRootDir = project.rootDir.path
            val distDir = File(projectRootDir, "web/Needix/wasm")
            commonWebpackConfig {
                outputFileName = "needix.js"
                outputPath = distDir
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        wasmJsMain.dependencies {
            // Dépendances spécifiques au runtime Wasm / Browser
        }
    }
}
```

---

## 3. Abstraction de la File System Access API (Kotlin/Wasm)

Pour permettre la manipulation directe du fichier JSON local sans backend, l'API standard HTML5 `window.showOpenFilePicker` et `window.showSaveFilePicker` est interfacée via des déclarations externes en Kotlin :

```kotlin
package com.example.web.fs

import kotlin.js.Promise

external interface FileSystemHandle {
    val kind: String
    val name: String
}

external interface FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<org.w3c.files.File>
    fun createWritable(): Promise<FileSystemWritableFileStream>
}

external interface FileSystemWritableFileStream {
    fun write(data: String): Promise<Unit>
    fun close(): Promise<Unit>
}

external object window {
    fun showOpenFilePicker(options: dynamic): Promise<Array<FileSystemFileHandle>>
    fun showSaveFilePicker(options: dynamic): Promise<FileSystemFileHandle>
}
```

### Logique de Sauvegarde Directe :
1. **Ouverture** : `showOpenFilePicker()` renvoie un handle vers le fichier local.
2. **Lecture** : Le fichier est désérialisé en mémoire en objets Kotlin / JSON Needix.
3. **Édition** : L'utilisateur ajoute des articles, ajuste les stocks ou génère ses courses.
4. **Enregistrement** : `createWritable()` réécrit directement le JSON dans le fichier d'origine sans boîte de dialogue de téléchargement supplémentaire.

---

## 4. Déploiement Cloudflare Pages (`/Needix`)

### Structure des Dossiers Cloudflare :
```
web/
├── _headers               # En-têtes Wasm (application/wasm) et sécurité
├── _redirects             # Redirection / -> /Needix/ (301)
├── index.html             # Redirection immédiate vers /Needix/
├── Needix/
│   ├── index.html         # Landing page vitrine multilangue & Web App
│   ├── sample-inventory.json # Jeu de données d'exemple
│   ├── app/
│   │   └── index.html     # Application Web plein écran
│   ├── css/
│   │   └── style.css      # Design Material 3 responsive
│   └── js/
│       ├── i18n.js        # Dictionnaire FR / EN / NL
│       └── app.js         # Moteur File System Access API
└── wrangler.toml          # Configuration Cloudflare Pages
```

### Déploiement :
Le projet est configuré avec `pages_build_output_dir = "web"`. Tout commit sur `main` déploie instantanément le site vitrine et la Web App sur Cloudflare Pages.
