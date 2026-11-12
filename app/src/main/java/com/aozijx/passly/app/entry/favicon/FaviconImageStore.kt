package com.aozijx.passly.app.entry.favicon

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.github.f4b6a3.uuid.UuidCreator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
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
class FaviconImageStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /** Snapshot before this process can promote new editor-owned files. */
        val recoveredPromotedCandidates: List<String> =
            VaultResourcePaths
                .vaultImagesDir(context)
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile &&
                        it.extension.lowercase() in setOf(WEBP_EXTENSION.removePrefix("."), GIF_EXTENSION.removePrefix(".")) &&
                        PROMOTED_FILE_PREFIXES.any(it.name::startsWith)
                }.map(File::getAbsolutePath)

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
                        if (total > FaviconImageLimits.MAX_INPUT_BYTES) {
                            throw FaviconImageException(FaviconImageFailure.TOO_LARGE)
                        }
                        output.write(buffer, 0, count)
                    }
                    if (total == 0) throw FaviconImageException(FaviconImageFailure.EMPTY_INPUT)
                }
                return if (target.hasGifSignature()) {
                    target.withExtension(GIF_EXTENSION).also { gifTarget ->
                        if (!target.renameTo(gifTarget)) {
                            throw FaviconImageException(FaviconImageFailure.SAVE_FAILED)
                        }
                    }.absolutePath
                } else {
                    target.absolutePath
                }
            } catch (error: Throwable) {
                cleanupFaviconFileAfterFailure(target, error)
                throw error
            }
        }

        fun createTransformOutput(extension: String): File {
            if (extension != WEBP_EXTENSION && extension != GIF_EXTENSION) {
                throw FaviconImageException(FaviconImageFailure.SAVE_FAILED)
            }
            return createStagingFile(extension)
        }

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

        fun finishTransform(
            input: File,
            output: File,
        ): String {
            requireStaged(input.absolutePath)
            requireStaged(output.absolutePath)
            input.delete()
            return output.absolutePath
        }

        fun promote(stagedPath: String): String {
            val source = requireStaged(stagedPath)
            val root = VaultResourcePaths.vaultImagesDir(context).apply(File::mkdirs).canonicalFile
            val extension = source.extension.let { ".$it" }
            if (extension != WEBP_EXTENSION && extension != GIF_EXTENSION) {
                throw FaviconImageException(FaviconImageFailure.SAVE_FAILED)
            }
            val target = File(root, "favicon_${UuidCreator.getTimeOrderedEpoch()}$extension").canonicalFile
            if (target.parentFile != root || !source.renameTo(target)) {
                throw FaviconImageException(FaviconImageFailure.SAVE_FAILED)
            }
            return target.absolutePath
        }

        fun discard(path: String) {
            runCatching { requireStaged(path).delete() }
        }

        fun cleanupStaging() {
            stagingRoot().listFiles()?.forEach(File::deleteRecursively)
        }

        fun isStaged(path: String): Boolean = runCatching { requireStaged(path).isFile }.getOrDefault(false)

        private fun createStagingFile(extension: String): File {
            val root = stagingRoot().apply(File::mkdirs).canonicalFile
            return File(root, "${UuidCreator.getTimeOrderedEpoch()}$extension").canonicalFile.also { target ->
                if (target.parentFile != root) {
                    throw FaviconImageException(FaviconImageFailure.INVALID_STAGING_PATH)
                }
            }
        }

        private fun File.withExtension(extension: String): File =
            File(parentFile, "$nameWithoutExtension$extension").canonicalFile.also { target ->
                if (target.parentFile != parentFile.canonicalFile) {
                    throw FaviconImageException(FaviconImageFailure.INVALID_STAGING_PATH)
                }
            }

        private fun File.hasGifSignature(): Boolean {
            val signature = ByteArray(GIF_SIGNATURE_SIZE)
            val bytesRead =
                inputStream().use { input ->
                    var offset = 0
                    while (offset < signature.size) {
                        val count = input.read(signature, offset, signature.size - offset)
                        if (count < 0) break
                        offset += count
                    }
                    offset
                }
            return bytesRead == signature.size &&
                (signature.contentEquals(GIF87A) || signature.contentEquals(GIF89A))
        }

        private fun stagingRoot(): File = VaultResourcePaths.vaultImagesStagingDir(context)

        companion object {
            private const val INPUT_EXTENSION = ".input"
            private const val WEBP_EXTENSION = ".webp"
            private const val GIF_EXTENSION = ".gif"
            private const val GIF_SIGNATURE_SIZE = 6
            private val GIF87A = "GIF87a".toByteArray(Charsets.US_ASCII)
            private val GIF89A = "GIF89a".toByteArray(Charsets.US_ASCII)
            private val PROMOTED_FILE_PREFIXES = listOf("favicon_", "restored_")
        }
    }
