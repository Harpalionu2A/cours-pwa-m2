# TD A0 — Sa PWA en TWA (45 min)

Jour 4 — lundi 2 novembre 2026. Point de départ : la **PWA n°1 du TP du 12 octobre**, en ligne, en HTTPS. Aucun fichier de Carnet n'est touché : ce TD porte sur la PWA de l'étudiant.

## Objectif

Empaqueter la PWA n°1 en APK (**Trusted Web Activity**, outil **Bubblewrap**), l'installer sur son téléphone, et contrôler l'installation : si l'APK s'installe, le JDK, le SDK et `adb` sont en place pour la suite de la journée.

## Fonctionnalités attendues

- [ ] 1. `bubblewrap init --manifest https://<pwa>/manifest.json` : un projet Android généré dans `twa/` à partir du manifest de la PWA
- [ ] 2. `bubblewrap build` : un keystore généré (**mot de passe noté quelque part**), un `app-release-signed.apk`
- [ ] 3. `adb install app-release-signed.apk` : l'icône de la PWA sur l'écran d'accueil
- [ ] 4. `assetlinks.json` avec l'empreinte SHA-256 de la clé, déployé dans `https://<pwa>/.well-known/assetlinks.json`
- [ ] 5. L'icône et l'écran de démarrage viennent du manifest (`icons`, `background_color`, `theme_color`)

## Validation

- L'icône est sur l'écran d'accueil ; l'app s'ouvre **sans barre d'adresse** (plein écran)
- `https://<pwa>/.well-known/assetlinks.json` répond (`200`, `application/json`)
- Le generator Digital Asset Links de Google valide le couple site / app : [developers.google.com/digital-asset-links/tools/generator](https://developers.google.com/digital-asset-links/tools/generator)
- `adb shell pm get-app-links <packageId>` → `verified` pour l'hôte de la PWA

## L'échec à vivre

Un **constat**, pas une panne :

1. Tant que `assetlinks.json` manque (ou que l'empreinte est fausse), l'app s'ouvre avec **la barre d'adresse de Chrome** : Chrome ne fait pas confiance à l'APK (il n'a pas la preuve que le site et l'app ont le même propriétaire) et affiche la PWA comme un onglet personnalisé.
2. Une fois le fichier déposé : icône native, plein écran… **et rien de plus qu'avant**. Pas de stockage sans quota d'éviction, pas d'arrière-plan garanti, pas de capteurs, pas de notification reçue par l'OS app fermée. La TWA est la PWA, affichée par Chrome.

## La réparation

- Déposer `assetlinks.json` dans `.well-known/` à la racine du site et redéployer (Netlify : le `netlify.toml` du cours a déjà la redirection `/.well-known/*`). Vérifier l'URL dans un navigateur, puis `adb shell pm verify-app-links --re-verify <packageId>` et `adb shell pm get-app-links <packageId>`.
- Pour le reste (stockage, arrière-plan, capteurs, notifications) : c'est le programme des TD A1 à A7.

## Aide

`twa-manifest.example.json` et `assetlinks.example.json` dans ce dossier.

```bash
# Empreinte SHA-256 de la clé générée par bubblewrap build
keytool -list -v -keystore android.keystore -alias android -storepass <mot de passe>
#   SHA256: AA:BB:CC:…
```

```json
// .well-known/assetlinks.json (sur la PWA)
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "app.netlify.ma_pwa.twa",
    "sha256_cert_fingerprints": ["AA:BB:CC:…"]
  }
}]
```

Bubblewrap génère ce fichier : `bubblewrap fingerprint generateAssetLinks` (dans le dossier `twa/`).

Netlify sert les fichiers de `.well-known/` tels quels si le dossier est dans le site déployé ; `netlify.toml` :

```toml
[[redirects]]
from = "/.well-known/*"
to = "/.well-known/:splat"
status = 200
```

## Commandes

```bash
npm install -g @bubblewrap/cli@1.13.2          # une fois (annexe setup)
bubblewrap doctor                               # JDK et SDK trouvés

mkdir twa && cd twa
bubblewrap init --manifest https://<pwa>/manifest.json
#   → questions : domaine, nom, package (app.netlify.ma_pwa.twa), icône, couleurs,
#     keystore : android.keystore, alias android, DEUX mots de passe à noter
bubblewrap build
#   → app-release-signed.apk, app-release-bundle.aab

adb install app-release-signed.apk              # ou : bubblewrap install
adb shell pm get-app-links <packageId>          # avant assetlinks.json : none / legacy_failure

# déposer .well-known/assetlinks.json sur la PWA, redéployer, puis :
adb shell pm verify-app-links --re-verify <packageId>
adb shell pm get-app-links <packageId>          # → verified
```

**Ce que le TD prouve :** la PWA du 12 octobre est installable comme une app Android, en 45 minutes, sans une ligne de Kotlin — et la frontière entre les deux est exactement la liste des capacités que l'OS n'accorde qu'aux apps (slide « Ce que l'OS accorde », Jour 4, chapitre 2).

**Corrigé :** aucun code ; les deux fichiers `*.example.json` de ce dossier.
