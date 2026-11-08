package com.aozijx.passly.data.testing

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/** Keeps instrumented persistence tests inside their per-test temporary directory. */
internal class IsolatedTestContext(
    base: Context,
    private val root: File,
) : ContextWrapper(base) {
    override fun getApplicationContext(): Context = this

    override fun getFilesDir(): File = directory("files")

    override fun getCacheDir(): File = directory("cache")

    override fun getDatabasePath(name: String): File = File(directory("databases"), name)

    override fun deleteDatabase(name: String): Boolean = getDatabasePath(name).delete()

    private fun directory(name: String): File = File(root, name).apply { mkdirs() }
}
