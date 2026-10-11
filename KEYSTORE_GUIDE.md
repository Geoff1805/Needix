# Guide de Génération du Keystore Persistant & Secrets GitHub Actions — Needix

Pour permettre les mises à jour sans avoir à désinstaller l'ancienne application, votre clé de signature (Keystore) doit être persistante et identique à chaque build.

---

## 1. Générer le Fichier Keystore Localement

Exécutez cette commande dans votre terminal (Linux, macOS, ou Git Bash sur Windows) :

```bash
keytool -genkey -v \
  -keystore upload-keystore.jks \
  -alias needix-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass MonSuperMotDePasse123! \
  -keypass MonSuperMotDePasse123! \
  -dname "CN=Needix, OU=Mobile, O=NeedixApp, L=Paris, ST=IDF, C=FR"
```

> **Conseil** : Sauvegardez précieusement ce fichier `upload-keystore.jks` et vos mots de passe.

---

## 2. Encoder le Keystore en Base64

Pour injecter votre keystore en toute sécurité dans GitHub Actions sans versionner de fichier binaire :

### Sur Linux / macOS :
```bash
base64 -w 0 upload-keystore.jks > keystore_base64.txt
# Sur macOS si -w n'est pas reconnu :
# base64 -i upload-keystore.jks | tr -d '\n' > keystore_base64.txt
```

### Sur Windows (PowerShell) :
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Out-File -Encoding ASCII keystore_base64.txt
```

---

## 3. Configurer les Secrets dans GitHub Actions

Rendez-vous sur votre dépôt GitHub :  
**Settings** > **Secrets and variables** > **Actions** > **New repository secret**

Ajoutez les 4 secrets suivants :

| Nom du Secret | Valeur |
| :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Le contenu intégral copié depuis `keystore_base64.txt` |
| `STORE_PASSWORD` | Le mot de passe du keystore (`MonSuperMotDePasse123!`) |
| `KEY_ALIAS` | L'alias de la clé (`needix-upload`) |
| `KEY_PASSWORD` | Le mot de passe de la clé (`MonSuperMotDePasse123!`) |

---

## 4. Fonctionnement du Fallback Automatique

Le script `app/build.gradle.kts` est configuré avec un repli transparent :
1. **En CI / Production** : Si les variables d'environnement (`KEYSTORE_PATH` ou secret Base64) sont présentes, elles signent l'APK/AAB avec votre clé officielle.
2. **En Local / AI Studio** : Si aucune variable n'est configurée, Gradle bascule automatiquement sur `debug.keystore`, ce qui permet de continuer à développer sans erreur de compilation.
