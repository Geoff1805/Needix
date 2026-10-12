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

## Conventions du Projet
- **Langue** : Toujours répondre en français de manière concise avec des liens cliquables `file:///` vers les fichiers et symboles modifiés.
- **Souveraineté & Green IT** : Maintenir l'architecture 100 % locale sans dépendance cloud propriétaire obligatoire.
