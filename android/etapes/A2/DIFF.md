# Étape A2 — Le cycle de vie observé

État du projet Carnet **à la fin du TD A2**. Par rapport à `android/etapes/A1/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/ui/notes/NotesViewModel.kt` — `class NotesViewModel(savedState: SavedStateHandle) : ViewModel()` : `notes: StateFlow<List<Note>>` (en mémoire), `draft: StateFlow<String>` (`savedState.getStateFlow("draft", "")`), `onDraftChange`, `addNote`, `deleteNote(id)` ; logs `Carnet` à la création et dans `onCleared`.

## Fichiers modifiés

- `app/src/main/java/corsica/universita/carnet/data/Note.kt` — `id: Long` ajouté (nécessaire pour supprimer depuis le ViewModel).
- `app/src/main/java/corsica/universita/carnet/ui/notes/NotesListScreen.kt` — `NotesListScreen(viewModel: NotesViewModel = viewModel())` : l'état est collecté (`collectAsStateWithLifecycle`), plus rien n'est gardé dans le composable ; `items(notes, key = { it.id })`.
- `app/src/main/java/corsica/universita/carnet/MainActivity.kt` — `LifecycleEventObserver` (tag `Carnet`), log de `savedInstanceState` dans `onCreate`.

## Ce que fait Carnet à la fin

La liste survit à la rotation (ViewModel) ; le brouillon survit à la rotation **et** à la mort du process (`SavedStateHandle`) ; la liste **ne survit pas** à la mort du process — c'est attendu, et c'est le TD A4 (Room) qui le répare.

## Manipulations du TD (sans code)

- Logcat, filtre `tag:Carnet` : `ON_CREATE → ON_START → ON_RESUME`, puis à la rotation `ON_PAUSE → ON_STOP → ON_DESTROY` et une **nouvelle instance** `ON_CREATE…` avec `état restauré : true`.
- Options pour les développeurs → « Ne pas conserver les activités », ou `adb shell am kill corsica.universita.carnet` après avoir mis l'app en arrière-plan : au retour, le brouillon est là, la liste est vide.
