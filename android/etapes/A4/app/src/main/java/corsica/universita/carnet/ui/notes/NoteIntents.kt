package corsica.universita.carnet.ui.notes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import corsica.universita.carnet.BuildConfig
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note

private const val TAG = "Carnet"

/**
 * TD A3 : partager une note. Un Intent implicite ACTION_SEND : l'OS cherche qui accepte du
 * `text/plain` (Messages, Gmail, Keep… et Carnet lui-même, désormais) et affiche le sélecteur.
 */
fun Context.shareNote(note: Note) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, note.text)
    }
    startActivity(Intent.createChooser(send, getString(R.string.share_via)))
}

/**
 * TD A3 : ouvrir sa PWA. `Intent(ACTION_VIEW, url)` suffit… tant que Carnet n'est pas lui-même le
 * gestionnaire vérifié de cette URL (App Link) : ensuite l'OS renverrait l'Intent à Carnet.
 * Le sélecteur MAIN + APP_BROWSER demande explicitement le navigateur par défaut.
 */
fun Context.openPwa() {
    val url = "https://${BuildConfig.PWA_HOST}/".toUri()
    val browser = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
        data = url
    }
    try {
        startActivity(browser)
    } catch (e: ActivityNotFoundException) {
        Log.d(TAG, "Pas de navigateur par défaut, repli sur ACTION_VIEW : $e")
        startActivity(Intent(Intent.ACTION_VIEW, url))
    }
}
