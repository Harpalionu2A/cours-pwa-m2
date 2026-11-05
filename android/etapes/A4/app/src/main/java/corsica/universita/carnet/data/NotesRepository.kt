package corsica.universita.carnet.data

import corsica.universita.carnet.data.local.NoteDao
import corsica.universita.carnet.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * La seule source de vérité de l'application (TD A4) : l'UI ne parle qu'au repository, jamais à Room.
 * Les écritures sont `suspend` ; Room les exécute sur son propre dispatcher.
 */
class NotesRepository(private val dao: NoteDao) {

    val notes: Flow<List<Note>> = dao.all().map { list -> list.map { it.toNote() } }

    suspend fun add(text: String): Long =
        dao.upsert(NoteEntity(text = text, createdAt = System.currentTimeMillis()))

    suspend fun update(note: Note) {
        dao.upsert(note.toEntity())
    }

    suspend fun delete(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }
}

/** Conversions entre la ligne Room et le modèle de l'UI : l'UI ne voit jamais une entité. */
internal fun NoteEntity.toNote() = Note(id = id, text = text, createdAt = createdAt, pinned = pinned)

internal fun Note.toEntity() = NoteEntity(id = id, text = text, createdAt = createdAt, pinned = pinned)
