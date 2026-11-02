package corsica.universita.carnet.ui.notes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import corsica.universita.carnet.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "Carnet"
private const val KEY_DRAFT = "draft"

/**
 * L'état de l'écran « liste des notes », hors de l'écran (TD A2, complété au TD A3).
 *
 * Où vit l'état, à ce stade :
 *  - `notes` : dans le ViewModel, en mémoire → survit à la rotation (le ViewModel n'est pas recréé
 *              avec l'Activity)… mais PAS à la mort du process (le ViewModel meurt avec lui).
 *  - `draft` : dans le SavedStateHandle → sauvé par l'OS, survit à la rotation ET à la mort du process.
 * La liste exige le disque : Room, TD A4.
 */
class NotesViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    /** Le brouillon : lu et écrit dans le SavedStateHandle, exposé en StateFlow. */
    val draft: StateFlow<String> = savedState.getStateFlow(KEY_DRAFT, "")

    private var nextId = 1L

    init {
        Log.d(TAG, "NotesViewModel créé (brouillon restauré : « ${draft.value} »)")
    }

    fun onDraftChange(text: String) {
        savedState[KEY_DRAFT] = text
    }

    fun addNote() {
        val text = draft.value.trim()
        if (text.isEmpty()) return
        _notes.update { listOf(Note(id = nextId++, text = text, createdAt = System.currentTimeMillis())) + it }
        savedState[KEY_DRAFT] = ""
    }

    fun deleteNote(id: Long) {
        _notes.update { list -> list.filterNot { it.id == id } }
    }

    /** TD A3 : un texte reçu par Intent (partage, App Link) préremplit le brouillon. */
    fun receiveSharedText(text: String) {
        Log.d(TAG, "Texte reçu : « $text »")
        savedState[KEY_DRAFT] = text
    }

    override fun onCleared() {
        // Appelé quand l'Activity disparaît pour de bon (retour, geste de fermeture) — pas à la rotation.
        Log.d(TAG, "NotesViewModel onCleared")
    }
}
