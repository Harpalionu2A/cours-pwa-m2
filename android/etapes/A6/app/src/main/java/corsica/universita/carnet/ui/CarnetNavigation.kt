package corsica.universita.carnet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import corsica.universita.carnet.ui.notes.NotesListScreen
import corsica.universita.carnet.R
import corsica.universita.carnet.ui.notes.copyToClipboard
import corsica.universita.carnet.ui.notes.openNotificationSettings
import corsica.universita.carnet.ui.notes.openPwa
import corsica.universita.carnet.ui.notes.shareNote
import kotlinx.serialization.Serializable

/** Clé de l'écran « liste des notes » : une entrée typée du backstack (Navigation 3). */
@Serializable
data object NotesList : NavKey

/**
 * Navigation 3 : le backstack est un état (`rememberNavBackStack`), sauvegardé à la rotation.
 * Un seul écran pour l'instant ; le détail d'une note (NoteDetail) arrive au TD A7.
 * Les Intents (TD A3, A6) partent d'ici : l'écran ne connaît pas le Context, il émet des événements.
 */
@Composable
fun CarnetNavigation() {
    val backStack = rememberNavBackStack(NotesList)
    val context = LocalContext.current
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<NotesList> {
                NotesListScreen(
                    onOpenPwa = { context.openPwa() },
                    onShare = { note -> context.shareNote(note) },
                    onCopyToken = { token -> context.copyToClipboard(context.getString(R.string.copy_token), token) },
                    onOpenNotificationSettings = { context.openNotificationSettings() },
                )
            }
        },
    )
}
