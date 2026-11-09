package corsica.universita.carnet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import corsica.universita.carnet.R
import corsica.universita.carnet.ui.notes.NoteDetailScreen
import corsica.universita.carnet.ui.notes.NotesListScreen
import corsica.universita.carnet.ui.notes.NotesViewModel
import corsica.universita.carnet.ui.notes.copyToClipboard
import corsica.universita.carnet.ui.notes.openNotificationSettings
import corsica.universita.carnet.ui.notes.openPwa
import corsica.universita.carnet.ui.notes.shareNote
import kotlinx.serialization.Serializable

/** Clé de l'écran « liste des notes » : une entrée typée du backstack (Navigation 3). */
@Serializable
data object NotesList : NavKey

/** Clé de l'écran « détail d'une note » (TD A7) : la deuxième entrée, avec son argument. */
@Serializable
data class NoteDetail(val id: Long) : NavKey

/**
 * Navigation 3 : le backstack est un état (`rememberNavBackStack`), sauvegardé à la rotation.
 * Deux écrans : la liste et le détail. Les Intents (TD A3, A6, A7) partent d'ici : les écrans ne
 * connaissent pas le Context, ils émettent des événements.
 */
@Composable
fun CarnetNavigation() {
    val backStack = rememberNavBackStack(NotesList)
    val context = LocalContext.current

    // TD A6-A7 : le clic sur une notification (noteId) ouvre le détail de la note visée
    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
    val openNoteId by notesViewModel.openNoteId.collectAsStateWithLifecycle()
    LaunchedEffect(openNoteId) {
        openNoteId?.let { id ->
            if (backStack.lastOrNull() != NoteDetail(id)) backStack.add(NoteDetail(id))
            notesViewModel.openNote(null)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<NotesList> {
                NotesListScreen(
                    onOpen = { note -> backStack.add(NoteDetail(note.id)) },
                    onOpenPwa = { context.openPwa() },
                    onShare = { note -> context.shareNote(note) },
                    onCopyToken = { token -> context.copyToClipboard(context.getString(R.string.copy_token), token) },
                    onOpenNotificationSettings = { context.openNotificationSettings() },
                )
            }
            entry<NoteDetail> { key ->
                NoteDetailScreen(
                    id = key.id,
                    onBack = { backStack.removeLastOrNull() },
                    onShare = { note -> context.shareNote(note) },
                )
            }
        },
    )
}
