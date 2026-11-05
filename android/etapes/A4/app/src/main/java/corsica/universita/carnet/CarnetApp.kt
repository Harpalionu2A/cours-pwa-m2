package corsica.universita.carnet

import android.app.Application
import android.content.Context
import corsica.universita.carnet.data.NotesRepository
import corsica.universita.carnet.data.local.CarnetDatabase

/**
 * Pas de framework d'injection : un conteneur construit à la main, une fois par process (TD A4).
 * Les ViewModels y prennent le repository ; rien d'autre ne touche à la base.
 */
object CarnetContainer {
    lateinit var database: CarnetDatabase
        private set
    lateinit var repository: NotesRepository
        private set

    fun init(context: Context) {
        database = CarnetDatabase.build(context)
        repository = NotesRepository(database.noteDao())
    }
}

/**
 * Un objet Application par process : créé avant toute Activity, vivant tant que le process vit.
 * Il construit le conteneur ; les canaux de notification y sont créés au TD A6.
 */
class CarnetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CarnetContainer.init(this)
    }
}
