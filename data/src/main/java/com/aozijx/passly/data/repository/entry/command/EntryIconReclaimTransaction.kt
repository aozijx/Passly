package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.local.database.AppDatabase
import java.io.File

internal enum class IconResourceReclaimOutcome { COMPLETE, REFERENCED }

/** Must run in a write transaction, serializing the reference check and deletion with entry writes. */
internal object EntryIconReclaimTransaction {
    suspend fun reclaim(db: AppDatabase, imageRoot: File, path: String): IconResourceReclaimOutcome {
        val root = imageRoot.canonicalFile
        val target = File(path).canonicalFile
        if (target.parentFile != root) return IconResourceReclaimOutcome.COMPLETE
        if (db.entryQueryDao().countByIconCustomReference(path) > 0 ||
            db.entryRevisionQueryDao().countByIconCustomReference(path) > 0) {
            return IconResourceReclaimOutcome.REFERENCED
        }
        check(!target.exists() || target.delete()) { "Unable to delete custom icon" }
        return IconResourceReclaimOutcome.COMPLETE
    }
}
