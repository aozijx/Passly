package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchemaCompatibilityTest {
    @Test
    fun legacyVersionOneDatabaseFailsOpenWithoutDeletingItsData() =
        runBlocking {
            System.loadLibrary("sqlcipher")
            val context = ApplicationProvider.getApplicationContext<Context>()
            val databaseName = "legacy_attachment_schema_test.db"
            context.deleteDatabase(databaseName)
            val provider = DatabaseProvider(context, NoOpLogger)

            try {
                provider.open(databaseName, DATABASE_KEY.copyOf()).close()
                legacyHelper(context, databaseName).use { helper ->
                    replaceAttachmentTableWithLegacySchema(helper.writableDatabase)
                }

                val file = context.getDatabasePath(databaseName)
                assertTrue(file.exists())
                assertNotNull(runCatching { provider.open(databaseName, DATABASE_KEY.copyOf()) }.exceptionOrNull())
                assertTrue("A schema mismatch must not delete the user's database", file.exists())

                legacyHelper(context, databaseName).use { helper ->
                    val columns =
                        helper.writableDatabase.query("PRAGMA table_info(attachment_refs)").use { cursor ->
                            buildSet {
                                val nameColumn = cursor.getColumnIndexOrThrow("name")
                                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                            }
                        }
                    assertTrue(columns.containsAll(setOf("entryId", "stagingOwnerId", "status")))
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }

    private fun replaceAttachmentTableWithLegacySchema(db: SupportSQLiteDatabase) {
        // Room trusts its identity hash. Remove the current hash so opening the
        // reconstructed v1 schema must validate its actual legacy columns.
        db.execSQL("DROP TABLE IF EXISTS room_master_table")
        db.execSQL("DROP TRIGGER IF EXISTS attachment_refs_validate_insert")
        db.execSQL("DROP TRIGGER IF EXISTS attachment_refs_validate_update")
        db.execSQL("DROP TABLE attachment_refs")
        db.execSQL(
            """CREATE TABLE attachment_refs (
                attachmentId TEXT NOT NULL PRIMARY KEY,
                resourceId TEXT NOT NULL,
                entryId TEXT,
                stagingOwnerId TEXT,
                fileName TEXT NOT NULL,
                mimeType TEXT,
                displayOrder INTEGER NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(entryId) REFERENCES entries(entryId) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(resourceId) REFERENCES attachment_resources(resourceId) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""",
        )
        db.execSQL("CREATE INDEX index_attachment_refs_entryId ON attachment_refs(entryId)")
        db.execSQL("CREATE INDEX index_attachment_refs_resourceId ON attachment_refs(resourceId)")
        db.execSQL("CREATE INDEX index_attachment_refs_status ON attachment_refs(status)")
        db.execSQL("CREATE INDEX index_attachment_refs_stagingOwnerId ON attachment_refs(stagingOwnerId)")
    }

    private fun legacyHelper(
        context: Context,
        databaseName: String,
    ): SupportSQLiteOpenHelper =
        SupportOpenHelperFactory(DATABASE_KEY.copyOf()).create(
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(DatabaseSchema.VERSION) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )

    private object NoOpLogger : Logger {
        override fun debug(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun info(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun degraded(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            fields: LogFields,
        ) = Unit

        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Unused")

        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Unused")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("schema_compatibility")
        override val spanId = SpanId("schema_compatibility")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Legacy attachment schema compatibility test key".toByteArray()
    }
}
