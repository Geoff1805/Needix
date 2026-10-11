# Needix v0.6.0 — Backlog & Spécifications du Pivot Commercial

Plan d'implémentation et suivi du backlog pour les évolutions juridiques, le nettoyage de marque, la résolution des anomalies d'icônes et la configuration du chemin de base Web sous le domaine dédié `needix.alsteens.net`.

---

## User Review & Décisions Clés

> [!IMPORTANT]
> L'ensemble des éléments ci-dessous a été consigné dans le backlog actif du projet (`BACKLOG.md`). Vous pouvez valider ce plan d'action pour ordonner le lancement des travaux de modification du code.

- **Décision 1 (Juridique)** : Passage immédiat en licence propriétaire exclusive avec droits d'auteur réservés à Geoffroy Alsteens, retrait de toute mention Apache 2.0.
- **Décision 2 (Branding)** : Nettoyage intégral de toute référence à Google AI Code tout en préservant les descripteurs internes requis par la plateforme.
- **Décision 3 (Monétisation & Stores)** : Retrait de F-Droid, GitHub Sponsors, Liberapay et Ko-fi au profit du modèle commercial unifié.
- **Décision 4 (Anomalie Doubles Icônes)** : Audit du manifest (intent-filter unique) et alignement des mipmaps pour éradiquer tout dédoublement dans le lanceur.
- **Décision 5 (Sous-domaine Web Dédié)** : Configuration de `<base href="/">`, élimination du dossier virtuel `/Needix/` dans l'URL pour servir l'application à la racine de `https://needix.alsteens.net/`.

---

## 1. Backlog Actif

### [BACKLOG-JURIDIQUE-01] Passage en Licence Propriétaire Stricte
- **Statut** : `À faire / Validé`
- **Objectif** : Supprimer l'open source et protéger formellement la propriété intellectuelle du logiciel.
- **Actions** :
  1. Remplacer intégralement le fichier `LICENSE` à la racine par :
     ```text
     Copyright (c) 2026 Geoffroy Alsteens. All Rights Reserved. 
     This software is proprietary and confidential. Unauthorized copying, modifying, merging, publishing, distributing, or selling of this software, via any medium, is strictly prohibited.
     ```
  2. Supprimer toute référence à la licence Apache 2.0 dans l'ensemble du projet :
     - `web/Needix/index.html` (footer et liens légaux).
     - `web/Needix/js/i18n.js` (`footer_text` en français, anglais et néerlandais).
     - Documentation associée (`BUILD_MANUAL_GUIDE.md`, etc.).

---

### [BACKLOG-BRANDING-02] Suppression de toute référence à Google AI Code
- **Statut** : `À faire / Validé`
- **Objectif** : Supprimer toute mention de Google AI Code dans le codebase et la documentation utilisateur.
- **Actions** :
  1. Audit de conformité dans l'ensemble des fichiers du projet.
  2. Conservation stricte des prérequis système AI Studio : maintien obligatoire de `"MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"` dans `metadata.json` sans exposition visuelle dans l'application.

---

### [BACKLOG-CLEANUP-03] Suppression des boutons et références à F-Droid, GitHub et Dons FOSS
- **Statut** : `À faire / Validé`
- **Objectif** : Aligner l'interface utilisateur et le code sur le modèle 100% commercial (abandon définitif des flux FOSS et des plates-formes de dons).
- **Actions** :
  1. **Landing Page Web (`web/Needix/`)** :
     - Retrait du badge de téléchargement F-Droid dans la grille des stores (`.fdroid-card`).
     - Retrait du lien GitHub dans le pied de page.
     - Retrait de la question FAQ n°4 (comparatif F-Droid vs version commerciale).
     - Retrait du tag de présentation "Modèle Open-Source & FOSS" dans la bannière hero.
  2. **Traductions (`web/Needix/js/i18n.js`)** :
     - Suppression des entrées `store_fdroid`, `faq_q4`, `faq_a4`, `hero_badge_foss` dans les langues FR, EN et NL.
  3. **Application Mobile Android (`app/`)** :
     - Suppression du switcher de test de variante "F-Droid / Play Store" dans l'écran de sauvegarde (`SyncScreen.kt`).
     - Retrait du bouton vers l'écran de dons et suppression de la navigation vers `SupportProjectScreen` (liens GitHub Sponsors, Liberapay, Ko-fi, PayPal).
     - Retrait des ressources de chaînes devenues obsolètes dans `res/values/strings.xml`, `values-en/strings.xml` et `values-nl/strings.xml`.
     - Consolidation de la suite de tests pour refléter le flux commercial exclusif.

---

