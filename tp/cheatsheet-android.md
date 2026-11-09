# Antisèche Android — référence du TP (cours Android)

> Tous les extraits du cours, pris dans `android/tp-exemple/` (compilé) et prêts à adapter. À garder ouvert pendant le TP.
> Conventions : package `corsica.universita.carnet`, Kotlin + Compose + Material 3, versions épinglées dans `gradle/libs.versions.toml` (ne rien changer). Les `Rappel PWA` renvoient à l'antisèche du cours PWA, `tp/cheatsheet.md`.

---

## 1. Manifest minimal (permissions, intent-filters, service FCM, FileProvider)

```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Permissions : normales (accordées à l'installation) et dangereuses (runtime, § 8) -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application android:name=".CarnetApp" android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name" android:theme="@style/Theme.Carnet">

        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTop">
            <intent-filter>                                  <!-- l'icône -->
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>                                  <!-- recevoir un texte partagé (TD A3) -->
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
            <intent-filter android:autoVerify="true">        <!-- App Link : l'URL de la PWA ouvre l'app (§ 9) -->
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="${pwaHost}" />
            </intent-filter>
        </activity>

        <service android:name=".notif.CarnetMessagingService" android:exported="false">   <!-- push FCM (§ 7) -->
            <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>
        </service>

        <provider android:name="androidx.core.content.FileProvider"                       <!-- partager un fichier privé (§ 8) -->
            android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
        </provider>

        <meta-data android:name="com.google.firebase.messaging.default_notification_icon" android:resource="@drawable/ic_notification" />
        <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" android:value="hub" />
    </application>
</manifest>
```

```xml
<!-- app/src/main/res/xml/file_paths.xml -->
<paths><files-path name="photos" path="photos/" /></paths>
```

`${pwaHost}` vient de `manifestPlaceholders["pwaHost"]` dans `app/build.gradle.kts`, lu dans `local.properties` (`PWA_HOST=ma-pwa.netlify.app`). `android:exported` explicite sur chaque composant.

---

## 2. Icône adaptive + splash

```xml
<!-- res/mipmap-anydpi-v26/ic_launcher.xml : fond + premier plan, l'OS applique la forme -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />   <!-- zone sûre : 66 dp centrés sur 108 -->
</adaptive-icon>
```

```xml
<!-- res/values/themes.xml : core-splashscreen (Android 12+ natif, rétro-compatible), puis le thème de l'app -->
<style name="Theme.Carnet" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/carnet_paper</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.Carnet.Main</item>
</style>
<style name="Theme.Carnet.Main" parent="android:Theme.Material.Light.NoActionBar" />
```

```kotlin
// MainActivity.kt — avant super.onCreate
installSplashScreen()
```

Android Studio : clic droit sur `res` → New → Image Asset pour générer les PNG par densité à partir d'un SVG. Rappel PWA : `icons` `maskable` ↔ icône adaptive, `background_color` ↔ `windowSplashScreenBackground`.

---

## 3. Room : entité, DAO, base, migration, repository

```kotlin
// data/local/NoteEntity.kt
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String, val createdAt: Long,
    val pinned: Boolean = false,          // v2
    val synced: Boolean = false,          // v3 : l'outbox
    val lat: Double? = null, val lng: Double? = null, val photoPath: String? = null,   // v4
)
```

```kotlin
// data/local/NoteDao.kt — le SQL est vérifié à la compilation
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<NoteEntity>>                           // réactif : chaque écriture réémet
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun get(id: Long): NoteEntity?
    @Query("SELECT * FROM notes WHERE synced = 0 ORDER BY createdAt") suspend fun unsynced(): List<NoteEntity>
    @Upsert suspend fun upsert(n: NoteEntity): Long
    @Delete suspend fun delete(n: NoteEntity)
    @Query("UPDATE notes SET synced = 1 WHERE id IN (:ids)") suspend fun markSynced(ids: List<Long>)
}
```

