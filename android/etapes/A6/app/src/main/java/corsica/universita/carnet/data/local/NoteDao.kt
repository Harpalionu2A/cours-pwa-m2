package corsica.universita.carnet.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Les accès à la table `notes`. Le SQL des `@Query` est vérifié à la compilation (table, colonnes, types). */
@Dao
interface NoteDao {

    /** Un Flow : réémis à chaque écriture dans la table — l'UI n'a rien à rafraîchir. Épinglées en tête (v2). */
    @Query("SELECT * FROM notes ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    /** L'outbox (v3) : ce qui n'est pas encore parti, dans l'ordre de création. */
    @Query("SELECT * FROM notes WHERE synced = 0 ORDER BY createdAt")
    suspend fun unsynced(): List<NoteEntity>

    /** INSERT si l'id n'existe pas, UPDATE sinon ; renvoie l'id de la ligne. */
    @Upsert
    suspend fun upsert(n: NoteEntity): Long

    @Delete
    suspend fun delete(n: NoteEntity)

    @Query("UPDATE notes SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
