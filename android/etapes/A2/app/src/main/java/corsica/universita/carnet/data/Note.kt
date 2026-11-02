package corsica.universita.carnet.data

/**
 * Une note de terrain, en mémoire (TD A1-A3). L'identifiant, ajouté au TD A2, sert à supprimer
 * une note depuis le ViewModel. Room et le disque arrivent au TD A4.
 */
data class Note(val id: Long, val text: String, val createdAt: Long)
