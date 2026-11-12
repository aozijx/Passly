package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.migration.ActivityHistoryMigration
import com.aozijx.passly.data.testing.IsolatedTestContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder

@RunWith(AndroidJUnit4::class)
class ActivityHistoryMigrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun migratesLegacyFieldCopyActivitiesAndAddsFieldKeys() {
        System.loadLibrary("sqlcipher")
        val context =
            IsolatedTestContext(
                ApplicationProvider.getApplicationContext(),
                temporaryFolder.root,
            )
        val databaseName = "activity_history_migration_test.db"

        legacyHelper(context, databaseName).use { helper ->
            val db = helper.writableDatabase
            db.execSQL(
                """CREATE TABLE entry_activities (
                    activityId TEXT NOT NULL PRIMARY KEY,
                    entryId TEXT NOT NULL,
                    activityType TEXT NOT NULL,
                    revisionVersion INTEGER,
                    createdAt INTEGER NOT NULL
                )""",
            )
            db.execSQL(
                "INSERT INTO entry_activities(activityId, entryId, activityType, createdAt) " +
                    "VALUES ('username-copy', 'entry', 'COPY_USERNAME', 1)",
            )
            db.execSQL(
                "INSERT INTO entry_activities(activityId, entryId, activityType, createdAt) " +
                    "VALUES ('password-copy', 'entry', 'COPY_PASSWORD', 2)",
            )
            db.execSQL(
                "INSERT INTO entry_activities(activityId, entryId, activityType, createdAt) " +
                    "VALUES ('view', 'entry', 'VIEW', 3)",
            )

            ActivityHistoryMigration.MIGRATION_1_2.migrate(db)

            val migrated =
                db.query(
                    "SELECT activityType, affectedFieldKeys FROM entry_activities ORDER BY createdAt",
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(cursor.getString(0) to cursor.getString(1))
                        }
                    }
                }
            assertEquals(
                listOf(
                    "COPY_FIELD" to "[\"USERNAME\"]",
                    "COPY_FIELD" to "[\"PASSWORD\"]",
                    "VIEW" to "[]",
                ),
                migrated,
            )
        }

        context.deleteDatabase(databaseName)
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
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        )

    private companion object {
        val DATABASE_KEY = "Activity history migration test key".toByteArray()
    }
}
