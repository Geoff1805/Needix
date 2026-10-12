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

## 4. Keystore Persistant Intégré (Zéro Désinstallation)

Le dépôt Needix intègre désormais un Keystore persistant sous forme encodée (`debug.keystore.base64` qui restaure automatiquement `debug.keystore`) :
1. **Identité de signature garantie** : Tous les builds (locaux ou générés par GitHub Actions, en Debug comme en Release par défaut) utilisent désormais cette même clé cryptographique persistante (SHA-256 : `48:F2:EB:6E:AE:9B:11:D4:C4:74:EE:B0:BE:AD:1D:2E:51:59:16:27:6C:18:B8:2F:AB:8E:60:DB:77:AC:A4:90`).
2. **Mises à jour directes sans désinstallation** : Vous pouvez installer les nouveaux APKs directement par-dessus les versions existantes sans perdre vos données locales ni devoir désinstaller l'application au préalable.
3. **Transition unique requise** : Si votre téléphone contient encore une version antérieure générée avant cette mise en place (signée avec une clé temporaire différente), vous devez la désinstaller **une toute dernière fois**. Toutes les versions suivantes s'installeront ensuite directement sans aucune désinstallation.
4. **Signature officielle Google Play (optionnelle)** : Si vous configurez les secrets décrits aux étapes 1 à 3, les builds de Release utiliseront votre clé privée officielle.