### [BACKLOG-BUG-04] Correction du problème des doubles icônes (Lanceur Android / Web)
- **Statut** : `À faire / En attente d'arbitrage`
- **Origine** : Signalement utilisateur — présence d'une double icône constatée.
- **Diagnostic technique prévisionnel** :
  1. **Côté Android (Lanceur d'applications)** :
     - Vérifier qu'un seul intent filter avec `android.intent.action.MAIN` et `android.intent.category.LAUNCHER` est déclaré dans `AndroidManifest.xml` (aucune autre Activity ni `<activity-alias>` ne doit être exposée comme lanceur).
     - Harmoniser les ressources d'icônes adaptatives (`res/mipmap-anydpi-v26/ic_launcher.xml` et `ic_launcher_round.xml`) et les formats matriciels WebP/PNG (`mipmap-hdpi`, `mdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) afin d'éviter tout conflit de cache ou d'affichage dans le lanceur.
     - Nettoyer tout vestige d'ancien build ou de package d'émulateur pouvant résulter d'un changement d'applicationId ou de flavor antérieur.
  2. **Côté Web / Landing Page** :
     - Vérifier les balises favicons et le rendu du header (`brand-icon` vs logo texte) pour s'assurer qu'aucun dédoublement visuel n'apparaît dans la navigation.
- **Actions prévues** :
  - Audit du manifest et de la configuration des lanceurs.
  - Nettoyage et synchronisation rigoureuse des icônes d'application.
  - Validation sur build propre (`./gradlew assembleDebug`).

---

### [BACKLOG-WEB-05] Correction du chemin de base (Base Path) Web & Sous-domaine Dédié (needix.alsteens.net)
- **Statut** : `À faire / Validé`
- **Origine** : Demande utilisateur — Hébergement direct sur le sous-domaine `https://needix.alsteens.net`, suppression du sous-dossier `/Needix/` de la barre d'adresse.
- **Spécifications détaillées** :
  1. **Landing Page & Web App (`index.html`)** :
     - Intégration de la balise `<base href="/">` dans `<head>` de `index.html` (et `app/index.html`).
     - Suppression de tout préfixe `/Needix/` dans les routes relatives, liens de navigation et ancres (`/` au lieu de `/Needix/`).
     - Vérification et mise à jour de l'appel de tous les assets statiques depuis la racine :
       - CSS : `/css/style.css`
       - JavaScript : `/js/app.js`, `/js/i18n.js`
       - Données & Échantillons : `/sample-inventory.json`
       - Favicon et médias graphiques.
  2. **Code Kotlin Android & Multiplatform (`SyncScreen.kt`, strings.xml)** :
     - Remplacement de l'URL cible de la Web App de `https://needix.pages.dev/Needix/` vers `https://needix.alsteens.net/`.
     - Nettoyage des libellés de ressource :
       - `web_landing_button` : "Ouvrir la version Web" (suppression de la mention "(/Needix)").
       - `web_landing_desc` : "Accédez au site vitrine et à la version Web sans installation (https://needix.alsteens.net)..." (suppression de "(/Needix)").
       - Mêmes adaptations pour `values-en/strings.xml` et `values-nl/strings.xml`.
  3. **Configuration Cloudflare Pages & Déploiement** :
     - Adaptation de la configuration de déploiement (`web/_redirects`, `.github/workflows/cloudflare-pages.yml`) :
       - Suppression de la redirection 301 de `/` vers `/Needix/`.
       - Configuration pour servir directement le contenu du site et de l'app à la racine du domaine `needix.alsteens.net`.
     - Mise à jour de la documentation de build manuel (`BUILD_MANUAL_GUIDE.md`) et du packaging d'archive Web (`tar -czf needix-web-v0.6.0.tar.gz`).

---

## 2. Architecture Technique & Flux de Redirection Web

```
                    ┌────────────────────────────────────────┐
                    │      https://needix.alsteens.net/      │
                    └───────────────────┬────────────────────┘
                                        │ (Base Path: /)
                 ┌──────────────────────┴──────────────────────┐
                 ▼                                             ▼
    ┌────────────────────────┐                    ┌────────────────────────┐
    │     Landing Page       │                    │  Web App Sans Serveur  │
    │      index.html        │                    │     /app/index.html    │
    │  - <base href="/">     │                    │  - <base href="/">     │
    │  - Assets: /css/, /js/ │                    │  - File System Access  │
    │  - Checkout PayPal Web │                    │  - Déverrouillage Clé  │
    └────────────────────────┘                    └────────────────────────┘
```

---

## 3. Historique des Versions Livrées

- **v0.6.0 (Tests Automatisés & Déploiement Manuel)** :
  - Pipeline GitHub Actions durci avec étape de tests unitaires bloquante (`./gradlew testDebugUnitTest`).
  - Passage de l'environnement de build à **Java 21 (Temurin)** pour la compatibilité avec le SDK Android 36 et la sandbox Robolectric.
  - Déploiement manuel avec attachement automatique de l'AAB et de l'archive Web aux Releases GitHub.
  - Rédaction du guide de build manuel `BUILD_MANUAL_GUIDE.md`.
