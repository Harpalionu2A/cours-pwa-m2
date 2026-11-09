package corsica.universita.carnet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * La base SQLite de Carnet, vue par Room. `exportSchema = true` : le plugin Room écrit le schéma de
 * chaque version dans `app/schemas/` (versionné dans git) — la mémoire de ce qui vit chez les utilisateurs.
 *
 * v1 : `notes(id, text, createdAt)` · v2 : + `pinned` (TD A4) · v3 : + `synced` (TD A5) · v4 : + `lat`, `lng`, `photoPath` (TD A7).
 */
@Database(entities = [NoteEntity::class], version = 4, exportSchema = true)
abstract class CarnetDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        /** v1 → v2 : la colonne ajoutée, avec une valeur pour les lignes existantes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v2 → v3 : l'outbox. Les notes existantes ne sont pas synchronisées : elles partiront. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v3 → v4 : position et photo. Colonnes NULL : les notes existantes n'en ont pas, et c'est juste. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN lat REAL")
                db.execSQL("ALTER TABLE notes ADD COLUMN lng REAL")
                db.execSQL("ALTER TABLE notes ADD COLUMN photoPath TEXT")
            }
        }

        fun build(context: Context): CarnetDatabase =
            Room.databaseBuilder(context.applicationContext, CarnetDatabase::class.java, "carnet.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
