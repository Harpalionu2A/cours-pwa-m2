package corsica.universita.carnet.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une ligne de la table `notes`. La forme de cette classe EST le schéma : chaque colonne ajoutée
 * = une nouvelle version de la base + une Migration écrite
 * (v2 : `pinned`, TD A4 · v3 : `synced`, TD A5 · v4 : `lat`, `lng`, `photoPath`, TD A7).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    /** v2 (TD A4) : épingler une note. `DEFAULT 0` pour les lignes déjà en base. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** v3 (TD A5) : la note est partie vers le serveur. Les notes déjà en base repartent toutes (DEFAULT 0). */
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    /** v4 (TD A7) : position et photo, optionnelles — colonnes NULL, aucune valeur par défaut nécessaire. */
    val lat: Double? = null,
    val lng: Double? = null,
    val photoPath: String? = null,
)
