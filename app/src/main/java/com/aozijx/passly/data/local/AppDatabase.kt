package com.aozijx.passly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.local.migration.Migrations
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [VaultEntryEntity::class, VaultHistoryEntity::class],
    version = DatabaseConfig.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun vaultHistoryDao(): VaultHistoryDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val passphrase = DatabasePassphraseManager.getPassphrase(context)
                    val factory = SupportOpenHelperFactory(passphrase)

                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DatabaseConfig.DATABASE_NAME
                    )
                        .openHelperFactory(factory)
                        .addMigrations(*Migrations.getAll())
                        .build()

                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Logcat.e(TAG, "Critical error opening database", e)
                    throw e
                }
            }
        }

        fun preWarm(context: Context) {
            getDatabase(context)
        }
    }
}