```kotlin
// data/local/CarnetDatabase.kt — une version + une Migration par changement de schéma, JAMAIS fallbackToDestructiveMigration
@Database(entities = [NoteEntity::class], version = 4, exportSchema = true)
abstract class CarnetDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }
        // MIGRATION_2_3 (synced), MIGRATION_3_4 (lat REAL, lng REAL, photoPath TEXT) : même forme
        fun build(context: Context): CarnetDatabase =
            Room.databaseBuilder(context, CarnetDatabase::class.java, "carnet.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
```

```kotlin
// app/build.gradle.kts — schémas exportés dans app/schemas/ (à versionner dans git)
plugins { alias(libs.plugins.room); alias(libs.plugins.ksp) }
room { schemaDirectory("$projectDir/schemas") }
dependencies { implementation(libs.room.runtime); ksp(libs.room.compiler) }
```

```kotlin
// data/NotesRepository.kt — la seule source de vérité est Room ; le réseau est un rafraîchissement
class NotesRepository(private val dao: NoteDao, private val api: SyncApi, private val workManager: WorkManager) {
    val notes: Flow<List<Note>> = dao.all().map { list -> list.map { it.toNote() } }
    suspend fun add(text: String): Long {
        val id = dao.upsert(NoteEntity(text = text, createdAt = System.currentTimeMillis()))
        enqueueSync(); return id
    }
    suspend fun update(note: Note) { dao.upsert(note.toEntity()) }
    suspend fun delete(id: Long) { dao.get(id)?.let { dao.delete(it) } }
    // enqueueSync() et syncPending() : § 6
}
```

Les échecs à connaître : colonne ajoutée sans changer la version → `Room cannot verify the data integrity` ; version changée sans `Migration` → `A migration from 1 to 2 was required but not found` (crash au lancement, chez tous ceux qui mettent à jour). Rappel PWA : le Service Worker zombie, même leçon.

---

## 4. ViewModel + `StateFlow` + collecte

```kotlin
// ui/notes/NotesViewModel.kt
class NotesViewModel(
    private val savedState: SavedStateHandle,
    private val repository: NotesRepository,
    private val settings: Settings,
) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pendingCount: StateFlow<Int> = notes.map { l -> l.count { !it.synced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val draft: StateFlow<String> = savedState.getStateFlow(KEY_DRAFT, "")   // survit à la mort du process

    fun onDraftChange(text: String) { savedState[KEY_DRAFT] = text }
    fun addNote() {
        val text = draft.value.trim(); if (text.isEmpty()) return
        viewModelScope.launch { repository.add(text) }       // la coroutine meurt avec le ViewModel
        savedState[KEY_DRAFT] = ""
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {       // pas de Hilt : une Factory
            initializer { NotesViewModel(createSavedStateHandle(), CarnetContainer.repository, CarnetContainer.settings) }
        }
    }
}
```

```kotlin
// ui/notes/NotesListScreen.kt — l'écran ne garde rien, il affiche l'état et remonte les événements
@Composable
fun NotesListScreen(viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory), onShare: (Note) -> Unit, …) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()      // collecte arrêtée quand l'écran n'est plus visible
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    OutlinedTextField(value = draft, onValueChange = viewModel::onDraftChange, label = { Text(stringResource(R.string.draft_hint)) })
    Button(onClick = viewModel::addNote, enabled = draft.isNotBlank()) { Text(stringResource(R.string.add)) }
    LazyColumn { items(notes, key = { it.id }) { note -> ListItem(headlineContent = { Text(note.text) }) } }
}
```

```kotlin
// CarnetApp.kt — le conteneur, construit une fois par process (pas de Hilt)
object CarnetContainer {
    lateinit var repository: NotesRepository; private set
    lateinit var settings: Settings; private set
    fun init(context: Context) {
        val api = Network.syncApi(Network.okHttp(context))
        repository = NotesRepository(CarnetDatabase.build(context).noteDao(), api, WorkManager.getInstance(context))
        settings = Settings(context)
    }
}
class CarnetApp : Application() {
    override fun onCreate() { super.onCreate(); CarnetContainer.init(this); Notifications.createChannels(this) }
}
```

