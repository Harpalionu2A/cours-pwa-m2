# TD A3 — Intents et App Link (45 min)

Jour 4 — lundi 2 novembre 2026. Point de départ : son projet à la fin du TD A2 (ou `android/etapes/A2/`), et l'`assetlinks.json` déposé sur sa PWA au TD A0.

## Objectif

Partager, ouvrir, recevoir : trois Intents. Puis l'**App Link** : l'URL de sa PWA ouvre Carnet au lieu du navigateur.

## Fonctionnalités attendues

- [ ] 1. Une icône « Partager » sur chaque note : `ACTION_SEND` + `Intent.createChooser` → le sélecteur du système
- [ ] 2. Un bouton « Ouvrir ma PWA » dans la barre : `ACTION_VIEW` sur `https://<PWA_HOST>/` → le navigateur
- [ ] 3. Un intent-filter `SEND` `text/plain` sur `MainActivity` : depuis Chrome (ou n'importe quelle app), « Partager » → Carnet apparaît dans le sélecteur, et le texte reçu **préremplit le brouillon**
- [ ] 4. Un intent-filter `VIEW` `https://<PWA_HOST>` avec `android:autoVerify="true"`, et une **deuxième empreinte** (la clé debug du cours) dans `assetlinks.json`

`PWA_HOST` est lu dans `local.properties` (`PWA_HOST=ma-pwa.netlify.app`) : `BuildConfig.PWA_HOST` dans le code, `${pwaHost}` dans le manifest. Le ViewModel reçoit `receiveSharedText(text: String)`.

## Validation

- Partager une note vers Messages, Gmail, Keep… : le texte arrive
- Depuis Chrome, menu → Partager → **Carnet** : Carnet s'ouvre (ou revient au premier plan), le brouillon contient le texte partagé
- `adb shell pm get-app-links corsica.universita.carnet` → `verified` pour l'hôte de la PWA
- Un lien vers sa PWA, tapé dans une note du Class Hub (ou envoyé par Messages), ouvre **Carnet**, pas le navigateur ; Logcat : `ACTION_VIEW reçu (App Link)`

## L'échec à vivre

Avec l'intent-filter `VIEW` en place et `autoVerify`, mais **sans la deuxième empreinte** dans `assetlinks.json` :

- `adb shell pm get-app-links corsica.universita.carnet` → `none` ou `legacy_failure` (ou `1024` : *unverified*)
- Le lien vers la PWA ouvre **le navigateur**. L'OS a bien téléchargé `assetlinks.json` à l'installation, mais l'empreinte de la clé qui a signé Carnet (la clé debug du cours) n'y est pas : la TWA du TD A0 y est, pas Carnet. Sans preuve de propriété, l'OS ne donne pas à une app le droit de capter les URL d'un site.

## La réparation

1. L'empreinte de la clé debug du cours : `./gradlew signingReport` (variante *debug*, ligne `SHA-256`) — la même pour toute la promo : `07:B4:8C:EF:9D:A3:75:AC:A1:78:3E:08:A6:0D:9F:1F:12:AC:F8:C8:50:9F:BC:C0:6E:5B:AD:1D:69:3E:00:CD`
2. Une **deuxième entrée** dans `assetlinks.json` (`package_name` : `corsica.universita.carnet`), redéployer la PWA
3. `adb shell pm verify-app-links --re-verify corsica.universita.carnet`, puis `pm get-app-links` → `verified`

## Aide

```xml
<!-- AndroidManifest.xml, dans <activity android:name=".MainActivity" android:launchMode="singleTop" …> -->
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>

<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="${pwaHost}" />
</intent-filter>
```

```kotlin
// Partager (ACTION_SEND, implicite : l'OS propose qui accepte text/plain)
val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, note.text) }
context.startActivity(Intent.createChooser(send, "Partager la note via…"))

// Ouvrir une URL
context.startActivity(Intent(Intent.ACTION_VIEW, "https://${BuildConfig.PWA_HOST}/".toUri()))
```

```kotlin
// MainActivity.kt — recevoir
private val viewModel: NotesViewModel by viewModels()

override fun onCreate(savedInstanceState: Bundle?) {
    …
    if (savedInstanceState == null) handleIntent(intent)   // pas après une rotation
}
override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }

private fun handleIntent(intent: Intent) = when (intent.action) {
    Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { viewModel.receiveSharedText(it) }
    Intent.ACTION_VIEW -> intent.data?.let { viewModel.receiveSharedText(it.toString()) }
    else -> null
}
```

```json
// .well-known/assetlinks.json — deux entrées : la TWA (TD A0) et Carnet (clé debug du cours)
[
  { "relation": ["delegate_permission/common.handle_all_urls"],
    "target": { "namespace": "android_app", "package_name": "app.netlify.ma_pwa.twa",
                "sha256_cert_fingerprints": ["<empreinte de la clé TWA>"] } },
  { "relation": ["delegate_permission/common.handle_all_urls"],
    "target": { "namespace": "android_app", "package_name": "corsica.universita.carnet",
                "sha256_cert_fingerprints": ["07:B4:8C:EF:9D:A3:75:AC:A1:78:3E:08:A6:0D:9F:1F:12:AC:F8:C8:50:9F:BC:C0:6E:5B:AD:1D:69:3E:00:CD"] } }
]
```

Une fois Carnet vérifié pour cet hôte, `ACTION_VIEW` sur l'URL de la PWA **revient à Carnet** : pour que « Ouvrir ma PWA » ouvre vraiment le navigateur, le corrigé utilise `Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)` avec `data = url`.

## Commandes

```bash
./gradlew signingReport                                              # empreinte SHA-256 de la clé debug
adb shell pm get-app-links corsica.universita.carnet                 # none / legacy_failure / verified
adb shell pm verify-app-links --re-verify corsica.universita.carnet  # relancer la vérification (Android 12+)
adb shell am start -a android.intent.action.VIEW -d https://<pwa>/   # simuler le clic sur le lien
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "Bonjour"   # simuler un partage
adb logcat -s Carnet
```

Les réglages de l'utilisateur comptent aussi : Paramètres → Applications → Carnet → **Ouvrir par défaut** → « Ouvrir les liens compatibles » doit être activé (c'est le cas après une vérification réussie).

**Corrigé :** `corrige/` → `android/etapes/A3/` (avec `assetlinks.example.json`).
