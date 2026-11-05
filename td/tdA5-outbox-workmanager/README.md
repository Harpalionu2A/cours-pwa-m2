# TD A5 — L'outbox WorkManager (50 min)

Jour 5 — jeudi 5 novembre 2026, chapitre 4. Point de départ : son projet à la fin du TD A4 (ou `android/etapes/A4/`).

## Objectif

Envoyer chaque note en arrière-plan vers un écho HTTP quand le réseau est là — app ouverte, fermée ou tuée ; vivre le refus de l'OS (thread principal, puis Doze).

## Fonctionnalités attendues

- [ ] 0. **(échec préalable)** Un appel réseau direct (`URL("https://httpbin.org/get").readText()`) dans `addNote()` du ViewModel, sur le thread principal — à provoquer, puis à retirer
- [ ] 1. `data/remote/SyncApi.kt` : `@Serializable NoteDto`, `interface SyncApi { @POST("post") suspend fun push(@Body body: NoteDto): EchoResponse }` ; `data/remote/Network.kt` : `OkHttpClient` (cache, journalisation) + `Retrofit` sur `BuildConfig.SYNC_BASE_URL` (`https://httpbin.org/`)
- [ ] 2. **Version 3** : colonne `synced` (`DEFAULT 0`) — **la migration `2 → 3` est écrite seul**, sans modèle ; `NoteDao.unsynced()`, `markSynced(ids)`
- [ ] 3. `work/SyncWorker.kt` (`CoroutineWorker`) : les notes non synchronisées → `POST` → `synced = 1` ; `Result.retry()` sur `IOException`
- [ ] 4. `NotesRepository.enqueueSync()` : `OneTimeWorkRequest`, contrainte `NetworkType.CONNECTED`, `enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, …)`, appelé à chaque création de note
- [ ] 5. Badge « n en attente » à côté du compteur de notes

Textes dans `strings.xml` (`pending`, `pending_count`).

## Validation

- Mode avion : créer 3 notes → « 3 en attente » ; Background Task Inspector (App Inspection) montre le travail `sync` **ENQUEUED**
- Couper le mode avion : les notes partent sans aucune action, le badge disparaît ; Logcat (`tag:Carnet`) montre `--> POST https://httpbin.org/post`, `<-- 200`, et la réponse de l'écho avec le texte de la note
- Mode avion, 2 notes, **fermer Carnet par geste** (ou `am kill`), couper le mode avion : les notes partent quand même (Logcat, puis rouvrir : badge à 0)
- `app/schemas/…/3.json` existe ; installer par-dessus la v2 sans perdre les notes

## L'échec à vivre

**(0) Le thread principal.** Dans `addNote()`, avant tout le reste :

```kotlin
val echo = java.net.URL("https://httpbin.org/get").readText()   // sur le thread principal
```

Ajouter une note : `android.os.NetworkOnMainThreadException`, immédiate, dès le premier octet. L'OS refuse — depuis Android 3.0 (2011) — ce que le navigateur tolérait (un `fetch` dans un gestionnaire de clic). Sans cette exception, ce serait l'ANR du Jour 4 : l'écran gelé pendant toute la requête. **Réparation :** la requête part dans une coroutine, sur `Dispatchers.IO`, depuis le repository — et c'est le worker qui la lance, pas le ViewModel.

