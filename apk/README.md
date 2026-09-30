# Millennium POS — application Android

Ce dépôt construit automatiquement l'APK **Millennium POS** avec GitHub Actions.
L'application ouvre la caisse `https://milleniumgrouphaiti.com/pos/caisse` en plein écran
et ajoute l'impression sur imprimantes thermiques **Bluetooth classique**.

Les améliorations publiées sur le site arrivent automatiquement dans l'application :
il n'est pas nécessaire de reconstruire l'APK à chaque changement du site.

## Contenu

| Fichier | Rôle |
|---|---|
| `capacitor.config.json` | Nom, identifiant (`ht.milleniumgroup.pos`), adresse de la caisse |
| `www/offline.html` | Écran affiché si l'application démarre sans internet |
| `assets/` | Icône et écran de démarrage |
| `native/` | Plugin Bluetooth classique (Java) |
| `scripts/prepare-android.mjs` | Autorisations, version, signature |
| `.github/workflows/create-keystore.yml` | Crée la clé de signature (une seule fois) |
| `.github/workflows/build-apk.yml` | Construit et publie l'APK |

## Mise en route (une seule fois)

1. **Secret du mot de passe** — *Settings → Secrets and variables → Actions → New repository secret*
   - Nom : `KEYSTORE_PASSWORD`
   - Valeur : un mot de passe fort (12 caractères minimum). **Notez-le en lieu sûr.**
2. **Créer la clé** — onglet *Actions* → « Créer la clé de signature (une seule fois) » → *Run workflow*.
3. À la fin, ouvrez l'exécution et téléchargez l'artefact **cle-millennium-pos** :
   - `millennium-pos.jks` : **gardez ce fichier précieusement** (clé USB + e-mail à vous-même). Sans lui, impossible de mettre à jour l'application sur le Play Store.
   - `KEYSTORE_BASE64.txt` : ouvrez-le et copiez tout son contenu.
4. **Secret de la clé** — nouveau secret `KEYSTORE_BASE64`, valeur : le contenu copié.
5. Supprimez l'artefact de la clé (bouton corbeille sur la page de l'exécution).

## Construire l'APK

Onglet *Actions* → « Construire l'APK » → *Run workflow* (ou envoyez une modification sur `main`).
Après 5 à 10 minutes, l'APK apparaît dans **Releases**. Lien permanent vers la dernière version :

```
https://github.com/VOTRE-COMPTE/millennium-pos-android/releases/latest/download/millennium-pos.apk
```

Sans les secrets, un APK **de test** est construit (onglet Actions → exécution → artefact), mais il n'est pas publié.

## Installer sur une tablette

Ouvrez le lien sur la tablette, téléchargez l'APK et autorisez « installer des applications inconnues »
quand Android le demande.

## Imprimante Bluetooth classique

1. Allumez l'imprimante et appairez-la dans *Réglages Android → Bluetooth* (code souvent `0000` ou `1234`).
2. Dans la caisse : menu ☰ → « Choisir l'imprimante ».
3. L'imprimante est mémorisée et reconnectée automatiquement.
