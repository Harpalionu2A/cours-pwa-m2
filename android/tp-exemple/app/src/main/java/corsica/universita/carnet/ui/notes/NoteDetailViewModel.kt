package corsica.universita.carnet.ui.notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import corsica.universita.carnet.CarnetContainer
import corsica.universita.carnet.data.Note
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.location.LocationSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "Carnet"

/** L'état de l'écran « détail d'une note » (TD A7) : la note (depuis Room, réactive), la position, la photo. */
class NoteDetailViewModel(
    private val id: Long,
    private val repository: NotesRepository,
    private val locationSource: LocationSource,
) : ViewModel() {

    val note: StateFlow<Note?> = repository.notes
        .map { list -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    /** Le dernier relevé (précision, précise/approx) : affiché, pas stocké — seules lat/lng vont en base. */
    private val _lastFix = MutableStateFlow<LocationSource.Fix?>(null)
    val lastFix: StateFlow<LocationSource.Fix?> = _lastFix.asStateFlow()

    /** À appeler une fois la permission accordée (le flux de permission est dans l'écran). */
    fun locate() {
        if (_locating.value) return
        viewModelScope.launch {
            _locating.value = true
            val fix = locationSource.current()
            _lastFix.value = fix
            if (fix != null) note.value?.let { repository.update(it.copy(lat = fix.lat, lng = fix.lng)) }
            else Log.d(TAG, "Pas de position pour la note $id")
            _locating.value = false
        }
    }

    fun attachPhoto(path: String) {
        viewModelScope.launch { note.value?.let { repository.update(it.copy(photoPath = path)) } }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch { repository.delete(id); onDone() }
    }

    companion object {
        fun factory(id: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { NoteDetailViewModel(id, CarnetContainer.repository, CarnetContainer.locationSource) }
        }
    }
}
