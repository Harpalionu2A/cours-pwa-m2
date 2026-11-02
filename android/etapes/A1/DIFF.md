# Étape A1 — Première app Compose

État du projet Carnet **à la fin du TD A1**. Par rapport à `android/starter/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/data/Note.kt` — `data class Note(text, createdAt)`, en mémoire, sans Room.

## Fichiers modifiés

- `app/src/main/java/corsica/universita/carnet/ui/notes/NotesListScreen.kt` — réécrit : `OutlinedTextField` + `Button` (`NoteForm`), compteur « n notes », `LazyColumn` (`NotesList`) avec suppression. État dans le composable : `remember { mutableStateListOf<Note>() }` pour la liste (observée, **perdue à la rotation**), `rememberSaveable` pour le brouillon (**conservé à la rotation**).
- `app/src/main/res/values/strings.xml` — `draft_hint`, `add`, `delete`, `plurals/notes_count`.
- `app/src/main/java/corsica/universita/carnet/data/README.md` — mis à jour.

## Ce que fait Carnet à la fin

Liste + formulaire en mémoire ; le brouillon survit à la rotation, **la liste non** (constat du TD, réparé au TD A2 par le ViewModel).

## Les deux versions « échec » du TD (pas dans ce corrigé)

1. `val notes = remember { mutableListOf<Note>() }` + `notes.add(...)` : la liste est gardée et change, **l'écran ne bouge pas** (liste non observée) → `mutableStateListOf`. *(À tester sur appareil avant le 2/11.)*
2. `var draft by remember { mutableStateOf("") }` : le brouillon disparaît à la rotation → `rememberSaveable`.
