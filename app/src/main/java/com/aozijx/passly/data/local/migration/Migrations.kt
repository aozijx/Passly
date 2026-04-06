package com.aozijx.passly.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aozijx.passly.data.local.config.DatabaseConfig

/**
 * 数据库版本迁移定义
 */
object Migrations {

    /**
     * Migration 1 → 2: 为 vault_entries 添加性能索引
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_favorite_usageCount_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`favorite`, `usageCount`, `createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_category` ON `${DatabaseConfig.TABLE_ENTRIES}` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_createdAt` ON `${DatabaseConfig.TABLE_ENTRIES}` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${DatabaseConfig.TABLE_ENTRIES}_usageCount` ON `${DatabaseConfig.TABLE_ENTRIES}` (`usageCount`)")
        }
    }

    /**
     * Migration 2 → 3: 添加支付平台、安全问题及答案列
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `paymentPlatform` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityQuestion` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `${DatabaseConfig.TABLE_ENTRIES}` ADD COLUMN `securityAnswer` TEXT DEFAULT NULL")
        }
    }

    /**
     * 获取所有已定义的迁移路径
     */
    fun getAll(): Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
