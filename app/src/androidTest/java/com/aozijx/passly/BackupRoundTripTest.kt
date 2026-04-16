package com.aozijx.passly

import android.content.Context
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.migration.Migrations
import com.aozijx.passly.data.repository.backup.BackupRepositoryImpl
import com.aozijx.passly.data.repository.backup.BackupRoomDataSource
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
 * 已经更新为测试内聚后的 BackupRepository 架构。
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepositoryImpl
    private val testDbName = "backup_test_${System.currentTimeMillis()}"
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val passphrase = DatabasePassphraseManager.getPassphrase(context)
        db = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(*Migrations.getAll()).allowMainThreadQueries().build()

        // 确保 schema 已创建
        db.openHelper.writableDatabase

        // 初始化测试专用的 Repository，注入测试数据库的 DAO
        val dataSource = BackupRoomDataSource(context, db.vaultEntryDao())
        repository = BackupRepositoryImpl(context, dataSource = dataSource)
    }

    @After
    fun tearDown() {
        runCatching { db.close() }
        context.deleteDatabase(testDbName)
        tempFiles.forEach { it.delete() }
    }

    // ─── 空库导出 ─────────────────────────────────────────────────────────

    @Test
    fun export_emptyDatabase_producesEmptyJsonArray() = runBlocking {
        val file = tempFile("empty")
        val result = repository.exportPlainBackup(file.toUri())

        assertTrue("空库导出应成功", result.isSuccess)
        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertEquals("空库应导出 0 条记录", 0, array.length())
    }

    // ─── 含数据导出 ────────────────────────────────────────────────────────

    @Test
    fun export_withTwoEntries_producesCorrectCount() = runBlocking {
        db.vaultEntryDao().insert(buildEntry("Entry A", "userA@test.com"))
        db.vaultEntryDao().insert(buildEntry("Entry B", "userB@test.com"))

        val file = tempFile("two_entries")
        val result = repository.exportPlainBackup(file.toUri())

        assertTrue("导出应成功", result.isSuccess)
        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertEquals("应导出 2 条记录", 2, array.length())
    }

    @Test
    fun export_jsonContainsExpectedFields() = runBlocking {
        db.vaultEntryDao().insert(buildEntry("Field Test", "field@test.com"))

        val file = tempFile("fields")
        repository.exportPlainBackup(file.toUri())

        val array = JSONArray(file.readText(Charsets.UTF_8))
        assertTrue(array.length() > 0)

        val entry = array.getJSONObject(0)
        // 注意：由于走的是解密后的导出，字段名与 JSON 序列化器定义的为准
        assertTrue("应含 title 字段", entry.has("title"))
        assertTrue("应含 username 字段", entry.has("username"))
        assertTrue("应含 password 字段", entry.has("password"))
        assertTrue("应含 entryType 字段", entry.has("entryType"))
        assertTrue("应含 createdAt 字段", entry.has("createdAt"))
    }

    // ─── 安全：排除 encryptedImageData ─────────────────────────────────────

    @Test
    fun export_doesNotIncludeEncryptedImageData() = runBlocking {
        db.vaultEntryDao().insert(buildEntry("Image Entry", "img@test.com"))

        val file = tempFile("no_image")
        repository.exportPlainBackup(file.toUri())

        val array = JSONArray(file.readText(Charsets.UTF_8))
        if (array.length() > 0) {
            val entry = array.getJSONObject(0)
            assertFalse(
                "encryptedImageData 应被排除在导出之外", entry.has("encryptedImageData")
            )
        }
    }

    // ─── Boolean 列序列化格式 ──────────────────────────────────────────────

    @Test
    fun export_booleanColumns_serializedAsBoolean_notInteger() = runBlocking {
        db.vaultEntryDao().insert(buildEntry("Bool Entry", "bool@test.com"))

        val file = tempFile("bool")
        repository.exportPlainBackup(file.toUri())

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
            "autoSubmit 应序列化为 Boolean，而不是整数；实际值: $autoSubmit", autoSubmit is Boolean
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