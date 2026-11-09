# Règles R8 de Carnet (Jour 6, chapitre 2). R8 lit aussi les règles embarquées dans les
# bibliothèques AndroidX, Room, OkHttp et Retrofit (META-INF/proguard) : ce fichier ne garde
# que ce qui dépend de NOTRE code.

# --- kotlinx.serialization : les classes @Serializable et leurs sérialiseurs générés ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class corsica.universita.carnet.**$$serializer { *; }
-keepclassmembers class corsica.universita.carnet.** { *** Companion; }
-keepclasseswithmembers class corsica.universita.carnet.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Retrofit : les interfaces d'API et les signatures génériques des méthodes suspend ---
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface corsica.universita.carnet.data.remote.SyncApi { *; }

# --- Room : les entités et la base (les règles de Room gardent déjà les classes générées) ---
-keep class corsica.universita.carnet.data.local.** { *; }

# --- Crashlytics : traces lisibles (numéros de ligne) une fois mapping.txt envoyé ---
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
