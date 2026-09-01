package com.aozijx.passly.app.entry.favicon

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class FaviconImageFailure {
    EMPTY_INPUT,
    TOO_LARGE,
    INVALID_STAGING_PATH,
    MISSING_STAGED_IMAGE,
    UNSUPPORTED_DIMENSIONS,
    DECODE_FAILED,
    ENCODE_FAILED,
    SAVE_FAILED,
}

class FaviconImageException(
    val reason: FaviconImageFailure,
) : IllegalStateException(reason.name)

@Singleton
class FaviconImageStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    init {
        cleanupStaging()
    }

    fun stage(input: InputStream): String {
        val target = createStagingFile(INPUT_EXTENSION)
        try {
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_INPUT_BYTES) {
                        throw FaviconImageException(FaviconImageFailure.TOO_LARGE)
                    }
                    output.write(buffer, 0, count)
                }
                if (total == 0) throw FaviconImageException(FaviconImageFailure.EMPTY_INPUT)
            }
            return target.absolutePath
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    fun createTransformOutput(): File = createStagingFile(WEBP_EXTENSION)

    fun requireStaged(path: String): File {
        val root = stagingRoot().apply(File::mkdirs).canonicalFile
        val target = File(path).canonicalFile
        if (target.parentFile != root) {
            throw FaviconImageException(FaviconImageFailure.INVALID_STAGING_PATH)
        }
        if (!target.isFile) {
            throw FaviconImageException(FaviconImageFailure.MISSING_STAGED_IMAGE)
        }
        return target
    }

    fun finishTransform(input: File, output: File): String {
        requireStaged(input.absolutePath)
        requireStaged(output.absolutePath)
        input.delete()
        return output.absolutePath
    }

    fun promote(stagedPath: String): String {
        val source = requireStaged(stagedPath)
        val root = VaultResourcePaths.vaultImagesDir(context).apply(File::mkdirs).canonicalFile
        val target = File(root, "favicon_${UUID.randomUUID()}$WEBP_EXTENSION").canonicalFile
        if (target.parentFile != root || !source.renameTo(target)) {
            throw FaviconImageException(FaviconImageFailure.SAVE_FAILED)
        }
        return target.absolutePath
    }

    fun discard(path: String?) {
        path ?: return
        runCatching { requireStaged(path).delete() }
    }

    fun discardPromotedCandidate(path: String?) {
        path ?: return
        runCatching {
            val root = VaultResourcePaths.vaultImagesDir(context).canonicalFile
            val target = File(path).canonicalFile
            if (
                target.parentFile == root &&
                target.name.startsWith("favicon_") &&
                target.extension.equals("webp", ignoreCase = true)
            ) {
                target.delete()
            }
        }
    }

    fun cleanupStaging() {
        stagingRoot().listFiles()?.forEach(File::deleteRecursively)
    }

    fun isStaged(path: String): Boolean = runCatching { requireStaged(path).isFile }.getOrDefault(false)

    private fun createStagingFile(extension: String): File {
        val root = stagingRoot().apply(File::mkdirs).canonicalFile
        return File(root, "${UUID.randomUUID()}$extension").canonicalFile.also { target ->
            if (target.parentFile != root) {
                throw FaviconImageException(FaviconImageFailure.INVALID_STAGING_PATH)
            }
        }
    }

    private fun stagingRoot(): File = VaultResourcePaths.vaultImagesStagingDir(context)

    companion object {
        const val MAX_INPUT_BYTES = 10 * 1024 * 1024
        private const val INPUT_EXTENSION = ".input"
        private const val WEBP_EXTENSION = ".webp"
    }
}
