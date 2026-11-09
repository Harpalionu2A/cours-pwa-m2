package corsica.universita.carnet

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.camera.PhotoStore
import corsica.universita.carnet.data.local.CarnetDatabase
import corsica.universita.carnet.data.local.Settings
import corsica.universita.carnet.data.location.LocationSource
import corsica.universita.carnet.data.remote.Network
import corsica.universita.carnet.data.remote.SyncApi
import corsica.universita.carnet.notif.Notifications

/**
 * Pas de framework d'injection : un conteneur construit à la main, une fois par process (TD A4-A7).
 * Les ViewModels, le SyncWorker et le service FCM y prennent ce dont ils ont besoin.
 */
object CarnetContainer {
    lateinit var database: CarnetDatabase
        private set
    lateinit var api: SyncApi
        private set
    lateinit var workManager: WorkManager
        private set
    lateinit var repository: NotesRepository
        private set
    lateinit var settings: Settings
        private set
    lateinit var locationSource: LocationSource
        private set
    lateinit var photoStore: PhotoStore
        private set

    fun init(context: Context) {
        database = CarnetDatabase.build(context)
        settings = Settings(context)
        locationSource = LocationSource(context)
        photoStore = PhotoStore(context)
        api = Network.syncApi(Network.okHttp(context))
        workManager = WorkManager.getInstance(context)
        repository = NotesRepository(database.noteDao(), api, workManager)
    }
}

/**
 * Un objet Application par process : créé avant toute Activity — et avant tout Worker.
 * Il construit le conteneur et crée les canaux de notification.
 */
class CarnetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CarnetContainer.init(this)
        // TD A6 : les canaux, une fois par process, avant toute notification (locale ou push)
        Notifications.createChannels(this)
    }
}
