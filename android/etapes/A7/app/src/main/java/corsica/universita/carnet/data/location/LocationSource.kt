package corsica.universita.carnet.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "Carnet"

/**
 * La position de l'appareil (TD A7), par le fournisseur fusionné de Play Services (GPS, Wi-Fi, cellules).
 * Sur un téléphone sans Google : `LocationManager` (AOSP), même idée, moins de fusion.
 * L'OS peut avoir accordé la précise, ou seulement l'approximative (Android 12+) : la source s'adapte.
 */
class LocationSource(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    data class Fix(val lat: Double, val lng: Double, val accuracyMeters: Float, val precise: Boolean)

    /** `null` si aucune permission, ou si l'appareil n'a pas pu se positionner dans le délai. */
    @SuppressLint("MissingPermission") // vérifié juste au-dessus : fine ou coarse accordée
    suspend fun current(): Fix? {
        val fine = has(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = has(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) return null
        val priority = if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val cancel = CancellationTokenSource()
        val location: Location? = suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(priority, cancel.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { e -> Log.d(TAG, "Localisation impossible : $e"); cont.resume(null) }
            cont.invokeOnCancellation { cancel.cancel() }
        }
        return location?.let { Fix(it.latitude, it.longitude, it.accuracy, precise = fine) }
            .also { Log.d(TAG, "Position : $it") }
    }

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
