# Étape A3 — Intents et App Link

État du projet Carnet **à la fin du TD A3** (= état « fin de Jour 4 », base du TD A4). Par rapport à `android/etapes/A2/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/ui/notes/NoteIntents.kt` — `Context.shareNote(note)` (`ACTION_SEND` + `createChooser`) et `Context.openPwa()` (le navigateur par défaut sur `https://<BuildConfig.PWA_HOST>/`, par `makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_BROWSER)` : un simple `ACTION_VIEW` reviendrait à Carnet une fois l'App Link vérifié ; repli `ACTION_VIEW` si aucun navigateur).
- `assetlinks.example.json` — le fichier à déposer dans `/.well-known/` de sa PWA, avec **deux** entrées : Carnet signé par la clé debug du cours (empreinte réelle de `debug.keystore`) et la TWA du TD A0 (à compléter).

## Fichiers modifiés

- `app/src/main/AndroidManifest.xml` — `MainActivity` : `launchMode="singleTop"` ; intent-filter `SEND` `text/plain` ; intent-filter `VIEW` `https://${pwaHost}` avec `android:autoVerify="true"` (`${pwaHost}` = `PWA_HOST` de `local.properties`, injecté par `manifestPlaceholders`).
- `app/src/main/java/corsica/universita/carnet/MainActivity.kt` — `viewModel by viewModels()` ; `handleIntent(intent)` (au premier `onCreate` seulement) et `onNewIntent` : `ACTION_SEND` → `receiveSharedText(EXTRA_TEXT)`, `ACTION_VIEW` → `receiveSharedText(uri)` + log.
- `app/src/main/java/corsica/universita/carnet/ui/notes/NotesViewModel.kt` — `receiveSharedText(text)` : le texte reçu préremplit le brouillon (`SavedStateHandle`).
- `app/src/main/java/corsica/universita/carnet/ui/notes/NotesListScreen.kt` — signature `NotesListScreen(viewModel = viewModel(), onOpenPwa: () -> Unit, onShare: (Note) -> Unit)` ; action « Ouvrir ma PWA » dans la `TopAppBar`, icône « Partager » sur chaque note.
- `app/src/main/java/corsica/universita/carnet/ui/CarnetNavigation.kt` — fournit `onOpenPwa` et `onShare` à l'écran avec le `Context` courant.
- `app/src/main/res/values/strings.xml` — `share`, `share_via`, `open_pwa`.

## Ce que fait Carnet à la fin

Partager une note vers n'importe quelle app ; recevoir un texte partagé (le brouillon se préremplit) ; l'URL de sa PWA ouvre Carnet au lieu du navigateur (`adb shell pm get-app-links corsica.universita.carnet` → `verified`), à condition que `assetlinks.json` contienne l'empreinte de la clé debug du cours.

## `local.properties` attendu

```properties
sdk.dir=/chemin/vers/le/sdk
PWA_HOST=ma-pwa.netlify.app
```

## Signatures livrées à l'agent J5 (contrat § 6.2)

- `data class Note(val id: Long, val text: String, val createdAt: Long)`
- `class NotesViewModel(private val savedState: SavedStateHandle) : ViewModel()` — `notes: StateFlow<List<Note>>`, `draft: StateFlow<String>`, `onDraftChange`, `addNote`, `deleteNote(id)`, `receiveSharedText(text)`
- `@Composable fun NotesListScreen(viewModel: NotesViewModel = viewModel(), onOpenPwa: () -> Unit, onShare: (Note) -> Unit)`
- `MainActivity` : `LifecycleEventObserver` (tag `Carnet`), `handleIntent`, `onNewIntent`
- Manifest : intent-filters `SEND text/plain` et `VIEW https://${pwaHost}` `autoVerify`