**(1) Doze.** Mode avion, 3 notes en attente, **écran éteint**, puis Doze forcé **avant** le retour du réseau (réseau revenu d'abord : le travail part aussitôt) :

```bash
adb shell dumpsys deviceidle force-idle                  # → "Now forced in to deep idle mode"
adb shell cmd connectivity airplane-mode disable         # puis : le réseau revient, sous Doze
```

Rien ne part : Background Task Inspector reste sur ENQUEUED, Logcat est muet. Le réseau est là, la contrainte est satisfaite… et l'OS a décidé que ce n'est pas le moment. Puis :

```bash
adb shell dumpsys deviceidle step            # une étape de plus : IDLE_MAINTENANCE = une fenêtre
adb shell dumpsys deviceidle unforce         # ou : sortir de Doze
```

Les notes partent dans la fenêtre de maintenance. Sur un Xiaomi, un Samsung, un Huawei : même scénario sans adb, écran éteint 10 minutes — et parfois rien du tout tant que l'« optimisation de la batterie » de l'app n'est pas désactivée par l'utilisateur (Paramètres → Batterie → Carnet). Le natif n'est pas libre : il négocie avec l'hôte.

## La réparation

Accepter le délai, et le montrer : la contrainte réseau (déjà là), le badge « en attente » (déjà là), `WorkInfo` observable si l'écran doit afficher « envoi en cours ». En option, pour les cas urgents : `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` sur la requête — un quota, pas une garantie. Jamais un foreground service pour une outbox.

```kotlin
// Observer l'état du travail unique (facultatif)
workManager.getWorkInfosForUniqueWorkFlow("sync")   // Flow<List<WorkInfo>> : ENQUEUED, RUNNING, SUCCEEDED…
```

## Aide

```kotlin
// data/remote/SyncApi.kt
@Serializable data class NoteDto(val id: Long, val text: String, val createdAt: Long)
@Serializable data class EchoResponse(val url: String = "", val json: NoteDto? = null)
interface SyncApi { @POST("post") suspend fun push(@Body body: NoteDto): EchoResponse }
```

```kotlin
// data/remote/Network.kt
object Network {
    private val json = Json { ignoreUnknownKeys = true }
    fun okHttp(context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
        .addInterceptor(HttpLoggingInterceptor { Log.d("Carnet", it) }.apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()
    fun syncApi(client: OkHttpClient): SyncApi = Retrofit.Builder()
        .baseUrl(BuildConfig.SYNC_BASE_URL).client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(SyncApi::class.java)
}
```

```kotlin
// work/SyncWorker.kt
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        val count = CarnetContainer.repository.syncPending()
        Result.success(workDataOf("count" to count))
    } catch (e: IOException) { Result.retry() } catch (e: HttpException) { Result.failure() }
}
```

```kotlin
// data/NotesRepository.kt
fun enqueueSync() {
    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, request)
}
suspend fun syncPending(): Int {
    val sent = mutableListOf<Long>()
    for (e in dao.unsynced()) { api.push(NoteDto(e.id, e.text, e.createdAt)); sent += e.id }
    if (sent.isNotEmpty()) dao.markSynced(sent)
    return sent.size
}
```

`WorkManager.getInstance(context)` dans `CarnetContainer` ; le repository reçoit `dao`, `api`, `workManager`. La migration `2 → 3` : même forme que `MIGRATION_1_2`, colonne `synced INTEGER NOT NULL DEFAULT 0`, et `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`.

Imports : `retrofit2.http.*`, `retrofit2.converter.kotlinx.serialization.asConverterFactory`, `okhttp3.MediaType.Companion.toMediaType`, `okhttp3.logging.HttpLoggingInterceptor`, `androidx.work.*`, `retrofit2.HttpException`, `java.io.IOException`.

## Commandes

```bash
adb logcat -s Carnet                                              # requêtes OkHttp, écho, SyncWorker
adb shell cmd connectivity airplane-mode enable                   # mode avion (Android 11+) ; disable pour sortir
adb shell dumpsys deviceidle force-idle                           # Doze profond, immédiatement (écran éteint d'abord, puis couper le mode avion)
adb shell dumpsys deviceidle step                                 # avancer d'une étape (fenêtre de maintenance)
adb shell dumpsys deviceidle unforce                              # sortir de Doze
adb shell dumpsys deviceidle get deep                             # l'état courant : ACTIVE, IDLE, IDLE_MAINTENANCE…
adb shell dumpsys jobscheduler | grep -A3 corsica.universita      # le travail vu par JobScheduler
```

Background Task Inspector : Android Studio → App Inspection → Background Task Inspector → Workers : `SyncWorker`, état, contraintes, tentatives.

**Corrigé :** `corrige/` → `android/etapes/A5/`. `SYNC_BASE_URL=…` dans `local.properties` pour viser un autre serveur.
