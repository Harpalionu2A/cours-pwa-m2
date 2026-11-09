package corsica.universita.carnet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Un seul DataStore par process, nommé `settings` (fichier `settings.preferences_pb` dans `filesDir/datastore`). */
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Les préférences de Carnet (TD A6) : DataStore, l'équivalent typé et asynchrone de `localStorage`.
 * Ici : le jeton FCM de l'appareil, pour l'afficher et le copier.
 */
class Settings(context: Context) {
    private val store = context.applicationContext.settingsStore

    val fcmToken: Flow<String?> = store.data.map { it[KEY_FCM_TOKEN] }

    suspend fun saveFcmToken(token: String) {
        store.edit { it[KEY_FCM_TOKEN] = token }
    }

    private companion object {
        val KEY_FCM_TOKEN = stringPreferencesKey("fcmToken")
    }
}
