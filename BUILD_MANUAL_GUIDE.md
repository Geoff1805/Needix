# Needix v0.6.0 — Guide de Déploiement Manuel & Commandes de Build

Ce document récapitule les commandes exactes à exécuter en local ou via terminal pour compiler, tester et générer les paquets de production de l'application **Needix v0.6.0** (Android Bundle et Web App Cloudflare).

---

## 1. Exécution des Tests Automatisés (Bloquants)

Tous les tests unitaires (couvrant la logique métier, l'accès aux abonnements RevenueCat, et la cohérence de l'interface) doivent valider avec succès avant toute publication :

```bash
# Exécuter l'ensemble de la suite de tests unitaires
./gradlew testDebugUnitTest

# Ou avec la commande Gradle globale :
gradle :app:testDebugUnitTest
```

Les rapports HTML détaillés sont générés dans :
`app/build/reports/tests/testDebugUnitTest/index.html`

---

## 2. Génération Manuelle du Paquet Android (.aab)

Pour générer le fichier de production Google Play (`.aab`) signé :

```bash
# 1. Compiler l'App Bundle en mode Release
./gradlew bundleRelease

# Ou avec gradle système :
gradle :app:bundleRelease
```

Le fichier `.aab` produit se trouve dans :
`app/build/outputs/bundle/release/app-release.aab`

> **Note signature** : Assurez-vous d'avoir configuré votre Keystore dans les variables d'environnement (`STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) ou dans le fichier de propriétés dédié.

---

## 3. Génération & Packaging de la Web App (Cloudflare Pages)

La Web App et la Landing Page Needix intégrant le SDK PayPal et le déverrouillage par clé de licence sont situées dans le dossier `/web/Needix`.

Pour préparer l'archive de déploiement manuel :

```bash
# Créer une archive tar.gz prête pour le téléversement Cloudflare Pages
tar -czf needix-web-v0.6.0.tar.gz -C web/Needix .

# Ou pour déployer directement via la CLI Wrangler (Cloudflare) :
npx wrangler pages deploy web/Needix --project-name=needix
```

---

## 4. Pipeline GitHub Actions Automatisé (`android-build.yml`)

Le pipeline est optimisé Green IT (`paths-ignore` sur markdown et workflows) et opère selon la logique suivante :
1. **Job 1 (`unit-tests`)** : Exécute automatiquement `testDebugUnitTest` sur chaque push vers `main` et pull request. Tout échec bloque immédiatement la suite de la chaîne de CI/CD.
2. **Job 2 (`package-and-release`)** :
   - Se déclenche uniquement après le succès complet de `unit-tests`.
   - Lors d'une nouvelle release publiée sur GitHub (`release: [published]`) ou d'un déclenchement manuel (`workflow_dispatch`), compile le `.aab`, archive la Web App et les attache automatiquement à la **Release GitHub**.
   - Vous pouvez ensuite télécharger les fichiers finaux directement depuis l'onglet **Releases** de votre dépôt GitHub pour les distribuer sur les stores.
