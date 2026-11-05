package corsica.universita.carnet.data

/**
 * Une note de terrain, telle que l'UI la voit. Depuis le TD A4 elle est lue depuis Room (NoteEntity)
 * par le repository ; `pinned` arrive avec la v2 du schéma.
 */
data class Note(val id: Long, val text: String, val createdAt: Long, val pinned: Boolean = false)