```kotlin
// app/src/test/…/NotesViewModelTest.kt — JVM, sans appareil : le vrai NotesRepository, ce qui est SOUS lui remplacé
class NotesViewModelTest {
    private class FakeNoteDao : NoteDao {                    // la table `notes` en mémoire
        val rows = MutableStateFlow<List<NoteEntity>>(emptyList())
        private var nextId = 1L
        override fun all() = rows.map { l -> l.sortedWith(compareByDescending<NoteEntity> { it.pinned }.thenByDescending { it.createdAt }) }
        override suspend fun get(id: Long) = rows.value.find { it.id == id }
        override suspend fun unsynced() = rows.value.filter { !it.synced }.sortedBy { it.createdAt }
        override suspend fun upsert(n: NoteEntity): Long {
            val id = if (n.id == 0L) nextId++ else n.id
            rows.update { l -> l.filterNot { it.id == id } + n.copy(id = id) }; return id
        }
        override suspend fun delete(n: NoteEntity) = rows.update { l -> l.filterNot { it.id == n.id } }
        override suspend fun markSynced(ids: List<Long>) = rows.update { l -> l.map { if (it.id in ids) it.copy(synced = true) else it } }
    }
    private class FakeSyncApi : SyncApi {                    // l'écho de httpbin, sans réseau
        val pushed = mutableListOf<NoteDto>()
        override suspend fun push(body: NoteDto): EchoResponse { pushed += body; return EchoResponse("https://httpbin.org/post", body) }
    }
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeNoteDao
    private lateinit var workManager: FakeWorkManager       // classe Java : voir ci-dessous
    private lateinit var viewModel: NotesViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)                      // viewModelScope tourne sur Main : dispatcher de test
        dao = FakeNoteDao(); workManager = FakeWorkManager()
        viewModel = NotesViewModel(SavedStateHandle(), NotesRepository(dao, FakeSyncApi(), workManager), Settings(FakeContext()))
    }
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun ajouterUneNoteVideLeBrouillonEtDemandeUneSync() = runTest(dispatcher) {
        viewModel.onDraftChange("  Source au pied du col  ")
        viewModel.notes.test {                               // Turbine : lire un StateFlow, un élément à la fois
            assertEquals(emptyList<Note>(), awaitItem())     // valeur initiale
            viewModel.addNote()
            assertEquals("Source au pied du col", awaitItem()[0].text)   // trim appliqué
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("", viewModel.draft.value)
        assertEquals(listOf("sync"), workManager.uniqueWorkNames)        // le travail unique « sync » est demandé
    }
}
```

`FakeWorkManager` (même dossier) : **classe Java**, le constructeur de `WorkManager` est `internal` en Kotlin, une classe Kotlin hors de la bibliothèque ne peut pas en hériter. Elle étend `WorkManager`, note le nom dans `uniqueWorkNames` quand `NotesRepository` appelle `enqueueUniqueWork`, et lève `UnsupportedOperationException` pour le reste de l'API. `FakeContext` (`ContextWrapper(null)`) suffit à construire `Settings` : DataStore ne touche au disque qu'à la première lecture, jamais ici. Dépendances : `kotlinx-coroutines-test` 1.10.2, Turbine 1.2.1.

```kotlin
// app/src/androidTest/…/NoteFormTest.kt — sur un appareil ou un émulateur : ./gradlew connectedDebugAndroidTest
class NoteFormTest {
    @get:Rule val composeRule = createComposeRule()          // import …compose.ui.test.junit4.v2.createComposeRule

    @Test fun leBoutonAjouterEstInactifTantQueLeBrouillonEstVide() {
        var draft by mutableStateOf("")
        var added = 0
        composeRule.setContent { CarnetTheme { NoteForm(draft = draft, onDraftChange = { draft = it }, onAdd = { added++ }) } }
        composeRule.onNodeWithText("Ajouter").assertIsNotEnabled()
        composeRule.onNodeWithText("Nouvelle note").performTextInput("Chêne vert marqué")
        composeRule.onNodeWithText("Ajouter").performClick()
        assertEquals("Chêne vert marqué", draft)
        assertEquals(1, added)
    }
}
```

