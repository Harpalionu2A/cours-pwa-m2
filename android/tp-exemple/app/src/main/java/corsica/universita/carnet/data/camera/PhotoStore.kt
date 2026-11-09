package corsica.universita.carnet.data.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Les photos de Carnet (TD A7) : des fichiers JPEG dans le répertoire privé de l'app (`filesDir/photos`),
 * invisibles aux autres apps. Pour en partager une, un `FileProvider` (déclaré dans le manifest) fabrique
 * une URI `content://` temporaire : l'autre app reçoit un droit de lecture sur ce fichier, rien d'autre.
 */
class PhotoStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun newFile(): File = File(dir, "note-${System.currentTimeMillis()}.jpg")

    fun uriFor(path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
}
