package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreshDatabaseTest {

    @Test
    fun freshSchemaOpensAndRunsTransaction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            database.runInTransaction {
                assertTrue(database.openHelper.writableDatabase.isOpen)
            }
        } finally {
            database.close()
        }
    }
}
