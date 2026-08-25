package com.aozijx.passly.core.platform.cache

import java.io.File

object DirectoryTreeSizeCalculator {
    fun bytes(directory: File): Long = if (directory.exists()) {
        directory.walkTopDown().filter(File::isFile).sumOf(File::length)
    } else {
        0L
    }
}