---

## 5. Retrofit + OkHttp (cache, log)

```kotlin
// data/remote/SyncApi.kt — une interface, des annotations ; Retrofit génère l'implémentation
@Serializable data class NoteDto(val id: Long, val text: String, val createdAt: Long)
@Serializable data class EchoResponse(val url: String = "", val json: NoteDto? = null)   // champs inconnus ignorés

interface SyncApi {
    @POST("post") suspend fun push(@Body body: NoteDto): EchoResponse
}
```

```kotlin
// data/remote/Network.kt — les étages de cache reviennent (Rappel PWA : Cache-Control du Jour 1)
object Network {
    private val json = Json { ignoreUnknownKeys = true }

    fun okHttp(context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))   // 10 Mo, piloté par Cache-Control
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        // .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build()) }
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun syncApi(client: OkHttpClient): SyncApi = Retrofit.Builder()
        .baseUrl(BuildConfig.SYNC_BASE_URL)                                   // https://httpbin.org/ par défaut
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(SyncApi::class.java)
}
```

Jamais d'appel réseau sur le thread principal (`NetworkOnMainThreadException`) : `withContext(Dispatchers.IO)` dans le repository, `viewModelScope.launch` depuis le ViewModel. Pas de cookie : un jeton dans un en-tête (intercepteur).

---

## 6. WorkManager : worker, contraintes, unique, observer

```kotlin
// work/SyncWorker.kt — l'outbox : vide les notes non synchronisées quand l'OS accorde le réseau
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as CarnetApp).container.repository
        return try {
            val count = repository.syncPending()
            if (count > 0) Notifications.showSyncDone(applicationContext, count)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()   // backoff exponentiel par défaut
        }
    }
    companion object { const val UNIQUE_NAME = "sync" }
}
```

```kotlin
// data/NotesRepository.kt — demander le travail (unique, contrainte réseau) et l'exécuter
override fun enqueueSync() {
    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        // .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)   // passer devant Doze, avec repli
        .build()
    workManager.enqueueUniqueWork(SyncWorker.UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)   // KEEP : pas de doublon
}

suspend fun syncPending(): Int = withContext(Dispatchers.IO) {
    val pending = dao.unsynced(); val sent = mutableListOf<Long>()
    for (n in pending) { api.push(NoteDto(n.id, n.text, n.createdAt)); sent += n.id }   // une exception → retry
    if (sent.isNotEmpty()) dao.markSynced(sent)
    sent.size
}
```

```kotlin
// Observer l'état du travail (badge, Background Task Inspector le montre aussi)
workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.UNIQUE_NAME)   // Flow<List<WorkInfo>> : ENQUEUED, RUNNING, SUCCEEDED…
```

Doze à simuler : `adb shell dumpsys deviceidle force-idle` (rien ne part) · `adb shell dumpsys deviceidle unforce` (ça part). Sur Xiaomi/Samsung : « optimisation de la batterie » à désactiver par l'utilisateur. Le délai se **montre** (badge « n en attente »), il ne se cache pas.

---

## 7. Notifications : canal, builder, `PendingIntent`, service FCM

```kotlin
// notif/Notifications.kt — canaux créés une fois (Application.onCreate) ; sans canal, notify() ne montre RIEN
object Notifications {
    const val CHANNEL_SYNC = "sync"; const val CHANNEL_HUB = "hub"; const val EXTRA_NOTE_ID = "noteId"

    fun createChannels(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL_HUB, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(ctx.getString(R.string.channel_hub)).build())
        // CHANNEL_SYNC : IMPORTANCE_LOW
    }

    fun canNotify(ctx: Context): Boolean {   // Android 13+ : POST_NOTIFICATIONS est une permission runtime
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    fun showHub(ctx: Context, title: String, body: String, noteId: Long?) {
        if (!canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_HUB)
            .setSmallIcon(R.drawable.ic_notification)        // monochrome : blanc sur alpha
            .setContentTitle(title).setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(ctx, noteId))          // le clic (≈ notificationclick)
            .setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify((noteId ?: 2L).toInt(), n)
    }

    private fun openApp(ctx: Context, noteId: Long?): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (noteId != null) putExtra(EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getActivity(ctx, (noteId ?: 0L).toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
```

