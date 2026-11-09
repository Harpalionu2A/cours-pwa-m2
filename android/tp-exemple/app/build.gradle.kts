import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)      // Jour 6 : rapports de plantage
    alias(libs.plugins.firebase.appdistribution)  // Jour 6 : distribution hors store (optionnelle)
}

// Valeurs propres à chaque étudiant, lues dans local.properties (jamais commité) :
//   PWA_HOST=ma-pwa.netlify.app        → hôte de sa PWA du Jour 3 (App Link, TD A3)
//   SYNC_BASE_URL=https://httpbin.org/ → cible de l'outbox (TD A5)
//   RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD → clé release (Jour 6)
//   CRASHLYTICS_UPLOAD_MAPPING=false   → tant que google-services.json est factice (Jour 6)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val pwaHost: String = localProps.getProperty("PWA_HOST") ?: "exemple.netlify.app"
val syncBaseUrl: String = localProps.getProperty("SYNC_BASE_URL") ?: "https://httpbin.org/"
val releaseStoreFile: String = localProps.getProperty("RELEASE_STORE_FILE")?.trim().orEmpty()
val uploadMapping: Boolean = (localProps.getProperty("CRASHLYTICS_UPLOAD_MAPPING") ?: "true").trim() == "true"

android {
    namespace = "corsica.universita.carnet"
    compileSdk = 37

    defaultConfig {
        applicationId = "corsica.universita.carnet"
        minSdk = 26
        targetSdk = 37
        versionCode = 7                // entier MONOTONE : +1 à chaque livraison (Jour 6, chapitre 1)
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PWA_HOST", "\"$pwaHost\"")
        buildConfigField("String", "SYNC_BASE_URL", "\"$syncBaseUrl\"")
        manifestPlaceholders["pwaHost"] = pwaHost
    }

    // Keystore debug du cours : tous les étudiants signent avec la même clé,
    // donc une seule empreinte SHA-256 à déclarer (assetlinks.json, Firebase).
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Clé release (Jour 6) : lue dans local.properties, jamais dans le dépôt (*.jks est ignoré).
        // Sans RELEASE_STORE_FILE, la release est signée avec la clé debug : l'APK s'installe,
        // mais n'a pas d'identité « produit » (la signature est une identité à vie).
        create("release") {
            if (releaseStoreFile.isNotEmpty()) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debug") }
        release {
            isMinifyEnabled = true         // R8 : réduire, obscurcir (Jour 6, chapitre 2)
            isShrinkResources = true       // retirer les ressources jamais référencées
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                // mapping.txt envoyé à Crashlytics pour lire les traces obscurcies.
                mappingFileUploadEnabled = uploadMapping
            }
            // Firebase App Distribution : ./gradlew assembleRelease appDistributionUploadRelease
            // (compte de service dans FIREBASE_SERVICE_CREDENTIALS, groupe de testeurs « promo-m2 »).
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "promo-m2"
                releaseNotes = "Carnet — build de référence du TP"
                System.getenv("FIREBASE_SERVICE_CREDENTIALS")?.takeIf { it.isNotEmpty() }?.let { serviceCredentialsFile = it }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Tests JVM : android.util.Log & co renvoient 0/null au lieu de lever « Stub! » (Jour 6, chapitre 2)
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md") }
}

room { schemaDirectory("$projectDir/schemas") }

// Toutes les dépendances des TD A1 à A7 sont déjà déclarées : le premier sync Gradle
// télécharge tout, une fois, avant le 2 novembre.
dependencies {
    // Compose + Material 3
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Activity, Lifecycle, Navigation 3
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Core, splash
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    // Room (TD A4), WorkManager (TD A5), DataStore
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)

    // Réseau (TD A5)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)

    // Firebase Cloud Messaging (TD A6)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)     // Jour 6

    // CameraX et localisation (TD A7)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.compose)
    implementation(libs.play.services.location)

    // Tests (Jour 6, chapitre 2)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
