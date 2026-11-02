# Needix — Règles Projet
> Hérite de `~/.gemini/config/AGENTS.md` et `PROJECTS_CACHE.md`.
- **CI/CD First (`Geoff1805/Needix`)** : Tests CI, Dependabot et `.github/workflows/update-gradle-wrapper.yml` (canal `stable` GA + SHA-256) sur GitHub Actions ; suivi et correction immédiate des runs.
- **Sync GitHub (`main`)** : Commit & push immédiat en Conventional Commits (`feat:`, `fix:`, `ci:`, `chore:`, `docs:`) via `git` ou MCP `github-mcp-server`.
- **Signature & Versioning** : APKs Debug/Release signés via Keystore persistant (`Needix-v<versionName>.apk`). SSoT dans `version.properties` + historique `BACKLOG.md`.
- **Stack** : KMP + Compose M3, Room/SQLite, 100% local offline-first.
