package corsica.universita.carnet

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.local.CarnetDatabase
import corsica.universita.carnet.data.remote.Network
import corsica.universita.carnet.data.remote.SyncApi

/**
 * Pas de framework d'injection : un conteneur construit à la main, une fois par process (TD A4-A5).
 * Les ViewModels et le SyncWorker y prennent le repository ; rien d'autre ne touche à la base ni au réseau.
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

    fun init(context: Context) {
        database = CarnetDatabase.build(context)
        api = Network.syncApi(Network.okHttp(context))
        workManager = WorkManager.getInstance(context)
        repository = NotesRepository(database.noteDao(), api, workManager)
    }
}

/**
 * Un objet Application par process : créé avant toute Activity — et avant tout Worker.
 * Il construit le conteneur ; les canaux de notification y sont créés au TD A6.
 */
class CarnetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CarnetContainer.init(this)
    }
}
