package corsica.universita.carnet.ui.notes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import corsica.universita.carnet.BuildConfig
import corsica.universita.carnet.CarnetContainer
import corsica.universita.carnet.R
import corsica.universita.carnet.data.Note

private const val TAG = "Carnet"

/**
 * TD A3 : partager une note. Un Intent implicite ACTION_SEND : l'OS cherche qui accepte du
 * `text/plain` (Messages, Gmail, Keep… et Carnet lui-même, désormais) et affiche le sélecteur.
 * TD A7 : avec une photo, le type devient `image/jpeg` et le fichier privé est exposé par le FileProvider
 * (`content://…`), avec un droit de lecture temporaire pour l'app choisie.
 */
fun Context.shareNote(note: Note) {
    val text = buildString {
        append(note.text)
        if (note.lat != null && note.lng != null) append("\n${note.lat}, ${note.lng}")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        val photo = note.photoPath
        if (photo == null) {
            type = "text/plain"
        } else {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, CarnetContainer.photoStore.uriFor(photo))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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

/** TD A6 : copier le jeton FCM, à coller dans la console Firebase (message de test). */
fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    // Android 13+ affiche lui-même une confirmation ; avant, un Toast
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.token_copied), Toast.LENGTH_SHORT).show()
    }
}

/**
 * TD A6 : l'écran des réglages de notification de Carnet — la seule issue quand la permission a été
 * refusée définitivement, ou quand l'utilisateur a coupé l'app dans les réglages.
 */
fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
