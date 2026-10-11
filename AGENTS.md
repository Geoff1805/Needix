# Règles d'Agent — Projet Needix

## Publication automatique sur GitHub
- **Publication systématique** : Toutes les modifications de code autorisées dans le projet Needix (`Geoff1805/Needix`, branche `main`) doivent être automatiquement publiées (commit & push) sur GitHub dès qu'elles sont appliquées localement.
- **Méthode de publication** : En l'absence du binaire `git` local, utiliser directement le serveur MCP GitHub (`github-mcp-server` via l'outil `push_files` ou `create_or_update_file`) vers `owner: "Geoff1805"`, `repo: "Needix"`, `branch: "main"`, avec un message de commit clair au format Conventional Commits (`feat:`, `fix:`, `ci:`, `chore:`, `docs:`).

## Conventions du Projet
- **Langue** : Toujours répondre en français de manière concise avec des liens cliquables `file:///` vers les fichiers et symboles modifiés.
- **Souveraineté & Green IT** : Maintenir l'architecture 100 % locale sans dépendance cloud propriétaire obligatoire.
