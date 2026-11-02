package com.aozijx.passly.app.database.backup

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException

/** Coordinates best-effort file changes with a Room restore transaction. */
internal class RestoreFileJournal(
    private val operations: RestoreFileOperations = SystemRestoreFileOperations,
) {
    private data class Change(
        val target: File,
        val temporary: File,
        val previous: File?,
    )

    private val changes = mutableListOf<Change>()

    fun replace(
        target: File,
        content: ByteArray,
    ) {
        val parent = requireNotNull(target.parentFile) { "恢复目标没有父目录" }
        require(operations.isDirectory(parent) || operations.mkdirs(parent)) {
            "无法创建恢复目录: ${parent.name}"
        }

        val token = UUID.randomUUID().toString()
        val temporary = File(parent, ".${target.name}.$token.importing")
        val previous =
            if (operations.exists(target)) {
                File(parent, ".${target.name}.$token.previous").also { backup ->
                    require(!operations.exists(backup)) { "恢复备份路径已存在: ${backup.name}" }
                }
            } else {
                null
            }
        if (previous != null) {
            if (!operations.rename(target, previous)) {
                throw IOException("无法暂存已有文件: ${target.name}")
            }
        }

        val change = Change(target, temporary, previous)
        changes += change
        try {
            operations.writeAndSync(temporary, content)
            if (!operations.rename(temporary, target)) {
                throw IOException("无法提交恢复文件: ${target.name}")
            }
        } catch (error: Throwable) {
            try {
                undo(change)
                changes.remove(change)
            } catch (compensationFailure: Throwable) {
                throw preferRestoreFailure(error, compensationFailure)
            }
            throw error
        }
    }

    /** Finalizes journal files after the Room transaction has committed. */
    fun commit() {
        completeChanges(::discardPrevious)
    }

    /** Restores the files to their pre-transaction state, retaining failed work for retry. */
    fun rollback() {
        completeChanges(::undo, reverse = true)
    }

    private fun completeChanges(
        action: (Change) -> Unit,
        reverse: Boolean = false,
    ) {
        var failure: Throwable? = null
        val pending = if (reverse) changes.asReversed().toList() else changes.toList()
        pending.forEach { change ->
            try {
                action(change)
                changes.remove(change)
            } catch (error: Throwable) {
                failure = failure?.let { preferRestoreFailure(it, error) } ?: error
            }
        }
        failure?.let { throw it }
    }

    private fun discardPrevious(change: Change) {
        if (operations.exists(change.temporary) && !operations.delete(change.temporary)) {
            throw IOException("无法清理恢复临时文件: ${change.temporary.name}")
        }
        change.previous?.let { previous ->
            if (operations.exists(previous) && !operations.delete(previous)) {
                throw IOException("无法清理恢复备份文件: ${previous.name}")
            }
        }
    }

    private fun undo(change: Change) {
        if (operations.exists(change.temporary) && !operations.delete(change.temporary)) {
            throw IOException("无法清理恢复临时文件: ${change.temporary.name}")
        }
        if (change.previous == null) {
            if (operations.exists(change.target) && !operations.delete(change.target)) {
                throw IOException("无法删除未提交的恢复文件: ${change.target.name}")
            }
            return
        }

        if (operations.exists(change.target) && !operations.delete(change.target)) {
            throw IOException("无法移除待回滚文件: ${change.target.name}")
        }
        if (!operations.exists(change.previous)) {
            throw IOException("恢复备份文件缺失: ${change.previous.name}")
        }
        if (!operations.rename(change.previous, change.target)) {
            throw IOException("无法恢复原有文件: ${change.target.name}")
        }
    }
}

internal fun preferRestoreFailure(
    first: Throwable,
    second: Throwable,
): Throwable {
    if (first === second) return first
    if (second.restoreFailurePriority() > first.restoreFailurePriority()) {
        second.addSuppressed(first)
        return second
    }
    first.addSuppressed(second)
    return first
}

private fun Throwable.restoreFailurePriority(): Int =
    when (this) {
        is Error -> 2
        is CancellationException -> 1
        else -> 0
    }

internal interface RestoreFileOperations {
    fun exists(file: File): Boolean

    fun isDirectory(file: File): Boolean

    fun mkdirs(file: File): Boolean

    fun writeAndSync(
        file: File,
        content: ByteArray,
    )

    fun rename(
        source: File,
        target: File,
    ): Boolean

    fun delete(file: File): Boolean
}

internal object SystemRestoreFileOperations : RestoreFileOperations {
    override fun exists(file: File): Boolean = file.exists()

    override fun isDirectory(file: File): Boolean = file.isDirectory

    override fun mkdirs(file: File): Boolean = file.mkdirs()

    override fun writeAndSync(
        file: File,
        content: ByteArray,
    ) {
        FileOutputStream(file).use { output ->
            output.write(content)
            output.flush()
            output.fd.sync()
        }
    }

    override fun rename(
        source: File,
        target: File,
    ): Boolean = source.renameTo(target)

    override fun delete(file: File): Boolean = file.delete()
}
