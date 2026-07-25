package com.aozijx.passly.data.backup.source

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Best-effort transaction journal for files restored alongside a Room
 * transaction.
 *
 * Existing files are moved aside before replacement. A database failure can
 * therefore restore the previous files instead of deleting user data.
 */
internal class RestoreFileJournal {
    private data class Change(
        val target: File,
        val previous: File?
    )

    private val changes = mutableListOf<Change>()

    fun replace(target: File, content: ByteArray) {
        val parent = requireNotNull(target.parentFile) { "恢复目标没有父目录" }
        require(parent.isDirectory || parent.mkdirs()) {
            "无法创建恢复目录: ${parent.name}"
        }

        val token = UUID.randomUUID().toString()
        val temporary = File(parent, ".${target.name}.$token.importing")
        val previous = if (target.exists()) {
            File(parent, ".${target.name}.$token.previous").also { backup ->
                require(target.renameTo(backup)) {
                    "无法暂存已有文件: ${target.name}"
                }
            }
        } else {
            null
        }

        try {
            FileOutputStream(temporary).use { output ->
                output.write(content)
                output.flush()
                output.fd.sync()
            }
            require(temporary.renameTo(target)) {
                "无法提交恢复文件: ${target.name}"
            }
            changes += Change(target, previous)
        } catch (error: Throwable) {
            temporary.delete()
            target.delete()
            previous?.renameTo(target)
            throw error
        }
    }

    fun commit() {
        changes.forEach { it.previous?.delete() }
        changes.clear()
    }

    fun rollback() {
        changes.asReversed().forEach { change ->
            change.target.delete()
            change.previous?.renameTo(change.target)
        }
        changes.clear()
    }
}
