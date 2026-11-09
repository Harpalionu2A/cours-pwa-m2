package corsica.universita.carnet.ui.notes

import android.content.Context
import android.content.ContextWrapper

/**
 * Un Context minimal pour construire Settings sur la JVM : DataStore ne touche au disque
 * (filesDir) qu'à la première lecture du Flow, que ces tests ne font jamais.
 */
class FakeContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}