```kotlin
// notif/CarnetMessagingService.kt — reçu par l'OS via Play Services, app fermée ou tuée
class CarnetMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d("Carnet", "jeton FCM : $token")      // à coller dans la console Firebase (message de test)
        scope.launch { applicationContext.Settings.edit { it[CarnetApp.KEY_FCM_TOKEN] = token } }   // DataStore
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Carnet"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Notifications.showHub(applicationContext, title, body, message.data["noteId"]?.toLongOrNull())
    }
}
```

```kotlin
// MainActivity.handleIntent — le clic sur la notification
if (intent.hasExtra(Notifications.EXTRA_NOTE_ID)) viewModel.openNote(intent.getLongExtra(Notifications.EXTRA_NOTE_ID, -1L))
```

Message *notification* : affiché par l'OS app fermée (`onMessageReceived` n'est pas appelé en arrière-plan) ; message *data* : toujours traité par l'app. Repli pour tester : console Firebase → Messaging → « Send test message » avec le jeton.

---

## 8. Permissions : launcher, rationale, réglages, localisation, CameraX

```kotlin
// ui/permissions/PermissionFlow.kt — une permission n'est pas un booléen : quatre états
class PermissionFlow(private val activity: Activity, val permission: String) {
    enum class State { NotAsked, Granted, DeniedOnce, DeniedForever }
    private var askedOnce = false
    fun current(): State = when {
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED -> State.Granted
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> State.DeniedOnce
        askedOnce -> State.DeniedForever          // demandé, refusé, plus de rationale : le système se tait
        else -> State.NotAsked
    }
    fun shouldExplain() = current() == State.DeniedOnce
    fun onAsked() { askedOnce = true }
    fun onResult(granted: Boolean): State { askedOnce = true; return if (granted) State.Granted else current() }
    fun openSettings() = activity.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
}
```

```kotlin
// ui/permissions/PermissionButton.kt — le flux complet dans un bouton (extrait)
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
    state = flow.onResult(results[permissions.first()] == true)
    if (results.values.any { it }) onGranted(results)        // COARSE accordée, FINE refusée : « approximative »
}
when (state) {
    PermissionFlow.State.DeniedForever -> OutlinedButton(onClick = { flow.openSettings() }) { Text(stringResource(R.string.open_settings)) }
    else -> Button(onClick = {
        when {
            state == PermissionFlow.State.Granted -> onGranted(permissions.associateWith { true })
            flow.shouldExplain() -> showRationale = true     // AlertDialog, puis flow.onAsked(); launcher.launch(…)
            else -> { flow.onAsked(); launcher.launch(permissions.toTypedArray()) }
        }
    }) { Text(label) }
}
```

```kotlin
// data/location/LocationSource.kt — fused (Play Services) ; précise ou approximative selon ce qui a été accordé
@SuppressLint("MissingPermission")
suspend fun current(precise: Boolean): Fix? = suspendCancellableCoroutine { cont ->
    val priority = if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
    val cts = CancellationTokenSource()
    client.getCurrentLocation(priority, cts.token)
        .addOnSuccessListener { loc -> cont.resume(loc?.let { Fix(it.latitude, it.longitude, it.accuracy) }) }
        .addOnFailureListener { cont.resume(null) }
    cont.invokeOnCancellation { cts.cancel() }
}
```

```kotlin
// ui/notes/NoteDetailScreen.kt — CameraX : Preview (PreviewView) + ImageCapture liés au cycle de vie
val imageCapture = remember { ImageCapture.Builder().build() }
val previewView = remember { PreviewView(context) }
LaunchedEffect(Unit) {
    val provider = ProcessCameraProvider.getInstance(context).get()
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
}
AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(320.dp))
Button(onClick = { scope.launch { val path = PhotoStore(context).capture(imageCapture); … } }) { Text("Déclencher") }
```

