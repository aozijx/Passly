package com.aozijx.passly.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object DatabaseConfig {
    const val DATABASE_NAME = "vault_database"
    const val TABLE_ENTRIES = "vault_entries"
    const val TABLE_HISTORY = "vault_history"
    const val VERSION = 3
}

@Database(
    entities = [VaultEntryEntity::class, VaultHistoryEntity::class],
    version = DatabaseConfig.VERSION,
    exportSchema = BuildConfig.EXPORT_ROOM_SCHEMA
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getDatabasePassphrase(context: Context): ByteArray {
            val alias = "${context.packageName}.vault_db_passphrase_key"
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

            if (!ks.containsAlias(alias)) {
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }

            val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
            val sharedPrefs = context.getSharedPreferences("secure_db_prefs", Context.MODE_PRIVATE)
            val encryptedPassphrase = sharedPrefs.getString("db_phrase", null)

            if (encryptedPassphrase != null) {
                try {
                    val combined = Base64.decode(encryptedPassphrase, Base64.NO_WRAP)
                    val buffer = ByteBuffer.wrap(combined)
                    val iv = ByteArray(12).also { buffer.get(it) }
                    val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                    return cipher.doFinal(encrypted)
                } catch (e: Exception) {
                    Logcat.e(TAG, "Database passphrase decryption failed!", e)
                    throw e
                }
            }

            val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(newPassphrase)
            val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
                .put(cipher.iv).put(encrypted).array()

            sharedPrefs.edit {
                putString(
                    "db_phrase",
                    Base64.encodeToString(combined, Base64.NO_WRAP)
                )
            }
            return newPassphrase
        }

        /**
         * Migration 1 → 2: add performance indices to vault_entries.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_favorite_usageCount_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`favorite`, `usageCount`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_category` ON `${DatabaseConfig.TABLE_ENTRIES}` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_usageCount` ON `${DatabaseConfig.TABLE_ENTRIES}` (`usageCount`)")
            }
        }

        /**
         * Migration 2 → 3: add paymentPlatform, securityQuestion, securityAnswer columns.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `paymentPlatform` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityQuestion` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityAnswer` TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val passphrase = getDatabasePassphrase(context)
                    val factory = SupportOpenHelperFactory(passphrase)
                    val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DatabaseConfig.DATABASE_NAME)
                        .openHelperFactory(factory)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
