package com.aozijx.passly.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ActivityHistoryMigration {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE entry_activities " +
                        "ADD COLUMN affectedFieldKeys TEXT NOT NULL DEFAULT '[]'",
                )
                db.execSQL(
                    "UPDATE entry_activities " +
                        "SET activityType = 'COPY_FIELD', affectedFieldKeys = '[\"USERNAME\"]' " +
                        "WHERE activityType = 'COPY_USERNAME'",
                )
                db.execSQL(
                    "UPDATE entry_activities " +
                        "SET activityType = 'COPY_FIELD', affectedFieldKeys = '[\"PASSWORD\"]' " +
                        "WHERE activityType = 'COPY_PASSWORD'",
                )
            }
        }
}
