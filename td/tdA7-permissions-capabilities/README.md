# TD A7 — Position et photo (30 min + ce qui reste)

Jour 5 — jeudi 5 novembre 2026, chapitre 6. Point de départ : son projet à la fin du TD A6 (ou `android/etapes/A6/`).

## Objectif

Une note porte une position et une photo ; le flux de permission tient jusqu'au refus définitif et renvoie vers les réglages. À la fin : Carnet est le squelette du TP.

## Fonctionnalités attendues

- [ ] 1. `ui/notes/NoteDetailScreen.kt` : deuxième entrée de Navigation 3 (`@Serializable data class NoteDetail(val id: Long) : NavKey`), ouverte en touchant une note ; retour système
- [ ] 2. `ui/permissions/PermissionFlow.kt` : la machine à états (`NotAsked`, `Granted`, `DeniedOnce`, `DeniedForever`), rationale avant la deuxième demande, bouton « Ouvrir les réglages » après le refus définitif, état recalculé au retour au premier plan
- [ ] 3. `data/location/LocationSource.kt` : `FusedLocationProviderClient.getCurrentLocation` ; précise **ou** approximative selon ce que l'utilisateur accorde (les deux permissions demandées ensemble)
- [ ] 4. CameraX : `Preview` + `ImageCapture.takePicture` vers `filesDir/photos/` ; `FileProvider` déclaré (`res/xml/file_paths.xml`)
- [ ] 5. **Version 4** : colonnes `lat`, `lng`, `photoPath` (NULL), `Migration(3, 4)`
- [ ] 6. Partage de la note avec sa photo (`ACTION_SEND`, `image/jpeg`, `EXTRA_STREAM`, `FLAG_GRANT_READ_URI_PERMISSION`) — le bouton du TD A3, étendu

Manifest : `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `<uses-feature android.hardware.camera required="false">`, le `<provider>`.

## Validation

- Ouvrir une note, « Ajouter ma position » : le dialogue système (précise / approximative, une fois / toujours) ; la position s'affiche avec sa précision (« à 12 m près »)
- « Prendre une photo » : aperçu, capture, la photo s'affiche dans la note ; partager vers Messages : le texte et la photo arrivent
- **Refuser deux fois** (sans rationale, voir l'échec) : l'app propose les réglages au lieu de se taire
- Paramètres → Applications → Carnet → Autorisations → révoquer la caméra → revenir dans Carnet : le bouton redemande proprement
- `am kill` : la note garde sa position et sa photo ; `4.json` existe ; installation par-dessus la v3 sans perte

## L'échec à vivre

Sans `PermissionFlow` : un bouton qui appelle `launcher.launch(CAMERA)` directement.

1. Appuyer, **refuser**. Appuyer, **refuser** encore (Android 11+ : deux refus = « ne plus demander », sans case à cocher).
2. Appuyer une troisième fois : **aucun dialogue** ; le launcher répond `false` immédiatement. L'app croit à un bug, l'utilisateur croit à un bug. Rien ne la laissera plus jamais demander : `shouldShowRequestPermissionRationale` renvoie `false`, comme avant la toute première demande.

Bonus, la localisation : demander `ACCESS_FINE_LOCATION` seule — sur Android 12+, aucun dialogue : la demande est ignorée, le launcher répond `false`, Logcat : `ACCESS_FINE_LOCATION must be requested with ACCESS_COARSE_LOCATION`. Demander les deux ensemble et choisir « Approximative » dans le dialogue → `result[FINE] = false`, `result[COARSE] = true` : lire `false` comme un refus est l'erreur ; prendre ce qu'on donne.

## La réparation

La machine à états, à partir de ce que l'OS expose — `checkSelfPermission` (accordée maintenant ?), `shouldShowRequestPermissionRationale` (`true` après un premier refus ; `false` avant toute demande **et** après le refus définitif), et un drapeau « déjà demandée » à mémoriser soi-même (`rememberSaveable`) :

| État | Comment on le sait | Ce que fait le bouton |
|---|---|---|
| `NotAsked` | pas accordée, pas de rationale, jamais demandée | `launcher.launch(…)` |
| `DeniedOnce` | pas accordée, **rationale = true** | expliquer (dialogue), puis `launch` |
| `DeniedForever` | pas accordée, rationale = false, déjà demandée | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` |
| `Granted` | `checkSelfPermission == GRANTED` | l'action |

