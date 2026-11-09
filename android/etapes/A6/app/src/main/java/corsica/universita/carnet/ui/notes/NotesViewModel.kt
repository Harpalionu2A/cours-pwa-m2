package corsica.universita.carnet.ui.notes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.messaging.FirebaseMessaging
import corsica.universita.carnet.CarnetContainer
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.local.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "Carnet"
private const val KEY_DRAFT = "draft"
private const val KEY_OPEN_NOTE = "openNoteId"

/**
 * L'état de l'écran « liste des notes » (TD A4-A6) : la liste vient du disque (Room, via le repository)
 * et survit à tout ; le brouillon reste dans le SavedStateHandle (il n'est pas en base tant qu'il
 * n'est pas validé).
 */
class NotesViewModel(
    private val savedState: SavedStateHandle,
    private val repository: NotesRepository,
    private val settings: Settings,
) : ViewModel() {

    /** Le Flow du repository devient un StateFlow : collecté tant qu'un écran l'observe (+ 5 s à la rotation). */
    val notes: StateFlow<List<Note>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** TD A5 : le badge « n en attente » — dérivé de la liste, pas un état de plus. */
    val pendingCount: StateFlow<Int> = notes.map { list -> list.count { !it.synced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val draft: StateFlow<String> = savedState.getStateFlow(KEY_DRAFT, "")

    /** TD A6 : la note visée par le clic sur une notification (null = aucune). Dans le Bundle : survit au process. */
    val openNoteId: StateFlow<Long?> = savedState.getStateFlow(KEY_OPEN_NOTE, null)

    /** TD A6 : le jeton FCM de cet appareil (DataStore), l'adresse à donner au serveur. */
    val fcmToken: StateFlow<String?> = settings.fcmToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        Log.d(TAG, "NotesViewModel créé (brouillon restauré : « ${draft.value} »)")
    }

    fun onDraftChange(text: String) {
        savedState[KEY_DRAFT] = text
    }

    fun addNote() {
        val text = draft.value.trim()
        if (text.isEmpty()) return
        // Une écriture Room est suspend : elle part dans la portée du ViewModel, annulée avec lui.
        viewModelScope.launch { repository.add(text) }
        savedState[KEY_DRAFT] = ""
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** v2 : épingler / désépingler — la liste se réordonne seule (le Flow réémet). */
    fun togglePinned(note: Note) {
        viewModelScope.launch { repository.update(note.copy(pinned = !note.pinned)) }
    }

    /** TD A6 : clic sur une notification → la note à montrer ; `null` une fois traitée. */
    fun openNote(id: Long?) {
        savedState[KEY_OPEN_NOTE] = id
    }

    /** TD A6 : demander le jeton à FCM (onNewToken ne rejoue pas s'il a été manqué) et le garder. */
    fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> viewModelScope.launch { settings.saveFcmToken(token) } }
            .addOnFailureListener { e -> Log.d(TAG, "Jeton FCM indisponible : $e") }
    }

    /** TD A3 : un texte reçu par Intent (partage, App Link) préremplit le brouillon. */
    fun receiveSharedText(text: String) {
        Log.d(TAG, "Texte reçu : « $text »")
        savedState[KEY_DRAFT] = text
    }

    override fun onCleared() {
        Log.d(TAG, "NotesViewModel onCleared")
    }

    companion object {
        /** Le ViewModel reçoit le repository du conteneur ; le SavedStateHandle vient du framework. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { NotesViewModel(createSavedStateHandle(), CarnetContainer.repository, CarnetContainer.settings) }
        }
    }
}
