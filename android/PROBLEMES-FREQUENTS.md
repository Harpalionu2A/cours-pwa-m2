# Problèmes fréquents — Partie II (Android)

Journal des problèmes rencontrés en construisant le module (agents J4, J5, J6), puis par Bat et les étudiants. Chaque entrée : symptôme · cause · remède. La version courte pour les étudiants est dans `annexe-setup-android.md` (slide « Problèmes fréquents ») et `android/starter/README.md` § 9.

## Build Gradle (environnement de construction, octobre 2026)

| Symptôme | Cause | Remède |
|---|---|---|
| `429 Too Many Requests` pendant la résolution des dépendances | Maven Central derrière un proxy ; trop de requêtes d'un coup | Relancer **la même** commande : les artefacts déjà résolus sont en cache. Ne pas changer de dépôt ni de version. |
| « serialization error » au configuration cache avec AGP 9.4.1 | Configuration cache instable avec AGP 9.4 dans cet environnement | `org.gradle.configuration-cache=false` dans `gradle.properties` (c'est le réglage livré dans le starter). |
| Build lent, machine à 2 CPU / 7 Go | Plusieurs builds Gradle en parallèle | Un seul build à la fois : `--no-daemon --max-workers=1` ; `org.gradle.jvmargs=-Xmx2g`. |
| `BUILD SUCCESSFUL` mais `compileDebugKotlin FROM-CACHE` | Build cache Gradle (`org.gradle.caching=true`) : une compilation identique a déjà eu lieu | Normal. Pour forcer une vraie compilation : `--no-build-cache --rerun-tasks`. |
| `rsync: command not found` dans l'environnement | Outil absent | `tar --exclude=build --exclude=.gradle … -cf - . \| (cd cible && tar xf -)`. |

## Android Studio et SDK (étudiants)

| Symptôme | Cause | Remède |
|---|---|---|
| « Unsupported Java » / « Gradle requires JVM 17 or later » | Un ancien JDK dans `JAVA_HOME` ou choisi dans les réglages | Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK : jbr-21** (le JDK d'Android Studio). |
| « This version of Android Studio cannot open this project » / « AGP 9.4.1 requires Android Studio … » | Android Studio trop ancien (Panda, Otter) | Mettre à jour vers **Quail 4 Patch 1 (2026.1.4)** ou plus récent. |
| Sync bloqué sur « Resolving dependencies » | Proxy / pare-feu (fac, entreprise) | Settings → Appearance & Behavior → System Settings → HTTP Proxy ; ou dans `gradle.properties` : `systemProp.https.proxyHost=…`, `systemProp.https.proxyPort=…`. Réessayer sur une autre connexion. |
| « SDK location not found » | `local.properties` absent ou mauvais `sdk.dir` | Android Studio le crée à l'ouverture ; sinon `sdk.dir=<ANDROID_HOME>` (Windows : doubler les `\`). |
| `adb: command not found` | `platform-tools` absent du `PATH` | Annexe setup, slide « SDK Manager » ; rouvrir le terminal. |
| `adb devices` → `unauthorized` | Boîte de dialogue « Autoriser le débogage USB » non acceptée | Débrancher, rebrancher, accepter sur le téléphone (« Toujours autoriser »). |
| `adb devices` → rien | Câble de charge seul (pas de données), pilote Windows, mode USB | Changer de câble ; Windows : pilote USB du constructeur ; téléphone : mode USB « Transfert de fichiers ». |
| Xiaomi / Redmi / POCO : « Installation via USB » refusée, ou l'app n'apparaît pas | Sécurité MIUI / HyperOS | Options développeur → **Installer via USB** et **Débogage USB (paramètres de sécurité)** (compte Mi requis). |
| Émulateur très lent, « HAXM is not installed », « Hypervisor » | Virtualisation désactivée | BIOS/UEFI : Intel VT-x / AMD-V ; Windows : activer Hyper-V ou Windows Hypervisor Platform, redémarrer. HAXM n'existe plus. |
| Émulateur : pas de notification FCM (TD A6) | Image *Google APIs* sans Play Store | Créer l'AVD avec l'image **Google Play** (colonne *Play Store*). |
| « Daemon will be stopped at the end of the build after running out of JVM memory » | `-Xmx2g` trop juste | `org.gradle.jvmargs=-Xmx4g` dans `gradle.properties` (machine ≥ 16 Go). |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Un Carnet signé avec une autre clé est déjà installé (le `debug.keystore` d'Android Studio au lieu de celui du cours, ou l'inverse) | `adb uninstall corsica.universita.carnet` puis réinstaller. C'est la démo du Jour 4, chapitre 1 (« La signature : une identité à vie »). |
| Android Studio propose de mettre à jour AGP / Kotlin / les bibliothèques | Assistant de mise à jour | Refuser pendant le module : les versions sont épinglées dans `gradle/libs.versions.toml` et vérifiées ensemble. |

## TD A0 (TWA, Bubblewrap)

| Symptôme | Cause | Remède |
|---|---|---|
| `bubblewrap init` veut télécharger un JDK et un SDK | Première exécution, chemins inconnus | Indiquer le JBR d'Android Studio (`…/jbr`) et `ANDROID_HOME` ; `bubblewrap updateConfig --jdkPath … --androidSdkPath …`. |
| La TWA s'ouvre avec la barre d'adresse de Chrome | `assetlinks.json` absent, mal placé, ou mauvaise empreinte | Le fichier à `https://<pwa>/.well-known/assetlinks.json`, servi en `application/json` ; l'empreinte de **la clé qui a signé l'APK** (`keytool -list -v`). Puis `adb shell pm verify-app-links --re-verify <package>`. |
| `bubblewrap build` échoue sur la validation de la PWA | Manifest incomplet (icônes 192/512, `start_url`), Lighthouse | Corriger le manifest ; en dépannage, `bubblewrap build --skipPwaValidation`. |

## TD A3 (App Link)

| Symptôme | Cause | Remède |
|---|---|---|
| `pm get-app-links` → `none` ou `legacy_failure` | Empreinte de la clé debug absente d'`assetlinks.json`, ou fichier non redéployé | Deuxième entrée (`corsica.universita.carnet` + empreinte `./gradlew signingReport`), redéployer, `pm verify-app-links --re-verify`. |
| Le lien ouvre toujours le navigateur malgré `verified` | Réglage utilisateur « Ouvrir les liens compatibles » désactivé, ou lien ouvert depuis Chrome lui-même | Paramètres → Applications → Carnet → Ouvrir par défaut ; tester avec `adb shell am start -a android.intent.action.VIEW -d https://<pwa>/` ou depuis Messages. |
| « Ouvrir ma PWA » rouvre Carnet | Une fois l'App Link vérifié, `ACTION_VIEW` revient à l'app | Le corrigé utilise `Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_BROWSER)` pour demander le navigateur. |
