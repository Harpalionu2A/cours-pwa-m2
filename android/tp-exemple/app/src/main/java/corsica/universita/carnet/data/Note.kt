package corsica.universita.carnet.data

/**
 * Une note de terrain, telle que l'UI la voit. Lue depuis Room (NoteEntity) par le repository ;
 * `pinned` : v2 (TD A4) · `synced` : v3 (TD A5) · `lat`, `lng`, `photoPath` : v4 (TD A7).
 */
data class Note(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val pinned: Boolean = false,
    val synced: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val photoPath: String? = null,
)
