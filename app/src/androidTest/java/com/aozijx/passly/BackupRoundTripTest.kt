package com.aozijx.passly

import android.content.Context
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.migration.Migrations
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 备份导出格式与兼容性测试
 *
 * 使用时间戳隔离的测试数据库，与生产数据库完全独立。
 * 测试完成后自动清理临时数据库和导出文件。
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val testDbName = "backup_test_${System.currentTimeMillis()}"
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val passphrase = DatabasePassphraseManager.getPassphrase(context)
        db = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(*Migrations.getAll())
            .allowMainThreadQueries()
            .build()

        // 确保 schema 已创建（触发 DB 文件写入）
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        runCatching { db.close() }
        context.deleteDatabase(testDbName)
        tempFiles.forEach { it.delete() }
    }

    // ─── 空库导出 ─────────────────────────────────────────────────────────

    @Test
    fun export_emptyDatabase_producesEmptyJsonArray() {
        val file = tempFile("empty")
        val result = EmergencyBackupExporter.exportPlainBackupToUri(
            context, file.toUri(), testDbName
        )

        assertTrue("空库导出应成功", result.isSuccess)
        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertEquals("空库应导出 0 条记录", 0, array.length())
    }

    // ─── 含数据导出 ────────────────────────────────────────────────────────

    @Test
    fun export_withTwoEntries_producesCorrectCount() {
        runBlocking {
            db.vaultEntryDao().insert(buildEntry("Entry A", "userA@test.com"))
            db.vaultEntryDao().insert(buildEntry("Entry B", "userB@test.com"))
        }
        db.close()

        val file = tempFile("two_entries")
        val result = EmergencyBackupExporter.exportPlainBackupToUri(
            context, file.toUri(), testDbName
        )

        assertTrue("导出应成功", result.isSuccess)
        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertEquals("应导出 2 条记录", 2, array.length())
    }

    @Test
    fun export_jsonContainsExpectedFields() {
        runBlocking { db.vaultEntryDao().insert(buildEntry("Field Test", "field@test.com")) }
        db.close()

        val file = tempFile("fields")
        EmergencyBackupExporter.exportPlainBackupToUri(context, file.toUri(), testDbName)

        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertTrue(array.length() > 0)

        val entry = array.getJSONObject(0)
        assertTrue("应含 id 字段", entry.has("id"))
        assertTrue("应含 title 字段", entry.has("title"))
        assertTrue("应含 username 字段", entry.has("username"))
        assertTrue("应含 password 字段", entry.has("password"))
        assertTrue("应含 entryType 字段", entry.has("entryType"))
        assertTrue("应含 createdAt 字段", entry.has("createdAt"))
    }

    // ─── 安全：排除 encryptedImageData ─────────────────────────────────────

    @Test
    fun export_doesNotIncludeEncryptedImageData() {
        runBlocking { db.vaultEntryDao().insert(buildEntry("Image Entry", "img@test.com")) }
        db.close()

        val file = tempFile("no_image")
        EmergencyBackupExporter.exportPlainBackupToUri(context, file.toUri(), testDbName)

        val array = JSONArray(file.readText(Charsets.UTF_8))
        if (array.length() > 0) {
            val entry = array.getJSONObject(0)
            assertFalse(
                "encryptedImageData 应被排除在导出之外",
                entry.has("encryptedImageData")
            )
        }
    }

    // ─── Boolean 列序列化格式 ──────────────────────────────────────────────

    @Test
    fun export_booleanColumns_serializedAsBoolean_notInteger() {
        runBlocking { db.vaultEntryDao().insert(buildEntry("Bool Entry", "bool@test.com")) }
        db.close()

        val file = tempFile("bool")
        EmergencyBackupExporter.exportPlainBackupToUri(context, file.toUri(), testDbName)

        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertTrue(array.length() > 0)

        val entry = array.getJSONObject(0)
        val wifiIsHidden = entry.opt("wifiIsHidden")
        assertNotNull("wifiIsHidden 应存在于导出中", wifiIsHidden)
        assertTrue(
            "wifiIsHidden 应序列化为 Boolean（false），而不是整数 0；实际值: $wifiIsHidden",
            wifiIsHidden is Boolean
        )

        val autoSubmit = entry.opt("autoSubmit")
        assertNotNull("autoSubmit 应存在于导出中", autoSubmit)
        assertTrue(
            "autoSubmit 应序列化为 Boolean，而不是整数；实际值: $autoSubmit",
            autoSubmit is Boolean
        )
    }

    // ─── 错误场景 ──────────────────────────────────────────────────────────

    @Test
    fun export_databaseNotExist_returnsFailure() {
        db.close()
        context.deleteDatabase(testDbName)

        val file = tempFile("nonexistent")
        val result = EmergencyBackupExporter.exportPlainBackupToUri(
            context, file.toUri(), testDbName
        )

        assertTrue("数据库不存在时应返回 Failure", result.isFailure)
        assertTrue(
            "错误信息应提及数据库文件",
            result.exceptionOrNull()?.message?.contains("数据库文件不存在") == true
        )
    }

    // ─── 工具函数 ─────────────────────────────────────────────────────────

    private fun buildEntry(title: String, username: String) = VaultEntryEntity(
        title = title,
        username = username,
        password = "encrypted_password_placeholder",
        category = "Test",
        totpPeriod = 30,
        totpDigits = 6,
        totpAlgorithm = "SHA1",
        wifiIsHidden = false,
        matchType = 0,
        autoSubmit = false,
        usageCount = 0,
        favorite = false,
        entryType = 0
    )

    private fun tempFile(tag: String): File {
        val f = File(context.cacheDir, "backup_test_${tag}_${System.currentTimeMillis()}.json")
        tempFiles.add(f)
        return f
    }
}
