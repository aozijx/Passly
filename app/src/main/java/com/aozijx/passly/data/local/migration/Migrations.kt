package com.aozijx.passly.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aozijx.passly.data.local.config.DatabaseConfig

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_${DatabaseConfig.TABLE_ENTRIES}_entryType " +
                    "ON ${DatabaseConfig.TABLE_ENTRIES} (entryType)"
        )
    }
}
