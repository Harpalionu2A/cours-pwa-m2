package corsica.universita.carnet.data

/**
 * Une note de terrain, telle que l'UI la voit. Lue depuis Room (NoteEntity) par le repository ;
 * `pinned` arrive avec la v2 du schéma (TD A4), `synced` avec la v3 (TD A5).
 */
data class Note(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val pinned: Boolean = false,
    val synced: Boolean = false,
)
