# TD A2 — Le cycle de vie observé (30 min)

Jour 4 — lundi 2 novembre 2026. Point de départ : son projet à la fin du TD A1 (ou `android/etapes/A1/`).

## Objectif

Voir les rappels du cycle de vie dans Logcat ; faire survivre l'état de l'écran à la rotation, puis à la mort du process — et constater ce qui ne survit pas.

## Fonctionnalités attendues

- [ ] 1. Un `LifecycleEventObserver` dans `MainActivity`, qui écrit chaque événement dans Logcat avec le tag **`Carnet`**
- [ ] 2. `ui/notes/NotesViewModel.kt` : `class NotesViewModel(savedState: SavedStateHandle) : ViewModel()` qui expose `notes: StateFlow<List<Note>>` ; `NotesListScreen` ne garde plus la liste
- [ ] 3. Le brouillon dans le **`SavedStateHandle`** (`draft: StateFlow<String>`), plus dans `rememberSaveable`
- [ ] 4. Options pour les développeurs → **« Ne pas conserver les activités »** activé, pour le scénario de la mort du process

`Note` reçoit un `id: Long` (pour `deleteNote(id)`).

## Validation

- Logcat (filtre `tag:Carnet`) montre, à la rotation : `ON_PAUSE`, `ON_STOP`, `ON_DESTROY`, puis `ON_CREATE`, `ON_START`, `ON_RESUME` d'une **nouvelle instance** (le numéro d'instance change), et `NotesViewModel créé` **une seule fois**
- La liste **survit à la rotation**
- Après `adb shell am kill corsica.universita.carnet` (app en arrière-plan) et retour par l'écran des applications récentes : le brouillon est là, **la liste est vide — et c'est attendu**

## L'échec à vivre

Avec le ViewModel en place, la rotation ne perd plus rien. Puis :

1. Mettre Carnet en arrière-plan (bouton Accueil), `adb shell am kill corsica.universita.carnet` (ou « Ne pas conserver les activités » + Accueil), revenir par les applications récentes.
2. **Tout est perdu**, brouillon compris, malgré le ViewModel : le process est mort, le ViewModel avec lui. L'utilisateur, lui, revient sur « son » écran et attend de le retrouver.

Logcat : `onCreate — état restauré : true` — l'OS avait sauvé un `Bundle` ; il ne contenait rien d'utile.

## La réparation

- Le brouillon : `savedState.getStateFlow("draft", "")` et `savedState["draft"] = text`. Le `SavedStateHandle` est écrit dans le `Bundle` de l'Activity par l'OS, avant de tuer le process, et rendu à la nouvelle instance du ViewModel.
- La liste : un `Bundle` n'est pas fait pour des données (taille limitée, ~500 Ko, transaction binder). **La liste exige le disque** : Room, Jour 5, TD A4. Écrit noir sur blanc dans le corrigé.

## Aide

```kotlin
// MainActivity.kt
private const val TAG = "Carnet"

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate — instance ${System.identityHashCode(this)}, état restauré : ${savedInstanceState != null}")
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        Log.d(TAG, "Activity ${event.name} — instance ${System.identityHashCode(this)}")
    })
    …
}
```

```kotlin
// NotesViewModel.kt
class NotesViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    val draft: StateFlow<String> = savedState.getStateFlow("draft", "")

    fun onDraftChange(text: String) { savedState["draft"] = text }
    fun addNote() { … _notes.update { listOf(Note(nextId++, text, System.currentTimeMillis())) + it }; savedState["draft"] = "" }
    fun deleteNote(id: Long) { _notes.update { list -> list.filterNot { it.id == id } } }
}
```

```kotlin
// NotesListScreen.kt — l'écran ne garde plus rien
@Composable
fun NotesListScreen(viewModel: NotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    …
    NoteForm(draft = draft, onDraftChange = viewModel::onDraftChange, onAdd = viewModel::addNote)
}
```

Imports : `androidx.lifecycle.LifecycleEventObserver`, `androidx.lifecycle.SavedStateHandle`, `androidx.lifecycle.ViewModel`, `androidx.lifecycle.viewmodel.compose.viewModel`, `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `kotlinx.coroutines.flow.*`.

## Commandes

```bash
adb logcat -s Carnet                                   # le journal, filtré
adb shell am kill corsica.universita.carnet            # tue le process (app en arrière-plan seulement)
adb shell am force-stop corsica.universita.carnet      # plus brutal : l'OS oublie aussi la tâche (pas de restauration)
adb shell dumpsys activity activities | grep -i carnet # l'état de l'Activity vu par l'OS
```

« Ne pas conserver les activités » : Paramètres → Système → Options pour les développeurs → section *Applications* → « Ne pas conserver les activités » (*Don't keep activities*). À **désactiver** à la fin du TD.

**Corrigé :** `corrige/` → `android/etapes/A2/`.
