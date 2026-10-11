# Needix v0.6.0 — Tests Automatisés, Déploiement Manuel & Pipeline de Release

Plan d'implémentation pour le cahier des charges **v0.6.0** de Needix : automatisation des tests unitaires, blocage strict de la chaîne de compilation en cas d'échec, suppression des déploiements automatisés vers les stores tiers, publication des artefacts finaux (`.aab` et archive Web) dans les Releases GitHub, et documentation des commandes de build manuelles.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Synthèse des décisions confirmées pour la version v0.6.0** :
> - **Numérotation sémantique** : Adoption officielle de la version **v0.6.0** (versionCode `6`, versionName `0.6.0`) dans `version.properties` et synchronisation sur l'ensemble de la documentation et des interfaces.
> - **Politique de tests sur GitHub Actions** : Exécution automatique de l'ensemble des tests unitaires (`./gradlew testDebugUnitTest`) à chaque push/PR sur la branche principale. Le pipeline échoue immédiatement et bloque la suite du build si le moindre test unitaire est en échec.
> - **Déploiement manuel via GitHub Releases** :
>   - Annulation du déploiement direct automatisé vers les stores (Google Play Store, App Store).
>   - En cas d'événement de publication d'une Release (`release: types: [published]`), la chaîne exécute les tests, compile le bundle de production Android (`.aab`), package l'application Web Cloudflare (`needix-web-v0.6.0.zip`), et attache automatiquement ces deux artefacts finaux aux assets de la Release GitHub pour téléchargement et téléversement manuel par le Lead Développeur.
> - **Optimisation Green IT maintenue** : Maintien strict de la directive `paths-ignore` sur le workflow (`.github/workflows/**`, `**.md`, `LICENSE`, `docs/**`) pour éviter la consommation inutile de serveurs cloud lors de modifications textuelles.
> - **Architecture unifiée (100% Commercial)** : Aucun flavor Gradle résiduel, code source unique `commonMain`/Android, intégration commerciale RevenueCat (période d'essai 14 jours puis abonnement), export/import local universel (CSV/JSON), et application Web avec boutons PayPal et déverrouillage par clé de licence.

---

## 1. Overview & Core Concept

- **Produit** : Needix v0.6.0 — Application de gestion d'inventaire domestique, prévention du gaspillage, alertes de réapprovisionnement et listes de courses intelligentes.
- **Objectif de la release v0.6.0** : Fiabiliser la qualité du code par une suite de tests unitaires exécutée en intégration continue, tout en simplifiant la distribution par un mécanisme de release manuelle contrôlée (fichiers `.aab` et bundle Web mis à disposition dans les Releases GitHub).
- **Règles d'indépendance** : Données locales souveraines exportables et importables à tout moment (formats ouverts CSV et JSON), sans dépendance propriétaire bloquante.

---

## 2. Technical Architecture & Modifications

### A. Pipeline GitHub Actions (`.github/workflows/android-build.yml`)
1. **Étape de Tests Unitaires Obligatoire** :
   - Ajout d'une étape dédiée `run: ./gradlew testDebugUnitTest --continue` ou standard sans tolérance d'erreur.
   - En cas de test défaillant, le job s'arrête avec code retour non nul, bloquant toute compilation ultérieure et alertant l'équipe.
2. **Conditionnement de la Release** :
   - Déclencheur : `release: types: [published]` ou `workflow_dispatch`.
   - Les tests s'exécutent en amont.
   - Compilation du bundle Android : `./gradlew bundleRelease`.
   - Archivage de la version Web Cloudflare (`web/`) sous format `.zip` ou `tar.gz`.
   - Téléversement direct des deux paquets dans les assets de la Release GitHub via l'action standard `gh release upload` ou `softprops/action-gh-release@v2`.
3. **Optimisation Green IT & Cache** :
   - Maintien du cache Gradle Temurin Java 17.
   - Préservation des filtres `paths-ignore`.

### B. Commandes de Build Manuel (Récapitulatif Officiel)
Les commandes suivantes sont documentées et prêtes à l'emploi en local ou en environnement CI :

| Cible / Action | Commande Gradle / Shell | Description |
| :--- | :--- | :--- |
| **Exécution des Tests** | `./gradlew testDebugUnitTest` | Exécute tous les tests unitaires et génère le rapport HTML dans `app/build/reports/tests/` |
| **Génération Debug APK** | `./gradlew assembleDebug` | Produit le fichier `app/build/outputs/apk/debug/app-debug.apk` pour tests sur appareil |
| **Génération Production AAB** | `./gradlew bundleRelease` | Produit le fichier `app/build/outputs/bundle/release/app-release.aab` signé pour Google Play |
| **Vérification Complète** | `./gradlew check` | Lance lint et tests unitaires |
| **Packaging Web (Wasm/Static)** | `zip -r needix-web-v0.6.0.zip web/` | Archive l'application Web Cloudflare Pages prête pour upload |
| **Déploiement Web Direct** | `npx wrangler pages deploy web/ --project-name=needix` | Déploie manuellement la version web sur Cloudflare Pages |

### C. Gestion des Versions & Alignement Projet
- Fichier `version.properties` :
  ```properties
  versionCode=6
  versionName=0.6.0
  ```
- Fichier `metadata.json` : mise à jour de la description pour refléter Needix v0.6.0 (maintien impératif de la capacité `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` et du nom `Needix`).
- Fichier `web/index.html` et `web/Needix/index.html` : mise à jour des mentions de version vers `v0.6.0`.

### D. Suite de Tests Unitaires (`app/src/test/`)
- Vérification et consolidation de la couverture des tests unitaires existants :
  - `InventoryAppUnitTest.kt` : validation du cycle de vie des articles d'inventaire, des seuils de réapprovisionnement, des transitions vers la liste de courses, et de la validation des formats d'export/import CSV et JSON.
  - Test de vérification de la période d'essai (14 jours) et de la validation de licence web.

---

## 3. Plan de Réalisation Étape par Étape

1. **Mise à jour des propriétés de version** :
   - Basculer `version.properties` sur `versionCode=6` et `versionName=0.6.0`.
   - Ajuster les badges et libellés dans `metadata.json` et les pages Web.
2. **Mise à niveau du workflow GitHub Actions (`.github/workflows/android-build.yml`)** :
   - Insérer l'étape d'exécution des tests automatisés `./gradlew testDebugUnitTest` avant toute étape de compilation binaire.
   - Configurer le packaging conditionnel de la release (production du `.aab` et de l'archive Web).
   - Configurer l'action d'attachement aux Releases GitHub (`gh release` ou upload d'artefacts).
3. **Création du guide d'instructions de compilation et déploiement manuel (`DEPLOYMENT_GUIDE_v0.6.0.md`)** :
   - Fournir les commandes précises pas-à-pas pour la console Android Google Play, la gestion des certificats, et l'hébergement Cloudflare Pages.
4. **Vérification et exécution des tests unitaires** :
   - Lancer l'exécution des tests via Gradle pour valider la suite de tests unitaires du projet.
   - S'assurer de la bonne compilation globale avec `compile_applet`.
