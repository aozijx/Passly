package com.aozijx.passly.data.local.migration

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.data.entity.VaultPayload
import com.aozijx.passly.data.local.config.DatabaseConfig

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_favorite_usageCount_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`favorite`, `usageCount`, `createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_category` ON `${DatabaseConfig.TABLE_ENTRIES}` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_usageCount` ON `${DatabaseConfig.TABLE_ENTRIES}` (`usageCount`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `paymentPlatform` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityQuestion` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityAnswer` TEXT DEFAULT NULL")
        }
    }

    /**
     * Migration 3 → 4: Encrypted Blob 重构。
     * 将 30+ 列的宽表收敛为 4 列 (id, entryType, encryptedBlob, updatedAt)。
     * 需要 SessionCryptoKey 已就绪（用户已认证解锁后执行）。
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `${DatabaseConfig.TABLE_ENTRIES}_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `entryType` INTEGER NOT NULL DEFAULT 0,
                    `encryptedBlob` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            val cursor = db.query("SELECT * FROM `${DatabaseConfig.TABLE_ENTRIES}`")
            cursor.use { c ->
                val stmt = db.compileStatement(
                    "INSERT INTO `${DatabaseConfig.TABLE_ENTRIES}_new` (`id`, `entryType`, `encryptedBlob`, `updatedAt`) VALUES (?, ?, ?, ?)"
                )
                while (c.moveToNext()) {
                    val id = c.getInt(c.getColumnIndexOrThrow("id"))
                    val entryType = c.getInt(c.getColumnIndexOrThrow("entryType"))
                    val updatedAt = c.getLongOrNull("updatedAt")
                        ?: c.getLongOrNull("createdAt")
                        ?: System.currentTimeMillis()

                    val payload = buildPayloadFromCursor(c)
                    val encryptedBlob = CryptoManager.encrypt(payload.toJson())

                    stmt.bindLong(1, id.toLong())
                    stmt.bindLong(2, entryType.toLong())
                    stmt.bindString(3, encryptedBlob)
                    stmt.bindLong(4, updatedAt)
                    stmt.executeInsert()
                    stmt.clearBindings()
                }
            }

            db.execSQL("DROP TABLE `${DatabaseConfig.TABLE_ENTRIES}`")
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}_new` RENAME TO `${DatabaseConfig.TABLE_ENTRIES}`")
        }
    }

    private fun buildPayloadFromCursor(c: Cursor): VaultPayload {
        fun getString(col: String): String = c.getString(c.getColumnIndexOrThrow(col)) ?: ""
        fun getStringOrNull(col: String): String? = c.getStringOrNull(col)
        fun getInt(col: String, def: Int = 0): Int = c.getIntOrDefault(col, def)
        fun getBool(col: String, def: Boolean = false): Boolean = c.getIntOrDefault(col, if (def) 1 else 0) != 0
        fun getFloatOrNull(col: String): Float? = c.getFloatOrNull(col)
        fun getLong(col: String): Long? = c.getLongOrNull(col)

        fun decryptMandatory(value: String): String {
            if (value.isEmpty()) return ""
            return CryptoAccess.decryptOrNull(value) ?: value
        }

        fun decryptOptional(value: String?): String? {
            if (value.isNullOrEmpty()) return value
            return CryptoAccess.decryptOrNull(value) ?: value
        }

        fun csvToList(value: String?): List<String>? {
            if (value.isNullOrEmpty()) return null
            return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        return VaultPayload(
            title = getString("title"),
            username = decryptMandatory(getString("username")),
            password = decryptMandatory(getString("password")),
            category = getString("category"),
            notes = decryptOptional(getStringOrNull("notes")),

            iconName = getStringOrNull("iconName"),
            iconCustomPath = getStringOrNull("iconCustomPath"),

            totpSecret = decryptOptional(getStringOrNull("totpSecret")),
            totpPeriod = getInt("totpPeriod", 30),
            totpDigits = getInt("totpDigits", 6),
            totpAlgorithm = getStringOrNull("totpAlgorithm") ?: "SHA1",

            passkeyDataJson = decryptOptional(getStringOrNull("passkeyDataJson")),
            recoveryCodes = decryptOptional(getStringOrNull("recoveryCodes")),
            hardwareKeyInfo = getStringOrNull("hardwareKeyInfo"),

            wifiSecurityType = getStringOrNull("wifiSecurityType") ?: "WPA",
            wifiIsHidden = getBool("wifiIsHidden"),

            cardCvv = decryptOptional(getStringOrNull("cardCvv")),
            cardExpiration = decryptOptional(getStringOrNull("cardExpiration")),
            idNumber = decryptOptional(getStringOrNull("idNumber")),

            paymentPin = decryptOptional(getStringOrNull("paymentPin")),
            paymentPlatform = getStringOrNull("paymentPlatform"),

            securityQuestion = getStringOrNull("securityQuestion"),
            securityAnswer = decryptOptional(getStringOrNull("securityAnswer")),

            sshPrivateKey = decryptOptional(getStringOrNull("sshPrivateKey")),
            cryptoSeedPhrase = decryptOptional(getStringOrNull("cryptoSeedPhrase")),

            entryType = getInt("entryType"),

            associatedAppPackage = getStringOrNull("associatedAppPackage"),
            associatedDomain = getStringOrNull("associatedDomain"),
            uriList = csvToList(getStringOrNull("uriList")),
            matchType = getInt("matchType"),
            customFieldsJson = decryptOptional(getStringOrNull("customFieldsJson")),
            autoSubmit = getBool("autoSubmit"),

            strengthScore = getFloatOrNull("strengthScore"),
            lastUsedAt = getLong("lastUsedAt"),
            usageCount = getInt("usageCount"),

            favorite = getBool("favorite"),
            tags = csvToList(getStringOrNull("tags")),
            createdAt = getLong("createdAt"),
            expiresAt = getLong("expiresAt")
        )
    }

    private fun Cursor.getStringOrNull(col: String): String? {
        val idx = getColumnIndex(col)
        return if (idx < 0 || isNull(idx)) null else getString(idx)
    }

    private fun Cursor.getIntOrDefault(col: String, def: Int): Int {
        val idx = getColumnIndex(col)
        return if (idx < 0 || isNull(idx)) def else getInt(idx)
    }

    private fun Cursor.getFloatOrNull(col: String): Float? {
        val idx = getColumnIndex(col)
        return if (idx < 0 || isNull(idx)) null else getFloat(idx)
    }

    private fun Cursor.getLongOrNull(col: String): Long? {
        val idx = getColumnIndex(col)
        return if (idx < 0 || isNull(idx)) null else getLong(idx)
    }

    fun getAll(): Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
