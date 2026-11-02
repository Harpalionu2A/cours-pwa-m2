package corsica.universita.carnet.data

/**
 * Une note de terrain, en mémoire (TD A1). Sans identifiant ni Room pour l'instant :
 * l'identifiant arrive au TD A2 (ViewModel), le disque au TD A4 (Room).
 */
data class Note(val text: String, val createdAt: Long)
