package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aozijx.passly.data.local.database.callback.AttachmentReferenceConstraintCallback
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Builds instrumented databases with the same SQLite engine and attachment constraints as production. */
internal object TestDatabaseFactory {
    private val testPassphrase = "Passly instrumented database test key v1".toByteArray()

    init {
        System.loadLibrary("sqlcipher")
    }

    fun inMemoryDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .openHelperFactory(SupportOpenHelperFactory(testPassphrase.copyOf()))
            .addCallback(AttachmentReferenceConstraintCallback)
}
