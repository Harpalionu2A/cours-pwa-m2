package corsica.universita.carnet.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import corsica.universita.carnet.CarnetContainer
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "Carnet"

/**
 * L'outbox (TD A5) : envoie les notes non synchronisées, quand l'OS le décide. Lancé par WorkManager
 * — app ouverte, fermée ou tuée ; différé sans réseau (contrainte) ou sous Doze (l'OS).
 * `Result.retry()` sur une panne réseau : WorkManager réessaie avec un délai croissant (backoff).
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker : début (tentative ${runAttemptCount + 1})")
        return try {
            val count = CarnetContainer.repository.syncPending()
            Log.d(TAG, "SyncWorker : $count note(s) envoyée(s)")
            Result.success(workDataOf(KEY_COUNT to count))
        } catch (e: IOException) {
            // Réseau coupé en cours de route, délai dépassé : réessayer plus tard
            Log.d(TAG, "SyncWorker : panne réseau, nouvelle tentative — $e")
            Result.retry()
        } catch (e: HttpException) {
            // Le serveur a répondu, mais en erreur : réessayer n'y changera rien tel quel
            Log.d(TAG, "SyncWorker : HTTP ${e.code()} — abandon")
            Result.failure()
        }
    }

    companion object {
        /** Nom du travail unique : une seule sync en file, jamais deux. */
        const val UNIQUE_NAME = "sync"
        const val KEY_COUNT = "count"
    }
}
