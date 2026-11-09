package corsica.universita.carnet.notif

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import corsica.universita.carnet.CarnetContainer
import corsica.universita.carnet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "Carnet"

/**
 * Le push, côté Android (TD A6) : un Service déclaré dans le manifest, que l'OS (Play Services) réveille
 * à l'arrivée d'un message FCM — app ouverte, fermée ou tuée. Jamais instancié par l'app.
 *
 * Deux formes de message : *notification* (affiché par l'OS app en arrière-plan, sans passer ici) et
 * *data* (toujours livré ici : Carnet construit la notification, avec la note visée). Un serveur envoie du *data*.
 */
class CarnetMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Le jeton de cet appareil, créé à la première ouverture et renouvelé parfois : à envoyer au serveur. */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Jeton FCM : $token")
        scope.launch { CarnetContainer.settings.saveFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: getString(R.string.notif_hub_default_title)
        val body = message.notification?.body ?: data["body"] ?: ""
        val noteId = data["noteId"]?.toLongOrNull()
        Log.d(TAG, "Push reçu de ${message.from} : « $title » (note $noteId)")
        Notifications.showHub(this, title, body, noteId)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
