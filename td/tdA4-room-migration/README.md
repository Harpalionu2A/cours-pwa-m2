# TD A4 — Notes Room et la migration (60 min)

Jour 5 — jeudi 5 novembre 2026, chapitre 2. Point de départ : son projet à la fin du TD A3 (ou `android/etapes/A3/`).

## Objectif

Mettre les notes sur le disque, de façon réactive (Room, `Flow`) ; puis vivre la migration manquante — le jumeau du Service Worker zombie du Jour 1 — et la réparer.

## Fonctionnalités attendues

- [ ] 1. `data/local/NoteEntity.kt` (`@Entity(tableName = "notes")` : `id` auto-généré, `text`, `createdAt`), `NoteDao.kt` (`all(): Flow<List<NoteEntity>>`, `get`, `@Upsert`, `@Delete`), `CarnetDatabase.kt` (`@Database(version = 1, exportSchema = true)`)
- [ ] 2. `data/NotesRepository.kt` : `notes: Flow<List<Note>>`, `add`, `update`, `delete` — la seule source de vérité ; construit une fois dans `CarnetContainer` (`CarnetApp.kt`)
- [ ] 3. `NotesViewModel` branché sur le repository (`stateIn`), `addNote` / `deleteNote` en `viewModelScope.launch` ; une `Factory` pour lui passer le repository
- [ ] 4. **Database Inspector** (App Inspection) montre la table `notes` et ses lignes, « Live updates » coché
- [ ] 5. **Version 2** : colonne `pinned`, icône épingler sur chaque note, tri épinglées en tête, `Migration(1, 2)` écrite
- [ ] 6. `app/schemas/corsica.universita.carnet.data.local.CarnetDatabase/1.json` et `2.json` exportés et versionnés

Textes dans `strings.xml` (`pin`).

## Validation

- Ajouter 3 notes, Accueil, `adb shell am kill corsica.universita.carnet`, revenir : **les notes sont là** ; redémarrer le téléphone : elles sont encore là
- Épingler une note : elle passe en tête, sans aucun code de rafraîchissement (le `Flow` réémet)
- Database Inspector : la table se met à jour en direct ; une requête `SELECT * FROM notes WHERE pinned = 1` à la main
- `2.json` existe ; installer la v2 **par-dessus la v1** (étape 5 par-dessus l'étape 4, sans désinstaller) : les notes de la v1 sont toujours là

## L'échec à vivre

L'étape 5 se joue **avec une base v1 déjà installée et remplie** (les 3 notes de l'étape 4). Dans cet ordre :

1. **(a) Colonne ajoutée, version inchangée.** Ajouter `val pinned: Boolean = false` dans `NoteEntity`, laisser `version = 1`, relancer (Run : l'APK se réinstalle par-dessus, la base reste). Au premier accès à la base : `java.lang.IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.` L'app crashe.
2. **(b) Version 2, sans migration.** Passer `version = 2`, relancer. `java.lang.IllegalStateException: A migration from 1 to 2 was required but not found. Please provide the necessary Migration path via RoomDatabase.Builder.addMigration(...)`. L'app crashe **au lancement** — et elle crasherait ainsi chez chaque utilisateur qui met à jour.

À essayer une fois, pour voir : `.fallbackToDestructiveMigration(true)` sur le builder → l'app démarre, **les 3 notes ont disparu**. L'utilisateur garde l'app et perd ses données. À retirer aussitôt : cette ligne n'est dans aucun corrigé.

Sur un téléphone neuf (base absente), aucun de ces crashs ne se produit : c'est précisément pourquoi on ne les voit pas en développement, et toujours en production.

## La réparation

```kotlin
// CarnetDatabase.kt
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
    }
}
Room.databaseBuilder(context, CarnetDatabase::class.java, "carnet.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

Avec `@ColumnInfo(defaultValue = "0")` sur `pinned`, le schéma exporté (`2.json`) et la table migrée sont identiques — Room les compare au démarrage. Les notes de la v1 reçoivent `pinned = 0`.

## Aide

```kotlin
// NoteEntity.kt
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,   // étape 5
)
```

```kotlin
// NoteDao.kt
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun get(id: Long): NoteEntity?
    @Upsert suspend fun upsert(n: NoteEntity): Long
    @Delete suspend fun delete(n: NoteEntity)
}
```

```kotlin
// CarnetApp.kt — pas de Hilt : un conteneur à la main
object CarnetContainer {
    lateinit var database: CarnetDatabase; lateinit var repository: NotesRepository
    fun init(context: Context) {
        database = CarnetDatabase.build(context)
        repository = NotesRepository(database.noteDao())
    }
}
class CarnetApp : Application() {
    override fun onCreate() { super.onCreate(); CarnetContainer.init(this) }
}
```

```kotlin
// NotesViewModel.kt
class NotesViewModel(private val savedState: SavedStateHandle, private val repository: NotesRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun addNote() { val t = draft.value.trim(); if (t.isEmpty()) return
        viewModelScope.launch { repository.add(t) }; savedState["draft"] = "" }
    fun togglePinned(note: Note) { viewModelScope.launch { repository.update(note.copy(pinned = !note.pinned)) } }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { NotesViewModel(createSavedStateHandle(), CarnetContainer.repository) }
        }
    }
}
// Partout où le ViewModel est demandé : viewModel(factory = NotesViewModel.Factory) / by viewModels { NotesViewModel.Factory }
```

Le schéma est exporté par le plugin Room à chaque compilation (`room { schemaDirectory("$projectDir/schemas") }` est déjà dans `app/build.gradle.kts`) : compiler **avant** de passer en version 2, pour avoir `1.json`.

Imports : `androidx.room.*`, `androidx.room.migration.Migration`, `androidx.sqlite.db.SupportSQLiteDatabase`, `androidx.lifecycle.viewmodel.initializer`, `androidx.lifecycle.viewmodel.viewModelFactory`, `androidx.lifecycle.createSavedStateHandle`, `kotlinx.coroutines.flow.stateIn`.

## Commandes

```bash
adb shell am kill corsica.universita.carnet                       # app en arrière-plan : le process meurt, la base reste
adb shell run-as corsica.universita.carnet ls databases/           # carnet.db, carnet.db-wal, carnet.db-shm
adb shell pm clear corsica.universita.carnet                       # efface la base (= désinstaller) : pour repartir d'une v1 propre
adb logcat -s Carnet AndroidRuntime                                # le crash de la migration manquante, en entier
```

Database Inspector : Android Studio → App Inspection (barre du bas) → Database Inspector → `carnet.db` → `notes` ; « Live updates » en haut à droite ; « Open New Query Tab » pour une requête à la main.

**Corrigé :** `corrige/` → `android/etapes/A4/`.
