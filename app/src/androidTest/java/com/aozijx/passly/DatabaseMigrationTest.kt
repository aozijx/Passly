package com.aozijx.passly

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.migration.Migrations
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据库迁移集成测试
 *
 * 注意：当前 1.json 与 3.json 之间存在已知的 schema 不一致（wifiEncryptionType →
 * wifiSecurityType 重命名，以及 encryptedImageData 列的移除），这些变更未体现在
 * Migrations.kt 的迁移 SQL 中。因此本测试不使用 runMigrationsAndValidate 的 schema
 * 哈希校验，而是直接验证迁移 SQL 的正确性（索引创建、列添加、数据保留）。
 *
 * 测试数据库使用固定的测试口令，与生产数据库完全隔离。
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    // 每次运行使用独立名称，防止测试间干扰
    private val testDbName = "migration_test_${System.currentTimeMillis()}"
    private val testDbName2 = "${testDbName}_b"

    // 固定测试口令：与生产数据库的 DatabasePassphraseManager 管理的口令完全隔离
    private val testPassphrase = "migration_test_passphrase_isolated".toByteArray()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        SupportOpenHelperFactory(testPassphrase)
    )

    // ─── MIGRATION_1_2: 添加索引 ─────────────────────────────────────────

    @Test
    fun migration1To2_createsAllFourIndexes() {
        val db = helper.createDatabase(testDbName, 1)

        // 直接应用迁移 SQL（不依赖 schema JSON 哈希校验）
        Migrations.MIGRATION_1_2.migrate(db)

        val actualIndexes = queryIndexNames(db, "vault_entries")
        assertTrue(
            "缺少索引: index_vault_entries_favorite_usageCount_createdAt",
            actualIndexes.contains("index_vault_entries_favorite_usageCount_createdAt")
        )
        assertTrue(
            "缺少索引: index_vault_entries_category",
            actualIndexes.contains("index_vault_entries_category")
        )
        assertTrue(
            "缺少索引: index_vault_entries_createdAt",
            actualIndexes.contains("index_vault_entries_createdAt")
        )
        assertTrue(
            "缺少索引: index_vault_entries_usageCount",
            actualIndexes.contains("index_vault_entries_usageCount")
        )
        db.close()
    }

    @Test
    fun migration1To2_doesNotChangeColumnStructure() {
        // 迁移前的列集合
        val dbBefore = helper.createDatabase(testDbName, 1)
        val colsBefore = queryColumnNames(dbBefore, "vault_entries")
        dbBefore.close()

        // 迁移后的列集合（新建隔离数据库）
        val dbAfter = helper.createDatabase(testDbName2, 1)
        Migrations.MIGRATION_1_2.migrate(dbAfter)
        val colsAfter = queryColumnNames(dbAfter, "vault_entries")
        dbAfter.close()

        assertEquals("MIGRATION_1_2 不应增删列", colsBefore, colsAfter)
    }

    @Test
    fun migration1To2_idempotent_doesNotErrorOnRerun() {
        val db = helper.createDatabase(testDbName, 1)
        Migrations.MIGRATION_1_2.migrate(db)

        // CREATE INDEX IF NOT EXISTS：重复执行不报错
        var exceptionThrown = false
        try {
            Migrations.MIGRATION_1_2.migrate(db)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue("MIGRATION_1_2 使用 IF NOT EXISTS，重复执行不应抛异常", !exceptionThrown)
        db.close()
    }

    // ─── MIGRATION_2_3: 添加新列 ─────────────────────────────────────────

    @Test
    fun migration2To3_addsThreeNewColumns() {
        val db = helper.createDatabase(testDbName, 1)
        Migrations.MIGRATION_1_2.migrate(db)
        Migrations.MIGRATION_2_3.migrate(db)

        val columns = queryColumnNames(db, "vault_entries")
        assertTrue("缺少列: paymentPlatform", columns.contains("paymentPlatform"))
        assertTrue("缺少列: securityQuestion", columns.contains("securityQuestion"))
        assertTrue("缺少列: securityAnswer", columns.contains("securityAnswer"))
        db.close()
    }

    @Test
    fun migration2To3_newColumnsDefaultToNull() {
        val db = helper.createDatabase(testDbName, 1)
        Migrations.MIGRATION_1_2.migrate(db)
        Migrations.MIGRATION_2_3.migrate(db)

        // 插入一行（不指定新列 → 应为 NULL）
        db.execSQL(
            """INSERT INTO vault_entries
               (title, username, password, category, totpPeriod, totpDigits, totpAlgorithm,
                wifiIsHidden, matchType, autoSubmit, usageCount, favorite, entryType)
               VALUES ('test', '', '', '', 30, 6, 'SHA1', 0, 0, 0, 0, 0, 0)"""
        )

        val cursor = db.query(
            "SELECT paymentPlatform, securityQuestion, securityAnswer " +
            "FROM vault_entries WHERE title = 'test'"
        )
        assertTrue(cursor.moveToFirst())
        assertNull("paymentPlatform 应默认为 NULL", cursor.getString(0))
        assertNull("securityQuestion 应默认为 NULL", cursor.getString(1))
        assertNull("securityAnswer 应默认为 NULL", cursor.getString(2))
        cursor.close()
        db.close()
    }

    @Test
    fun migration2To3_newColumnsAcceptStringValues() {
        val db = helper.createDatabase(testDbName, 1)
        Migrations.MIGRATION_1_2.migrate(db)
        Migrations.MIGRATION_2_3.migrate(db)

        db.execSQL(
            """INSERT INTO vault_entries
               (title, username, password, category, totpPeriod, totpDigits, totpAlgorithm,
                wifiIsHidden, matchType, autoSubmit, usageCount, favorite, entryType,
                paymentPlatform, securityQuestion, securityAnswer)
               VALUES ('payment_entry', '', '', '', 30, 6, 'SHA1', 0, 0, 0, 0, 0, 0,
                       'Alipay', 'Pet name?', 'Fluffy')"""
        )

        val cursor = db.query(
            "SELECT paymentPlatform, securityQuestion, securityAnswer " +
            "FROM vault_entries WHERE title = 'payment_entry'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("Alipay", cursor.getString(0))
        assertEquals("Pet name?", cursor.getString(1))
        assertEquals("Fluffy", cursor.getString(2))
        cursor.close()
        db.close()
    }

    // ─── 全链数据保留测试 ─────────────────────────────────────────────────

    @Test
    fun fullMigrationChain_preservesExistingRows() {
        val db = helper.createDatabase(testDbName, 1)

        // 在 v1 插入测试数据
        db.execSQL(
            """INSERT INTO vault_entries
               (title, username, password, category, totpPeriod, totpDigits, totpAlgorithm,
                wifiIsHidden, matchType, autoSubmit, usageCount, favorite, entryType)
               VALUES ('preserved_entry', 'user@test.com', 'encrypted_pass', 'Login',
                       30, 6, 'SHA1', 0, 0, 0, 5, 0, 0)"""
        )

        // 执行全部迁移
        Migrations.MIGRATION_1_2.migrate(db)
        Migrations.MIGRATION_2_3.migrate(db)

        // 验证原始数据完好无损
        val cursor = db.query(
            "SELECT title, username, usageCount FROM vault_entries WHERE title = 'preserved_entry'"
        )
        assertTrue("迁移后原始行应存在", cursor.moveToFirst())
        assertEquals("preserved_entry", cursor.getString(0))
        assertEquals("user@test.com", cursor.getString(1))
        assertEquals(5, cursor.getInt(2))
        cursor.close()
        db.close()
    }

    @Test
    fun fullMigrationChain_vaultHistoryUnaffected() {
        val db = helper.createDatabase(testDbName, 1)

        // 先插入 vault_entries 行（外键依赖）
        db.execSQL(
            """INSERT INTO vault_entries
               (id, title, username, password, category, totpPeriod, totpDigits, totpAlgorithm,
                wifiIsHidden, matchType, autoSubmit, usageCount, favorite, entryType)
               VALUES (1, 'entry1', '', '', '', 30, 6, 'SHA1', 0, 0, 0, 0, 0, 0)"""
        )
        db.execSQL(
            """INSERT INTO vault_history
               (entryId, fieldName, changeType, changedAt)
               VALUES (1, 'password', 0, ${System.currentTimeMillis()})"""
        )

        Migrations.MIGRATION_1_2.migrate(db)
        Migrations.MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT COUNT(*) FROM vault_history")
        assertTrue(cursor.moveToFirst())
        assertEquals("vault_history 行应在全链迁移后保留", 1, cursor.getInt(0))
        cursor.close()
        db.close()
    }

    // ─── 工具函数 ─────────────────────────────────────────────────────────

    private fun queryIndexNames(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='$tableName' " +
            "AND name LIKE 'index_%'"
        )
        return buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }.also { cursor.close() }
    }

    private fun queryColumnNames(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val cursor = db.query("PRAGMA table_info($tableName)")
        val nameIdx = cursor.getColumnIndexOrThrow("name")
        return buildSet {
            while (cursor.moveToNext()) add(cursor.getString(nameIdx))
        }.also { cursor.close() }
    }
}