Recalculer à chaque `ON_RESUME` : l'utilisateur revient des réglages, ou a révoqué entre-temps (révocation automatique après des mois d'inactivité, Android 11+).

## Aide

```kotlin
// ui/permissions/PermissionFlow.kt — l'essentiel
private fun compute(afterResult: Boolean) = when {
    granted() -> PermissionState.Granted
    rationale() -> PermissionState.DeniedOnce
    askedBefore() || afterResult -> PermissionState.DeniedForever
    else -> PermissionState.NotAsked
}
fun openSettings() = activity.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))

@Composable
fun rememberPermissionFlow(vararg permissions: String, requireAll: Boolean = true): PermissionFlow {
    val activity = LocalActivity.current!!
    var asked by rememberSaveable { mutableStateOf(false) }
    val flow = remember { PermissionFlow(activity, permissions.toList(), requireAll, { asked }, { asked = true }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { flow.onResult(it) }
    flow.attach(launcher)
    // + un LifecycleEventObserver ON_RESUME → flow.refresh()
    return flow
}
```

```kotlin
// data/location/LocationSource.kt
suspend fun current(): Fix? {
    val fine = has(ACCESS_FINE_LOCATION); val coarse = has(ACCESS_COARSE_LOCATION)
    if (!fine && !coarse) return null
    val priority = if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
    val cancel = CancellationTokenSource()
    val loc: Location? = suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(priority, cancel.token)
            .addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cancel.cancel() }
    }
    return loc?.let { Fix(it.latitude, it.longitude, it.accuracy, precise = fine) }
}
```

```kotlin
// CameraX — aperçu et capture, liés au cycle de vie de l'écran
val providerFuture = ProcessCameraProvider.getInstance(context)
providerFuture.addListener({
    val provider = providerFuture.get()
    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
}, ContextCompat.getMainExecutor(context))

imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor,
    object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(r: ImageCapture.OutputFileResults) = onPhoto(file)
        override fun onError(e: ImageCaptureException) { Log.d("Carnet", "$e") }
    })
```

```xml
<!-- AndroidManifest.xml -->
<provider android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
</provider>
<!-- res/xml/file_paths.xml -->
<paths><files-path name="photos" path="photos/" /></paths>
```

```kotlin
// Navigation 3 — la deuxième entrée
@Serializable data class NoteDetail(val id: Long) : NavKey
entry<NoteDetail> { key -> NoteDetailScreen(id = key.id, onBack = { backStack.removeLastOrNull() }, onShare = …) }
```

Le raccourci `ActivityResultContracts.TakePicture()` (l'app caméra du système, aucune permission) est valable pour le TP ; le TD prend CameraX pour posséder le pipeline — et donc la permission.

## Commandes

```bash
adb shell pm revoke corsica.universita.carnet android.permission.CAMERA              # révoquer (comme dans les réglages)
adb shell pm grant corsica.universita.carnet android.permission.ACCESS_COARSE_LOCATION
adb shell appops get corsica.universita.carnet                                       # ce que l'OS a accordé, et quand
adb shell run-as corsica.universita.carnet ls files/photos/                          # les photos du répertoire privé
adb emu geo fix 9.4500 42.7000                                                       # émulateur : une position (lng lat) — Bastia
adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:corsica.universita.carnet
```

Émulateur : une caméra virtuelle (« Emulated » ou la webcam) dans la configuration de l'AVD ; la position dans Extended Controls → Location.

**Corrigé :** `corrige/` → `android/etapes/A7/`. C'est l'état « fin de Jour 5 » : le point de départ du projet.