```kotlin
// data/camera/PhotoStore.kt — takePicture → filesDir/photos ; FileProvider pour partager sans copier
suspend fun capture(imageCapture: ImageCapture): String = suspendCoroutine { cont ->
    val file = File(dir, "note-${System.currentTimeMillis()}.jpg")
    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = cont.resume(file.absolutePath)
            override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
        })
}
fun shareUri(path: String): Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", File(path))
```

Le raccourci sans permission : `ActivityResultContracts.TakePicture()` (la caméra système). Afficher la photo : `BitmapFactory.decodeFile(path, Options().apply { inSampleSize = 4 })` hors du thread principal, puis `Image(bitmap.asImageBitmap(), …)` ; Coil fait la même chose en une ligne (`AsyncImage(model = File(path))`).

---

## 9. App Link + `assetlinks.json` à deux empreintes

```json
// https://<pwa>/.well-known/assetlinks.json — une entrée par (package, clé) : Carnet (clé debug du cours) + la TWA du TD A0
[
  { "relation": ["delegate_permission/common.handle_all_urls"],
    "target": { "namespace": "android_app", "package_name": "corsica.universita.carnet",
                "sha256_cert_fingerprints": ["07:B4:8C:EF:9D:A3:75:AC:A1:78:3E:08:A6:0D:9F:1F:12:AC:F8:C8:50:9F:BC:C0:6E:5B:AD:1D:69:3E:00:CD"] } },
  { "relation": ["delegate_permission/common.handle_all_urls"],
    "target": { "namespace": "android_app", "package_name": "app.netlify.ma_pwa.twa",
                "sha256_cert_fingerprints": ["<empreinte de la clé TWA : keytool -list -v -keystore android.keystore>"] } }
]
```

Pour la **release** (clé générée au § 10) : ajouter une **troisième** empreinte (`apksigner verify --print-certs app-release.apk`), sinon l'App Link n'est vérifié que pour la build debug.

```bash
./gradlew signingReport                                     # empreintes de chaque signingConfig
adb shell pm verify-app-links --re-verify corsica.universita.carnet
adb shell pm get-app-links corsica.universita.carnet       # → verified (sinon none / legacy_failure)
adb shell am start -a android.intent.action.VIEW -d https://<pwa>/   # doit ouvrir Carnet, pas Chrome
```

```kotlin
// MainActivity.kt — recevoir l'Intent (onCreate au premier lancement seulement, puis onNewIntent avec singleTop)
override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
private fun handleIntent(intent: Intent) = when (intent.action) {
    Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { viewModel.receiveSharedText(it) }
    Intent.ACTION_VIEW -> intent.data?.let { viewModel.receiveSharedText(it.toString()) }
    else -> null
}
```

---

## 10. Signature release et `versionCode`

```bash
keytool -genkeypair -v -keystore release.jks -alias carnet -keyalg RSA -keysize 2048 -validity 10000
# À GARDER (et son mot de passe) : perdre la clé = ne plus jamais mettre à jour l'app (Jour 4, chapitre 1)
```

```properties
# local.properties (jamais commité ; *.jks dans .gitignore)
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=carnet
RELEASE_KEY_PASSWORD=…
```

