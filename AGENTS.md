# Règles d'Agent — Projet Needix

## Publication automatique sur GitHub
- **Publication systématique** : Toutes les modifications de code autorisées dans le projet Needix (`Geoff1805/Needix`, branche `main`) doivent être automatiquement publiées (commit & push) sur GitHub dès qu'elles sont appliquées localement.
- **Méthode de publication** : En l'absence du binaire `git` local, utiliser directement le serveur MCP GitHub (`github-mcp-server` via l'outil `push_files` ou `create_or_update_file`) vers `owner: "Geoff1805"`, `repo: "Needix"`, `branch: "main"`, avec un message de commit clair au format Conventional Commits (`feat:`, `fix:`, `ci:`, `chore:`, `docs:`).

## Délégation Maximale vers GitHub Actions (CI/CD First)
- **Règle absolue : Ce qui peut être fait par GitHub doit être fait sur GitHub.**
- **Standards obligatoires** :
  1. **Tests automatiques** : Workflows CI systématiques.
  2. **Sécurité & Dépendances** : Dependabot (`.github/dependabot.yml`) et scanners de sécurité.
  3. **Mise à jour automatique Gradle (GA stable)** : Workflow de mise à jour automatique du Gradle Wrapper (`.github/workflows/update-gradle-wrapper.yml`) ciblant le canal `stable` (GA uniquement), avec SHA-256 et `./gradlew` dynamique.
  4. **Suivi proactif des GitHub Actions** : Toujours suivre les exécutions de CI/CD sur GitHub Actions et corriger immédiatement toute erreur détectée (`/learn`).
  5. **Signature systématique des APKs** : Veiller à ce que tous les APKs produits (Debug et Release) soient systématiquement signés avec le Keystore persistant (ou la clé officielle en production) afin d'assurer des mises à jour directes sans désinstallation (`/learn`).
  6. **Numérotation et Versioning Univoques (/learn)** :
     - **Source unique de vérité** : La version de l'application est strictement définie dans `version.properties` (`versionCode` entier séquentiel, `versionName` SemVer `X.Y.Z`).
     - **Zéro valeur en dur** : Les scripts CI/CD, workflows, compilations Gradle et noms d'archives doivent dériver dynamiquement leur version depuis `version.properties` ou les variables d'environnement (`VERSION_NAME`, `VERSION_CODE`), sans jamais inscrire de numéro de version figé dans le code.
     - **Traçabilité stricte** : Chaque incrément de version doit être consigné dans `BACKLOG.md` (Section « Historique des Versions Livrées ») avec la liste précise des fonctionnalités/correctifs livrés, et synchronisé avec les tags/releases GitHub (`vX.Y.Z`).

## Conventions du Projet
- **Langue** : Toujours répondre en français de manière concise avec des liens cliquables `file:///` vers les fichiers et symboles modifiés.
- **Souveraineté & Green IT** : Maintenir l'architecture 100 % locale sans dépendance cloud propriétaire obligatoire.

## Normalisation des Noms d'APK et Zéro ZIP Superflu pour TOUTES les Applications (/learn)
- **Règle absolue (/learn)** : Pour **TOUTES les applications existantes et TOUTES les nouvelles applications futures sans exception** (Android natif, Kotlin Multiplatform, Flutter, React Native, etc., incluant `InstallAPK`, `Bullix`, `Needix`, `AntigravityController`, etc.) :
  1. **Nommage normalisé strict `<AppName>.apk` dès la création** :
     - Dès l'initialisation, le premier commit ou la mise en place de la CI/CD de **toute nouvelle application**, l'APK produit et distribué doit porter exclusivement et obligatoirement le nom exact de l'application : **`<AppName>.apk`** (ex: `InstallAPK.apk`, `Bullix.apk`, `Needix.apk`, `AntigravityController.apk`).
     - Proscription formelle et absolue des noms par défaut des frameworks ou suffixes hétérogènes (`app-release.apk`, `app-debug.apk`, `-release-apk`, `-debug-apk`, `Needix-Debug-APK`, etc.) dans les artefacts et les releases.
  2. **Zéro ZIP superflu (Binaire APK brut directement installable)** :
     - Les APKs doivent être mis à disposition directement sous leur format `.apk` natif sans aucune compression ou encapsulation zip superflue.
     - Dans les GitHub Releases, le fichier `.apk` doit être immédiatement cliquable et téléchargeable depuis un smartphone pour une installation en un clic via `InstallAPK` ou le navigateur.
     - Dans les workflows CI/CD (`.github/workflows/*.yml`), toujours copier ou renommer l'APK compilé vers `build-artifacts/<AppName>.apk` avant les étapes d'upload d'artefact et de publication de release.
  3. **Détection automatique et intégration dans InstallAPK** :
     - Toute nouvelle application Android/mobile doit être compatible et immédiatement reconnue par `InstallAPK`.
     - `InstallAPK` trie les artefacts GitHub prioritairement par **date de création (`created_at`) décroissante** afin de toujours garantir l'installation du build le plus récent.
     - Les téléchargements depuis les dépôts privés s'effectuent via l'endpoint de l'API GitHub (`/releases/assets/{id}`) avec l'en-tête `Accept: application/octet-stream` pour éviter toute erreur 404.
