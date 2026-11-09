package corsica.universita.carnet.ui.notes;

import android.app.PendingIntent;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kotlinx.coroutines.flow.Flow;

/**
 * Un WorkManager de test : il ne lance rien, il note les noms des travaux uniques demandés.
 * Écrit en Java parce que le constructeur de WorkManager est « internal » en Kotlin : une classe
 * Kotlin hors de la bibliothèque ne peut pas en hériter, une classe Java le peut.
 * NotesRepository n'appelle que enqueueUniqueWork ; le reste de l'API n'est pas utilisé ici.
 */
public class FakeWorkManager extends WorkManager {

    public final List<String> uniqueWorkNames = new ArrayList<>();

    @NonNull
    @Override
    public Operation enqueueUniqueWork(@NonNull String uniqueWorkName, @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> requests) {
        uniqueWorkNames.add(uniqueWorkName);
        return new Operation() {
            @NonNull @Override public LiveData<State> getState() { throw unused(); }
            @NonNull @Override public ListenableFuture<State.SUCCESS> getResult() { throw unused(); }
        };
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("non utilisé par NotesRepository");
    }

    @NonNull @Override public Configuration getConfiguration() { throw unused(); }
    @NonNull @Override public Operation enqueue(@NonNull List<? extends WorkRequest> requests) { throw unused(); }
    @NonNull @Override public WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> requests) { throw unused(); }
    @NonNull @Override public WorkContinuation beginUniqueWork(@NonNull String name, @NonNull ExistingWorkPolicy policy,
            @NonNull List<OneTimeWorkRequest> requests) { throw unused(); }
    @NonNull @Override public Operation enqueueUniquePeriodicWork(@NonNull String name, @NonNull ExistingPeriodicWorkPolicy policy,
            @NonNull PeriodicWorkRequest request) { throw unused(); }
    @NonNull @Override public Operation cancelWorkById(@NonNull UUID id) { throw unused(); }
    @NonNull @Override public Operation cancelAllWorkByTag(@NonNull String tag) { throw unused(); }
    @NonNull @Override public Operation cancelUniqueWork(@NonNull String name) { throw unused(); }
    @NonNull @Override public Operation cancelAllWork() { throw unused(); }
    @NonNull @Override public PendingIntent createCancelPendingIntent(@NonNull UUID id) { throw unused(); }
    @NonNull @Override public Operation pruneWork() { throw unused(); }
    @NonNull @Override public LiveData<Long> getLastCancelAllTimeMillisLiveData() { throw unused(); }
    @NonNull @Override public ListenableFuture<Long> getLastCancelAllTimeMillis() { throw unused(); }
    @NonNull @Override public LiveData<WorkInfo> getWorkInfoByIdLiveData(@NonNull UUID id) { throw unused(); }
    @NonNull @Override public Flow<WorkInfo> getWorkInfoByIdFlow(@NonNull UUID id) { throw unused(); }
    @NonNull @Override public ListenableFuture<WorkInfo> getWorkInfoById(@NonNull UUID id) { throw unused(); }
    @NonNull @Override public LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(@NonNull String tag) { throw unused(); }
    @NonNull @Override public Flow<List<WorkInfo>> getWorkInfosByTagFlow(@NonNull String tag) { throw unused(); }
    @NonNull @Override public ListenableFuture<List<WorkInfo>> getWorkInfosByTag(@NonNull String tag) { throw unused(); }
    @NonNull @Override public LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(@NonNull String name) { throw unused(); }
    @NonNull @Override public Flow<List<WorkInfo>> getWorkInfosForUniqueWorkFlow(@NonNull String name) { throw unused(); }
    @NonNull @Override public ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(@NonNull String name) { throw unused(); }
    @NonNull @Override public LiveData<List<WorkInfo>> getWorkInfosLiveData(@NonNull WorkQuery query) { throw unused(); }
    @NonNull @Override public Flow<List<WorkInfo>> getWorkInfosFlow(@NonNull WorkQuery query) { throw unused(); }
    @NonNull @Override public ListenableFuture<List<WorkInfo>> getWorkInfos(@NonNull WorkQuery query) { throw unused(); }
    @NonNull @Override public ListenableFuture<UpdateResult> updateWork(@NonNull WorkRequest request) { throw unused(); }
}
