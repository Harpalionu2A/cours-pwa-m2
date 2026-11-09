package corsica.universita.carnet.data

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import corsica.universita.carnet.data.local.NoteDao
import corsica.universita.carnet.data.local.NoteEntity
import corsica.universita.carnet.data.remote.NoteDto
import corsica.universita.carnet.data.remote.SyncApi
import corsica.universita.carnet.work.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private const val TAG = "Carnet"

/**
 * La seule source de vérité de l'application : l'UI ne parle qu'au repository, jamais à Room ni au réseau.
 * Room est la vérité ; le réseau (TD A5) est une copie envoyée plus tard, quand l'OS le permet.
 */
class NotesRepository(
    private val dao: NoteDao,
    private val api: SyncApi,
    private val workManager: WorkManager,
) {

    val notes: Flow<List<Note>> = dao.all().map { list -> list.map { it.toNote() } }

    /** Écrit sur le disque, puis demande une sync : la note est « en attente » tant qu'elle n'est pas partie. */
    suspend fun add(text: String): Long {
        val id = dao.upsert(NoteEntity(text = text, createdAt = System.currentTimeMillis()))
        enqueueSync()
        return id
    }

    suspend fun update(note: Note) {
        dao.upsert(note.toEntity())
    }

    suspend fun delete(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }

    /**
     * Un travail unique « sync » : s'il est déjà en file, KEEP le garde (pas de doublon) ; contrainte
     * réseau : WorkManager ne le lance que connecté ; persistant : survit à la fermeture et au redémarrage.
     */
    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(SyncWorker.UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        Log.d(TAG, "Sync demandée (travail unique « ${SyncWorker.UNIQUE_NAME} », contrainte CONNECTED)")
    }

    /** Appelé par le SyncWorker : envoie chaque note non synchronisée, marque celles qui sont parties. */
    suspend fun syncPending(): Int {
        val pending = dao.unsynced()
        val sent = mutableListOf<Long>()
        for (entity in pending) {
            val echo = api.push(NoteDto(entity.id, entity.text, entity.createdAt))
            Log.d(TAG, "Écho reçu pour la note ${entity.id} : ${echo.url} → « ${echo.json?.text} »")
            sent += entity.id
        }
        if (sent.isNotEmpty()) dao.markSynced(sent)
        return sent.size
    }
}

/** Conversions entre la ligne Room et le modèle de l'UI : l'UI ne voit jamais une entité. */
internal fun NoteEntity.toNote() =
    Note(id = id, text = text, createdAt = createdAt, pinned = pinned, synced = synced, lat = lat, lng = lng, photoPath = photoPath)

internal fun Note.toEntity() =
    NoteEntity(id = id, text = text, createdAt = createdAt, pinned = pinned, synced = synced, lat = lat, lng = lng, photoPath = photoPath)
