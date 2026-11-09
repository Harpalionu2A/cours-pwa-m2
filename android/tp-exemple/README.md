# Carnet « produit » — référence pour le TP Android (Jour 6)

Le projet Carnet à la fin du TD A7 (Jour 5) — copie de `android/etapes/A7/`, inchangée —, plus ce que le Jour 6 ajoute pour en faire un produit : **signature release**, **R8**, **Crashlytics**, **un test unitaire et un test Compose**, **CI GitHub Actions**, **App Distribution** (optionnel). C'est l'état attendu du socle du TP, sans la capacité native au choix.

## Ce qui s'ajoute par rapport à `android/etapes/A7/`

| Fichier | Ajout |
|---|---|
| `app/build.gradle.kts` | `signingConfigs.release` lu dans `local.properties` (repli : clé debug) ; `release { isMinifyEnabled = true; isShrinkResources = true }` ; `versionCode = 7`, `versionName = "1.0.0"` ; plugins Crashlytics et App Distribution, `firebase-crashlytics` (BoM 34.19.0) ; dépendances de test |
| `app/proguard-rules.pro` | règles `keep` : kotlinx.serialization, Retrofit, entités Room, attributs pour Crashlytics |
| `app/src/test/…/NotesViewModelTest.kt` | le ViewModel testé sur la JVM (`runTest`, Turbine) : le vrai `NotesRepository` sur un `NoteDao` en mémoire, un `SyncApi` d'écho et un faux `WorkManager` ; 4 tests |
| `app/src/test/…/FakeWorkManager.java`, `FakeContext.kt` | les faux nécessaires : `WorkManager` compte les demandes de sync (en Java : son constructeur est `internal` en Kotlin) ; `Context` minimal pour construire `Settings` |
| `app/src/androidTest/…/NoteFormTest.kt` | le formulaire testé avec `createComposeRule` (appareil ou émulateur) |
| `.github/workflows/android.yml` | copie de `android/ci/android.yml` : `test` + `assembleRelease` + artefact APK |
| `gradle/libs.versions.toml` | plugins `firebase-crashlytics` 3.0.8, `firebase-appdistribution` 5.3.0 ; `coroutines-test`, `turbine`, `ui-test-junit4` |

## `local.properties` (jamais commité)

```properties
sdk.dir=/chemin/vers/le/sdk
PWA_HOST=ma-pwa.netlify.app                 # App Link (TD A3) ; défaut exemple.netlify.app
SYNC_BASE_URL=https://httpbin.org/          # l'écho de l'outbox (TD A5), surchargeable
CRASHLYTICS_UPLOAD_MAPPING=false            # true quand google-services.json est le vrai

# Signature release (Jour 6) — valeurs d'EXEMPLE, à remplacer par les siennes
RELEASE_STORE_FILE=release.jks              # relatif à la racine du projet ; *.jks est dans .gitignore
RELEASE_STORE_PASSWORD=exemple-mot-de-passe
RELEASE_KEY_ALIAS=carnet
RELEASE_KEY_PASSWORD=exemple-mot-de-passe
```

Sans `RELEASE_STORE_FILE`, la release est signée avec la clé **debug** du cours : l'APK s'installe, mais n'a pas d'identité « produit ».

## Générer son keystore (une fois, à garder : Jour 4, chapitre 1)

```bash
keytool -genkeypair -v -keystore release.jks -alias carnet -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Commandes

```bash
./gradlew assembleDebug                      # l'APK de développement (≈ 27 Mo)
./gradlew test                               # les tests JVM (NotesViewModelTest : 4 tests)
./gradlew connectedDebugAndroidTest          # le test Compose, sur un appareil ou un émulateur
./gradlew assembleRelease                    # R8 + signature → app-release.apk (≈ 3,3 Mo) + mapping.txt
./gradlew appDistributionUploadRelease       # optionnel : FIREBASE_SERVICE_CREDENTIALS=compte-de-service.json
```

## CI (GitHub Actions)

Secrets du dépôt : `RELEASE_KEYSTORE_B64` (`base64 -w0 release.jks`), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `PWA_HOST`. Le workflow lance `./gradlew test assembleRelease`, vérifie la signature et publie `app-release.apk` + `mapping.txt` en artefact.

## Firebase

`google-services.json` est celui du projet Firebase du cours (`cours-pwa-b0358`), `applicationId` `corsica.universita.carnet` — le même pour tout le module. Crashlytics n'envoie `mapping.txt` que si `CRASHLYTICS_UPLOAD_MAPPING=true` (valeur par défaut ; fichier réel requis — `false` tant que `google-services.json` est factice).

## Base Room

`app/schemas/…/1.json` à `4.json` : l'historique des versions de la base (TD A4 à A7), versionné ; `CarnetDatabase` version 4, migrations `1→2`, `2→3`, `3→4` écrites.
