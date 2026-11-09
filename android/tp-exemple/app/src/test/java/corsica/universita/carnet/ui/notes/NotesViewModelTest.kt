package corsica.universita.carnet.ui.notes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.local.NoteDao
import corsica.universita.carnet.data.local.NoteEntity
import corsica.universita.carnet.data.local.Settings
import corsica.universita.carnet.data.remote.EchoResponse
import corsica.universita.carnet.data.remote.NoteDto
import corsica.universita.carnet.data.remote.SyncApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Le ViewModel testé sur la JVM, sans appareil, sans base, sans réseau (Jour 6, chapitre 2).
 * Le vrai NotesRepository est gardé ; ce qu'il y a SOUS lui est remplacé : un NoteDao en mémoire
 * (ce que Room ferait, sans disque), un SyncApi qui renvoie l'écho, un WorkManager qui compte
 * les demandes de sync. `runTest` pour les coroutines, Turbine pour lire les StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    /** La table `notes` en mémoire, triée comme la requête de NoteDao.all(). */
    private class FakeNoteDao : NoteDao {
        val rows = MutableStateFlow<List<NoteEntity>>(emptyList())
        private var nextId = 1L

        override fun all(): Flow<List<NoteEntity>> =
            rows.map { list -> list.sortedWith(compareByDescending<NoteEntity> { it.pinned }.thenByDescending { it.createdAt }) }

        override suspend fun get(id: Long) = rows.value.find { it.id == id }
        override suspend fun unsynced() = rows.value.filter { !it.synced }.sortedBy { it.createdAt }

        override suspend fun upsert(n: NoteEntity): Long {
            val id = if (n.id == 0L) nextId++ else n.id
            rows.update { list -> list.filterNot { it.id == id } + n.copy(id = id) }
            return id
        }

        override suspend fun delete(n: NoteEntity) = rows.update { list -> list.filterNot { it.id == n.id } }
        override suspend fun markSynced(ids: List<Long>) =
            rows.update { list -> list.map { if (it.id in ids) it.copy(synced = true) else it } }
    }

    /** L'écho HTTP sans réseau : renvoie ce qu'il reçoit, comme httpbin.org/post. */
    private class FakeSyncApi : SyncApi {
        val pushed = mutableListOf<NoteDto>()
        override suspend fun push(body: NoteDto): EchoResponse {
            pushed += body
            return EchoResponse(url = "https://httpbin.org/post", json = body)
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeNoteDao
    private lateinit var api: FakeSyncApi
    private lateinit var workManager: FakeWorkManager
    private lateinit var repository: NotesRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)   // viewModelScope tourne sur Main : on lui donne le dispatcher de test
        dao = FakeNoteDao()
        api = FakeSyncApi()
        workManager = FakeWorkManager()
        repository = NotesRepository(dao, api, workManager)
        // Settings (DataStore) n'est lu que si fcmToken est collecté : ici, jamais.
        viewModel = NotesViewModel(SavedStateHandle(), repository, Settings(FakeContext()))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun ajouterUneNoteVideLeBrouillonEtDemandeUneSync() = runTest(dispatcher) {
        viewModel.onDraftChange("  Source au pied du col  ")

        viewModel.notes.test {
            assertEquals(emptyList<Note>(), awaitItem())          // valeur initiale du StateFlow
            viewModel.addNote()
            val notes = awaitItem()
            assertEquals(1, notes.size)
            assertEquals("Source au pied du col", notes[0].text)  // trim appliqué
            assertEquals(false, notes[0].synced)                  // en attente tant que la sync n'est pas passée
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("", viewModel.draft.value)
        assertEquals(listOf("sync"), workManager.uniqueWorkNames) // le travail unique « sync » est demandé
    }

    @Test
    fun unBrouillonVideNAjouteRien() = runTest(dispatcher) {
        viewModel.onDraftChange("   ")
        viewModel.addNote()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(dao.rows.value.isEmpty())
        assertTrue(workManager.uniqueWorkNames.isEmpty())
    }

    @Test
    fun leCompteurEnAttenteRetombeAZeroApresLaSync() = runTest(dispatcher) {
        viewModel.pendingCount.test {
            assertEquals(0, awaitItem())
            viewModel.onDraftChange("Chêne vert marqué")
            viewModel.addNote()
            assertEquals(1, awaitItem())
            // Ce que fait le SyncWorker : envoyer l'outbox, marquer ce qui est parti.
            assertEquals(1, repository.syncPending())
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Chêne vert marqué", api.pushed.single().text)
    }

    @Test
    fun unTextePartagePreremplitLeBrouillon() {
        viewModel.receiveSharedText("https://ma-pwa.netlify.app/")
        assertEquals("https://ma-pwa.netlify.app/", viewModel.draft.value)
    }
}
