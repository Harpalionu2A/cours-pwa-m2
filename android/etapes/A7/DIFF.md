# Étape A7 — Position et photo

État du projet Carnet **à la fin du TD A7** (Jour 5, chapitre 6) = état « fin de Jour 5 » (contrat § 6.3), base de `android/tp-exemple/` et squelette du TP. Par rapport à `android/etapes/A6/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/ui/permissions/PermissionFlow.kt` — `enum PermissionState { NotAsked, Granted, DeniedOnce, DeniedForever }` ; `class PermissionFlow(activity, permissions, requireAll, …)` : `state`, `request()`, `refresh()`, `openSettings()` (`ACTION_APPLICATION_DETAILS_SETTINGS`) ; l'état est calculé depuis `checkSelfPermission`, `shouldShowRequestPermissionRationale` et un drapeau « déjà demandée » (`rememberSaveable`) ; `rememberPermissionFlow(vararg permissions, requireAll)` rattache le launcher (`RequestMultiplePermissions`) et recalcule l'état à chaque `ON_RESUME` (révocation dans les réglages).
- `app/src/main/java/corsica/universita/carnet/data/location/LocationSource.kt` — `class LocationSource(context)` : `suspend fun current(): Fix?` (`FusedLocationProviderClient.getCurrentLocation`, `PRIORITY_HIGH_ACCURACY` si précise accordée, `BALANCED_POWER_ACCURACY` si approximative seulement, `CancellationToken` lié à la coroutine).
- `app/src/main/java/corsica/universita/carnet/data/camera/PhotoStore.kt` — `newFile()` dans `filesDir/photos/`, `uriFor(path)` par `FileProvider`.
- `app/src/main/java/corsica/universita/carnet/ui/notes/CameraCapture.kt` — CameraX `Preview` (`PreviewView` dans `AndroidView`) + `ImageCapture.takePicture` vers le fichier ; lié au cycle de vie de l'écran ; `unbindAll` à la sortie.
- `app/src/main/java/corsica/universita/carnet/ui/notes/NoteDetailScreen.kt` — `NoteDetailScreen(id, onBack, onShare)` : texte, date, position (lat/lng + précision du dernier relevé, précise/approx), photo (`BitmapFactory`, `inSampleSize = 4`, décodée en IO) ; `PermissionAction(flow, …)` : un bouton par état de la machine (demander · rationale puis demander · réglages · l'action) ; partage et suppression dans la barre.
- `app/src/main/java/corsica/universita/carnet/ui/notes/NoteDetailViewModel.kt` — `note: StateFlow<Note?>` (dérivé de `repository.notes`), `locate()`, `attachPhoto(path)`, `delete()`, `lastFix`, `locating` ; `factory(id)`.
- `app/src/main/res/xml/file_paths.xml` — `<files-path name="photos" path="photos/" />`.
- `app/schemas/…/4.json`.

## Fichiers modifiés

- `AndroidManifest.xml` — `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` ; `<uses-feature android.hardware.camera required=false>` ; `<provider androidx.core.content.FileProvider authorities="${applicationId}.fileprovider" exported=false grantUriPermissions=true>`.
- `data/local/NoteEntity.kt` — `lat: Double?`, `lng: Double?`, `photoPath: String?` (colonnes NULL).
- `data/local/CarnetDatabase.kt` — `version = 4`, `MIGRATION_3_4` (trois `ALTER TABLE … ADD COLUMN`).
- `data/Note.kt`, `data/NotesRepository.kt` — les trois champs, conversions.
- `CarnetApp.kt` — `CarnetContainer.locationSource`, `CarnetContainer.photoStore`.
- `ui/CarnetNavigation.kt` — `@Serializable data class NoteDetail(val id: Long) : NavKey` ; `entry<NoteDetail>` ; `openNoteId` du `NotesViewModel` (clic sur une notification) pousse `NoteDetail(id)` sur le backstack.
- `ui/notes/NotesListScreen.kt` — `onOpen: (Note) -> Unit` : une ligne ouvre le détail ; la mise en évidence de l'étape A6 est remplacée par la navigation.
- `ui/notes/NoteIntents.kt` — `shareNote` : avec une photo, `image/jpeg` + `EXTRA_STREAM` (URI du FileProvider) + `FLAG_GRANT_READ_URI_PERMISSION` ; la position est ajoutée au texte.
- `res/values/strings.xml` — détail, position, photo, rationales, réglages ; `res/xml/.gitkeep` supprimé.

## Ce que fait Carnet à la fin

Une note s'ouvre en détail ; elle peut porter une position (précise ou approximative selon ce que l'utilisateur a accordé, avec sa précision) et une photo (CameraX, dans le répertoire privé) ; le partage envoie le texte, la position et la photo ; le flux de permission tient jusqu'au refus définitif (rationale avant la deuxième demande, bouton « Ouvrir les réglages » ensuite) et redemande proprement après une révocation dans les réglages. **Carnet est le squelette du TP** : manifest, icône adaptive, splash, Room v4 migrée, outbox WorkManager, notification FCM, deux permissions jusqu'au refus définitif, App Link vers sa PWA.

Ce qui reste au Jour 6 (`tp-exemple`) : signature release, R8, Crashlytics, tests, CI.

## Compilation

`./gradlew assembleDebug --no-daemon --console=plain --max-workers=1` → `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk`.
