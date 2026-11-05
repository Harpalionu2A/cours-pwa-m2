# Étape A4 — Notes Room et la migration

État du projet Carnet **à la fin du TD A4** (Jour 5, chapitre 2). Par rapport à `android/etapes/A3/` (fin du Jour 4) :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/data/local/NoteEntity.kt` — `@Entity(tableName = "notes")` : `id` (auto), `text`, `createdAt`, et **`pinned`** (v2, `@ColumnInfo(defaultValue = "0")`).
- `app/src/main/java/corsica/universita/carnet/data/local/NoteDao.kt` — `all(): Flow<List<NoteEntity>>` (épinglées en tête, puis plus récentes), `get(id)`, `@Upsert upsert`, `@Delete delete`.
- `app/src/main/java/corsica/universita/carnet/data/local/CarnetDatabase.kt` — `@Database(version = 2, exportSchema = true)` ; `MIGRATION_1_2` (`ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0`) ; `build(context)` (`carnet.db`, `addMigrations`).
- `app/src/main/java/corsica/universita/carnet/data/NotesRepository.kt` — la source de vérité : `notes: Flow<List<Note>>`, `add(text)`, `update(note)`, `delete(id)` ; conversions `NoteEntity` ↔ `Note`.
- `app/schemas/corsica.universita.carnet.data.local.CarnetDatabase/1.json` et `2.json` — schémas exportés par le plugin Room (1.json produit en compilant d'abord l'étape 1 du TD, version 1 sans `pinned`).

## Fichiers modifiés

- `CarnetApp.kt` — `object CarnetContainer` (base + repository), construit dans `CarnetApp.onCreate()`.
- `data/Note.kt` — `pinned: Boolean = false`.
- `ui/notes/NotesViewModel.kt` — reçoit le `NotesRepository` ; `notes` = `repository.notes.stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())` ; `addNote` / `deleteNote` en `viewModelScope.launch` ; `togglePinned(note)` ; `companion object { val Factory }` (`viewModelFactory` + `createSavedStateHandle()` + `CarnetContainer.repository`).
- `ui/notes/NotesListScreen.kt` — `viewModel(factory = NotesViewModel.Factory)` par défaut ; icône épingler (`PushPin`) sur chaque note.
- `MainActivity.kt` — `by viewModels { NotesViewModel.Factory }`.
- `res/values/strings.xml` — `pin`.

## Fichiers supprimés

- `data/README.md` (« vide jusqu'au TD A4 ») et `app/schemas/README.md` : les dossiers ne sont plus vides.

## Ce que fait Carnet à la fin

Les notes sont sur le disque (`/data/data/corsica.universita.carnet/databases/carnet.db`) et survivent à la rotation, à `am kill`, au redémarrage ; une note épinglée passe en tête ; la liste se met à jour seule (le `Flow` de Room réémet) ; Database Inspector montre la table. Réinstaller la v2 par-dessus une v1 avec des notes **les conserve** (la migration est écrite). `fallbackToDestructiveMigration` n'apparaît pas dans ce corrigé.

## Compilation

`./gradlew assembleDebug --no-daemon --console=plain --max-workers=1` → `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk`.
