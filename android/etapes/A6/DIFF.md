# Étape A6 — Les notifications

État du projet Carnet **à la fin du TD A6** (Jour 5, chapitre 5). Par rapport à `android/etapes/A5/` :

## Fichiers ajoutés

- `app/src/main/java/corsica/universita/carnet/notif/Notifications.kt` — `object Notifications` : `CHANNEL_SYNC = "sync"`, `CHANNEL_HUB = "hub"`, `EXTRA_NOTE_ID` ; `createChannels(ctx)` (`NotificationChannelCompat`, importance LOW / HIGH) ; `areEnabled(ctx)` ; `showSyncDone(ctx, count)` ; `showHub(ctx, title, body, noteId)` (`NotificationCompat.Builder`, `BigTextStyle`, `setAutoCancel`) ; `PendingIntent` vers `MainActivity` avec l'extra `noteId` (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`, `requestCode` = id de la note).
- `app/src/main/java/corsica/universita/carnet/notif/CarnetMessagingService.kt` — `class CarnetMessagingService : FirebaseMessagingService()` : `onNewToken` → Logcat + DataStore ; `onMessageReceived` → `Notifications.showHub` (titre / corps / `noteId` lus dans `data`, ou dans `notification` si présent).
- `app/src/main/java/corsica/universita/carnet/data/local/Settings.kt` — `class Settings(context)` : Preferences DataStore `settings`, `fcmToken: Flow<String?>`, `saveFcmToken(token)`.
- `app/src/main/res/drawable/ic_notification.xml` — icône de notification monochrome.

## Fichiers modifiés

- `AndroidManifest.xml` — `<uses-permission POST_NOTIFICATIONS>` ; `<service .notif.CarnetMessagingService exported="false">` avec l'action `com.google.firebase.MESSAGING_EVENT` ; `meta-data` `default_notification_channel_id = hub` et `default_notification_icon`.
- `CarnetApp.kt` — `CarnetContainer.settings` ; `Notifications.createChannels(this)` dans `onCreate`.
- `work/SyncWorker.kt` — `Notifications.showSyncDone(applicationContext, count)` après une sync qui a envoyé au moins une note.
- `MainActivity.kt` — `handleIntent` : extra `noteId` (clic sur une notification) → `viewModel.openNote(id)`.
- `ui/notes/NotesViewModel.kt` — reçoit `Settings` ; `openNoteId: StateFlow<Long?>` (`SavedStateHandle`) ; `openNote(id)` ; `fcmToken: StateFlow<String?>` ; `refreshFcmToken()` (`FirebaseMessaging.getInstance().token`).
- `ui/notes/NotesListScreen.kt` — `NotificationsRow` : bouton « Activer les notifications » (`rememberLauncherForActivityResult(RequestPermission())`, Android 13+ ; après un refus : « Réglages des notifications ») + « Copier mon jeton » ; la note visée par le clic est surlignée ; deux paramètres de plus : `onCopyToken`, `onOpenNotificationSettings`.
- `ui/notes/NoteIntents.kt` — `Context.copyToClipboard(label, text)`, `Context.openNotificationSettings()` (`ACTION_APP_NOTIFICATION_SETTINGS`).
- `ui/CarnetNavigation.kt` — fournit les deux nouveaux événements.
- `res/values/strings.xml` — canaux, textes de notification, boutons.

## Fichiers supprimés

- `notif/README.md` (« vide jusqu'au TD A6 »).

## Ce que fait Carnet à la fin

Une notification locale (canal `sync`) à la fin d'une synchronisation ; un push **data** envoyé par un serveur (ou un message de test depuis la console Firebase) arrive app fermée, par `CarnetMessagingService`, sur le canal `hub` ; le clic ouvre Carnet et surligne la note visée (`noteId`) ; les réglages système montrent les deux canaux. Sans permission (Android 13+) ou app coupée dans les réglages : rien n'est affiché, Logcat le dit, et le bouton renvoie vers les réglages.

Le push attend le vrai `google-services.json` du projet `cours-pwa-b0358` (celui du starter est factice). Pour tester : le jeton copié dans la console Firebase, « Envoyer un message de test » ; un message **data** `{ title, body, noteId }` part d'un serveur, vers ce jeton.

## Compilation

`./gradlew assembleDebug --no-daemon --console=plain --max-workers=1` → `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk`.
