package corsica.universita.carnet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleEventObserver
import corsica.universita.carnet.notif.Notifications
import corsica.universita.carnet.ui.CarnetNavigation
import corsica.universita.carnet.ui.notes.NotesViewModel
import corsica.universita.carnet.ui.theme.CarnetTheme

private const val TAG = "Carnet"

/**
 * L'unique Activity de Carnet : un écran, lancé par l'OS sur un Intent — MAIN/LAUNCHER (l'icône),
 * SEND (un texte partagé), VIEW (l'URL de la PWA, App Link) ou le clic sur une notification (TD A6).
 */
class MainActivity : ComponentActivity() {

    // Le même ViewModel que celui de NotesListScreen : tous deux sont rattachés à l'Activity.
    private val viewModel: NotesViewModel by viewModels { NotesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // avant super.onCreate : l'écran de démarrage, puis Theme.Carnet.Main
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TD A2 : observer le cycle de vie. Filtre Logcat : tag:Carnet
        Log.d(TAG, "onCreate — instance ${System.identityHashCode(this)}, état restauré : ${savedInstanceState != null}")
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                Log.d(TAG, "Activity ${event.name} — instance ${System.identityHashCode(this)}")
            },
        )

        // TD A3 : l'Intent qui a lancé l'Activity. Au premier lancement seulement : après une rotation,
        // onCreate rejoue avec le même Intent, et le texte partagé ne doit pas écraser le brouillon.
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            CarnetTheme {
                CarnetNavigation()
            }
        }
    }

    /** L'Activity existe déjà (launchMode singleTop) : le nouvel Intent arrive ici. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // TD A6 : clic sur une notification push → la note visée (extra posé par Notifications.showHub)
        if (intent.hasExtra(Notifications.EXTRA_NOTE_ID)) {
            val noteId = intent.getLongExtra(Notifications.EXTRA_NOTE_ID, -1L)
            Log.d(TAG, "Clic sur une notification : note $noteId")
            viewModel.openNote(noteId)
            intent.removeExtra(Notifications.EXTRA_NOTE_ID)
        }
        when (intent.action) {
            // Texte partagé depuis une autre app (intent-filter SEND text/plain) → le brouillon
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        Log.d(TAG, "ACTION_SEND reçu : « $text »")
                        viewModel.receiveSharedText(text)
                    }
                }
            }
            // URL de la PWA (intent-filter VIEW https://<PWA_HOST>, vérifié) → Carnet s'ouvre, l'URL
            // arrive dans le brouillon : preuve visible que l'app, et non le navigateur, a reçu le lien.
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    Log.d(TAG, "ACTION_VIEW reçu (App Link) : $uri")
                    viewModel.receiveSharedText(uri.toString())
                }
            }
        }
    }
}
