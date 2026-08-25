package com.aozijx.passly.core.platform.cache

import java.io.File

object DirectoryContentsCleaner {
    fun clear(directory: File) {
        directory.listFiles()?.forEach { child ->
            if (child.isDirectory) clear(child)
            child.delete()
        }
    }
}
