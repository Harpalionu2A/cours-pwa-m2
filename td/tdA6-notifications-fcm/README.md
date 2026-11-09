# TD A6 — Les notifications (40 min)

Jour 5 — jeudi 5 novembre 2026, chapitre 5. Point de départ : son projet à la fin du TD A5 (ou `android/etapes/A5/`).

## Objectif

Une notification locale à la fin d'une synchronisation ; puis un push FCM envoyé depuis la console Firebase, reçu Carnet fermée, dont le clic ouvre l'app.

## Fonctionnalités attendues

- [ ] 1. `notif/Notifications.kt` : canaux `sync` et `hub` (`NotificationChannelCompat`), créés dans `CarnetApp.onCreate()`
- [ ] 2. Permission `POST_NOTIFICATIONS` (manifest + bouton « Activer les notifications » → `rememberLauncherForActivityResult(RequestPermission())`) ; après un refus, le bouton ouvre les réglages de notification de l'app
- [ ] 3. `Notifications.showSyncDone(ctx, count)` appelé par `SyncWorker` quand au moins une note est partie
- [ ] 4. `notif/CarnetMessagingService.kt` (`FirebaseMessagingService`, déclaré dans le manifest) : `onNewToken` → Logcat + DataStore ; `onMessageReceived` → `Notifications.showHub(title, body, noteId)` ; bouton « Copier mon jeton »
- [ ] 5. Jeton copié → console Firebase → Messaging → nouvelle campagne (notification) → « Envoyer un message de test » → coller le jeton
- [ ] 6. `PendingIntent` vers `MainActivity` avec l'extra `noteId` ; `handleIntent` ouvre la note visée

Textes dans `strings.xml` (canaux, titres, boutons).

## Validation

- Mode avion, une note, mode avion coupé : la note part **et une notification « Notes synchronisées »** apparaît (canal `sync`)
- Paramètres → Applications → Carnet → Notifications : les canaux **Synchronisation** et **Messages** sont visibles et réglables
- Jeton copié, message de test envoyé depuis la console Firebase ; Carnet **fermée par geste** : la notification arrive (affichée par l'OS) ; le clic ouvre Carnet
- `adb shell am start -n corsica.universita.carnet/.MainActivity --el noteId 3` : Carnet s'ouvre sur la note 3 (le même extra que le `PendingIntent`)
- Logcat : `Jeton FCM : …`, `Push reçu de … : « … » (note 3)`, `Notification 3 affichée`, `Clic sur une notification : note 3`

## L'échec à vivre

**(1) Pas de canal.** Dans `showSyncDone`, un `NotificationCompat.Builder(ctx, "sync")` **sans** avoir créé le canal (commenter `createChannels`). Synchroniser : **rien à l'écran, aucune exception.** Logcat (sans filtre) : `NotificationService: No Channel found for pkg=corsica.universita.carnet, channelId=sync` (et, en debug, un Toast « Developer warning for package … Failed to post notification on channel "sync" »). Depuis Android 8 (2017), une notification sans canal est jetée en silence. **Réparation :** `createChannels` dans `Application.onCreate`, avant toute notification.

**(2) Pas de permission.** Android 13+ : refuser le dialogue « Autoriser Carnet à envoyer des notifications ? ». Synchroniser : rien non plus — `notify()` ne fait rien, sans exception (lint signale seulement l'appel sans `areNotificationsEnabled()`). **Réparation :** `NotificationManagerCompat.areNotificationsEnabled()` avant de poster ; dans l'UI, expliquer, demander après une action, et après un refus envoyer vers les réglages (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`) — le dialogue système ne se représente pas éternellement. Version courte : un refus → réglages ; la machine complète (rationale, deuxième demande) au TD A7, `PermissionFlow`.

## La réparation

```kotlin
// CarnetApp.onCreate()
Notifications.createChannels(this)

// Notifications.kt
fun createChannels(ctx: Context) {
    val m = NotificationManagerCompat.from(ctx)
    m.createNotificationChannel(NotificationChannelCompat.Builder("sync", NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(ctx.getString(R.string.channel_sync_name)).build())
    m.createNotificationChannel(NotificationChannelCompat.Builder("hub", NotificationManagerCompat.IMPORTANCE_HIGH)
        .setName(ctx.getString(R.string.channel_hub_name)).build())
}
fun areEnabled(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
```

## Aide

```kotlin
// Notifications.kt — construire, et le clic
fun showHub(ctx: Context, title: String, body: String, noteId: Long?) {
    val intent = Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        noteId?.let { putExtra("noteId", it) }
    }
    val pending = PendingIntent.getActivity(ctx, noteId?.toInt() ?: 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val n = NotificationCompat.Builder(ctx, "hub")
        .setSmallIcon(R.drawable.ic_notification)      // monochrome : l'OS la teinte
        .setContentTitle(title).setContentText(body)
        .setContentIntent(pending).setAutoCancel(true).build()
    if (areEnabled(ctx)) NotificationManagerCompat.from(ctx).notify(noteId?.toInt() ?: 0, n)
}
```

```kotlin
// CarnetMessagingService.kt
class CarnetMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { Log.d("Carnet", "Jeton FCM : $token") /* + DataStore */ }
    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        Notifications.showHub(this, d["title"] ?: "Carnet", d["body"] ?: "", d["noteId"]?.toLongOrNull())
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
…
<service android:name=".notif.CarnetMessagingService" android:exported="false">
    <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>
</service>
```

```kotlin
// NotesListScreen.kt — la permission (Android 13+) ; version courte : un refus → réglages
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> … }
if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)

// MainActivity.handleIntent — le clic
if (intent.hasExtra("noteId")) viewModel.openNote(intent.getLongExtra("noteId", -1L))
```

Le jeton : `onNewToken` n'est appelé qu'à sa création ; pour le relire, `FirebaseMessaging.getInstance().token.addOnSuccessListener { … }`. Le message de test de la console Firebase est un message *notification* : app en arrière-plan ou fermée, il est affiché automatiquement par la bibliothèque Firebase (sans passer par `onMessageReceived`), sur le canal `hub` (« Messages ») déclaré en `meta-data`, sans `noteId`. Un message **data** `{ title, body, noteId }` passe toujours par `onMessageReceived`, app fermée ou non, mais il faut un serveur pour l'envoyer (API FCM v1).

Accès à la console : être membre du projet Firebase du cours, ou créer son propre projet Firebase (gratuit), y ajouter l'app Android `corsica.universita.carnet` et remplacer `app/google-services.json`.

## Commandes

```bash
adb logcat -s Carnet                                              # jeton, push reçu, clic
adb logcat | grep -i "No Channel found"                            # l'échec (1)
adb shell dumpsys notification --noredact | grep -A5 corsica       # les notifications postées, les canaux
adb shell pm revoke corsica.universita.carnet android.permission.POST_NOTIFICATIONS
adb shell am force-stop corsica.universita.carnet                  # « fermée par geste » : l'app ne tourne plus, le push arrive quand même
```

**Corrigé :** `corrige/` → `android/etapes/A6/`. Le push réel exige le vrai `google-services.json` du projet `cours-pwa-b0358` dans `app/` et une image d'émulateur **Google Play** (ou un téléphone avec Play Services).
