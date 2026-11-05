package corsica.universita.carnet.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une ligne de la table `notes`. La forme de cette classe EST le schéma : chaque colonne ajoutée
 * = une nouvelle version de la base + une Migration écrite (v1 → v2 : `pinned`, TD A4).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    /** v2 (TD A4, étape 5) : épingler une note. `DEFAULT 0` pour les lignes déjà en base. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)