```kotlin
// app/build.gradle.kts
val localProps = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun prop(name: String, default: String) = (localProps.getProperty(name) ?: System.getenv(name) ?: default).trim()

android {
    defaultConfig { versionCode = 7; versionName = "1.0.0" }   // versionCode : entier, MONOTONE, +1 à chaque livraison
    signingConfigs {
        create("release") {
            if (prop("RELEASE_STORE_FILE", "").isNotEmpty()) {
                storeFile = rootProject.file(prop("RELEASE_STORE_FILE", ""))
                storePassword = prop("RELEASE_STORE_PASSWORD", ""); keyAlias = prop("RELEASE_KEY_ALIAS", ""); keyPassword = prop("RELEASE_KEY_PASSWORD", "")
            } else initWith(getByName("debug"))               // sans clé : signée debug (s'installe, sans identité produit)
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true; isShrinkResources = true  // R8 : 27 Mo → 3 Mo sur Carnet
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

```
# app/proguard-rules.pro — ce qui dépend de NOTRE code (les bibliothèques embarquent leurs règles)
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, SourceFile, LineNumberTable
-keep,includedescriptorclasses class corsica.universita.carnet.**$$serializer { *; }     # kotlinx.serialization
-keepclassmembers class corsica.universita.carnet.** { *** Companion; }
-keepclasseswithmembers class corsica.universita.carnet.** { kotlinx.serialization.KSerializer serializer(...); }
-keep interface corsica.universita.carnet.data.remote.SyncApi { *; }                      # Retrofit
-keep class corsica.universita.carnet.data.local.** { *; }                                # Room
```

```bash
./gradlew assembleRelease                      # → app/build/outputs/apk/release/app-release.apk + mapping/release/mapping.txt
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk   # INSTALL_FAILED_UPDATE_INCOMPATIBLE si la debug est installée : désinstaller d'abord
```

Garder `mapping.txt` avec chaque release : c'est lui qui rend les traces R8 lisibles (Crashlytics l'envoie avec `mappingFileUploadEnabled`).

---

## 11. `adb` utiles

```bash
adb devices                                                   # l'appareil est vu (device, pas unauthorized)
adb install -r app-release.apk · adb uninstall corsica.universita.carnet
adb logcat -s Carnet                                          # le tag unique du projet (ou Logcat : tag:Carnet)
adb shell am kill corsica.universita.carnet                   # la mort du process (app en arrière-plan)
adb shell am force-stop corsica.universita.carnet             # l'app « fermée par geste » (WorkManager repart au prochain lancement)
adb shell settings put system user_rotation 1                 # rotation forcée (0 pour revenir)
adb shell dumpsys deviceidle force-idle · step · unforce       # Doze : rien ne part · fenêtre de maintenance · retour
adb shell cmd jobscheduler run -f corsica.universita.carnet <jobId>   # forcer un travail WorkManager (jobId dans dumpsys jobscheduler)
adb shell pm get-app-links corsica.universita.carnet          # verified / none / legacy_failure
adb shell pm verify-app-links --re-verify corsica.universita.carnet
adb shell pm revoke corsica.universita.carnet android.permission.CAMERA   # rejouer le flux de permission (grant pour accorder)
adb shell appops set corsica.universita.carnet RUN_ANY_IN_BACKGROUND deny # simuler une restriction d'arrière-plan OEM
adb shell am start -a android.intent.action.VIEW -d https://<pwa>/        # l'App Link
adb shell dumpsys package corsica.universita.carnet | grep -A3 "granted=" # permissions accordées
```

---

## 12. Checklist avant la démo

- [ ] **Release signée** avec sa propre clé (`apksigner verify --print-certs` ≠ `androiddebugkey`), clé et mot de passe **sauvegardés**
- [ ] **`versionCode` incrémenté** par rapport à la version installée chez soi
- [ ] **Hors ligne** testé en **mode avion** : l'app s'ouvre, les notes s'affichent, l'action impossible le dit
- [ ] **Migration** testée : installer la version précédente, créer des notes, installer la nouvelle → les notes sont là
- [ ] **Synchronisation** : notes créées en mode avion, réseau rétabli → elles partent sans action (Background Task Inspector)
- [ ] **Notification** reçue **app tuée** (`am force-stop`, puis un message de test depuis la console Firebase) ; le clic ouvre le bon écran
- [ ] **Permission** jusqu'au refus définitif : refuser deux fois → l'app propose les réglages, ne se tait pas
- [ ] **App Link** : `pm get-app-links` → `verified` ; l'URL de la PWA ouvre l'app
- [ ] **Icône adaptive + splash** : l'icône prend la forme du launcher, pas d'écran blanc au lancement
- [ ] **Capacité native** démontrable en **5 min**, démo répétée une fois
- [ ] **Dépôt Git** poussé, `README` à jour (construire, lancer)
