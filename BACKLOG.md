# Needix — Backlog & Spécifications de Développement

Ce document centralise l'état d'avancement, le registre des décisions et les tâches en attente du produit **Needix**.

---

## 1. Backlog Actif

### [BACKLOG-JURIDIQUE-01] Correction Juridique : Passage en Licence Propriétaire
- **Statut** : `À faire / Validé` (En attente d'implémentation)
- **Origine** : Demande utilisateur — Pivot commercial 100%.
- **Spécifications** :
  1. Remplacement intégral du fichier `LICENSE` à la racine par la mention de droits d'auteur stricte :
     ```text
     Copyright (c) 2026 Geoff1805. All Rights Reserved. 
     This software is proprietary and confidential. Unauthorized copying, modifying, merging, publishing, distributing, or selling of this software, via any medium, is strictly prohibited.
     ```
  2. Suppression de toutes les mentions de la licence "Apache 2.0" dans l'ensemble du projet :
     - Landing page web (`web/Needix/index.html` : footer, liens de licence).
     - Dictionnaire d'internationalisation (`web/Needix/js/i18n.js` : `footer_text` en FR, EN, NL).
     - Documentation associée (`BUILD_MANUAL_GUIDE.md`, etc.).

---

### [BACKLOG-BRANDING-02] Suppression de toute référence à Google AI Code
- **Statut** : `À faire / Validé` (En attente d'implémentation)
- **Origine** : Demande utilisateur — Nettoyage d'environnement et de marque.
- **Spécifications** :
  1. Audit complet du codebase pour vérifier l'absence de mentions "Google AI Code" ou dérivés dans les commentaires, codes sources et documentations client.
  2. Préservation stricte de la directive système AI Studio : maintien obligatoire de la clé `"MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"` dans `metadata.json` (sans mentionner Google AI Code dans l'application finale).

---

### [BACKLOG-CLEANUP-03] Suppression des boutons et références à F-Droid, GitHub et Dons FOSS
- **Statut** : `À faire / Validé` (En attente d'implémentation)
- **Origine** : Demande utilisateur — Abandon définitif du canal FOSS / F-Droid suite au passage 100% commercial.
- **Spécifications** :
  1. **Landing Page (`web/Needix/index.html`)** :
     - Suppression de la carte de téléchargement `F-Droid` (`.store-badge-card.fdroid-card`).
     - Suppression du lien GitHub dans le footer de la page.
     - Suppression de la FAQ n°4 comparant la version F-Droid et la version commerciale.
     - Nettoyage du badge de hero "Modèle Open-Source & FOSS" (`hero_badge_foss`).
  2. **Traductions (`web/Needix/js/i18n.js`)** :
     - Suppression des clés et libellés `store_fdroid`, `faq_q4`, `faq_a4`, `hero_badge_foss` pour les 3 langues (FR, EN, NL).
  3. **Application Android (`app/src/main/`)** :
     - Suppression du sélecteur de variante "Tester la variante : F-Droid / Play Store" dans l'écran de synchronisation / sauvegarde (`SyncScreen.kt`).
     - Suppression de l'accès à l'écran de dons FOSS (`SupportProjectScreen.kt`) et des boutons de dons tiers (GitHub Sponsors, Liberapay, Ko-fi, etc.).
     - Unification du modèle économique sur le mode commercial RevenueCat (période d'essai 14 jours, abonnement, restauration).
     - Nettoyage des chaînes de caractères résiduelles associées (`strings.xml` fr, en, nl).

---

### [BACKLOG-BUG-04] Correction du problème des doubles icônes (Lanceur Android / Web)
- **Statut** : `À faire / En attente d'arbitrage`
- **Origine** : Demande utilisateur avec capture d'écran — signalement de double icône.
- **Spécifications & Diagnostic technique** :
  1. **Côté Android** :
     - Contrôle d'unicité de l'intent filter `android.intent.category.LAUNCHER` dans `AndroidManifest.xml` (garantir qu'une seule Activity possède l'entrée du lanceur).
     - Vérification de l'absence d'`<activity-alias>` résiduel issu d'anciennes variantes.
     - Harmonisation des icônes adaptatives vectorielles (`res/mipmap-anydpi-v26/ic_launcher.xml` et `ic_launcher_round.xml`) avec les drawables matriciels WebP pour prévenir tout dédoublement sur les launchers d'appareils physiques et émulateurs.
  2. **Côté Web / Landing Page** :
     - Vérifier la barre de navigation et le favicon pour éviter toute redondance d'affichage d'icône.

---

### [BACKLOG-WEB-05] Correction du chemin de base (Base Path) Web & Sous-domaine Dédié (needix.app)
- **Statut** : `À faire / Validé` (En attente d'implémentation)
- **Origine** : Demande utilisateur — Hébergement sur le sous-domaine dédié `https://needix.app`, suppression du sous-dossier `/Needix/`.
- **Spécifications** :
  1. **Fichiers HTML (`index.html`)** :
     - Ajouter / configurer la balise `<base href="/">` dans la section `<head>` pour faire pointer la racine vers `/` au lieu d'un sous-dossier.
     - S'assurer que tous les assets (CSS, JS, images, polices, favicons) sont appelés depuis la racine (`/css/style.css`, `/js/app.js`, `/js/i18n.js`, etc.) sans préfixe `/Needix/`.
  2. **Routing et Code Kotlin (Android & Multiplatform)** :
     - Vérifier la configuration des URLs dans le code Kotlin : mise à jour de l'URL cible de la Web App de `https://needix.pages.dev/Needix/` vers `https://needix.app/` (dans `SyncScreen.kt`).
     - Nettoyer les mentions de `(/Needix)` dans les libellés (`strings.xml` fr, en, nl : `web_landing_button`, `web_landing_desc`).
  3. **Configuration Cloudflare Pages & Redirections** :
     - Supprimer la redirection `_redirects` forçant `/` vers `/Needix/`.
     - Organiser ou déployer le contenu web directement à la racine pour servir `index.html` à la racine de `https://needix.app`.

---

## 2. Historique des Versions Livrées

- **v0.6.0** :
  - Intégration de la suite de tests unitaires automatisés bloquants dans le pipeline GitHub Actions (`.github/workflows/android-build.yml`).
  - Passage à Java 21 (Temurin) pour la compatibilité avec le SDK Android 36 et Robolectric.
  - Déploiement manuel : compilation de l'App Bundle `.aab` et de l'archive Web rattachés automatiquement aux Releases GitHub.
  - Rédaction du guide de build manuel (`BUILD_MANUAL_GUIDE.md`).
