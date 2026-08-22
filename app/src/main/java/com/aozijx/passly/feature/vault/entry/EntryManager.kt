package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.service.FaviconService
import com.aozijx.passly.feature.vault.otp.TotpCoordinator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EntryManager(
    private val scope: CoroutineScope,
    private val entryCommandRepository: EntryCommandRepository,
    private val entryQueryRepository: EntryQueryRepository,
    private val faviconService: FaviconService,
    private val totp: TotpCoordinator,
    private val onError: (String) -> Unit = {},
    private val onEntryDeleted: (String) -> Unit = {}
) {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        AppTelemetry.e("EntryManager", "Operation failed", throwable)
        onError("操作失败: ${throwable.message ?: "未知错误"}")
    }
    private val deletingIds = mutableSetOf<String>()
    private val deletingIdsMutex = Mutex()

    fun addItem(entry: Entry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            when (val insertResult = entryCommandRepository.createEntry(entry)) {
                is AppResult.Success -> {
                    val entryId = insertResult.data
                    if (!domain.isNullOrBlank()) {
                        val update = faviconService.downloadAndPrepareUpdate(entry, domain)
                        if (update != null) {
                            val savedEntry = entryQueryRepository.getById(entryId)
                            if (savedEntry != null) {
                                entryCommandRepository.updateEntry(
                                    savedEntry.id,
                                    savedEntry.version,
                                    update
                                )
                            }
                        }
                    }
                    onComplete()
                }

                is AppResult.Failure -> {
                    onError(insertResult.error.code)
                }
            }
        }
    }

    /**
     * 将来自详情页面的完整条目更新原子提交。
     * 比较当前数据库条目与传入条目导出变更集 [EntryUpdate]，
     * 一次事务写入所有变化字段（Metadata + Credential + 版本 + 盲索引 + 快照）。
     * 覆盖 title、username、password、email、notes、otp、card、ssh、customFields 等全部字段。
     */
    fun updateEntry(entry: Entry) {
        scope.launch(Dispatchers.IO + handler) {
            val current = entryQueryRepository.getById(entry.id) ?: return@launch

            val metaChanged = current.profile != entry.profile
            val credChanged = current.secret != entry.secret

            if (!metaChanged && !credChanged) return@launch

            val changes = EntryUpdate(
                profile = if (metaChanged) entry.profile else null,
                secret = if (credChanged) entry.secret else null
            )

            entryCommandRepository.updateEntry(entry.id, current.version, changes)
                .onSuccess {
                    totp.onEntryUpdated(entry.id.value)
                }.onFailure { error ->
                    onError(error.code)
                }
        }
    }

    fun deleteEntry(entry: Entry) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entry.id.value, presetEntry = entry)
        }
    }

    fun deleteEntryById(entryId: String) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entryId)
        }
    }

    private suspend fun deleteEntryInternal(entryId: String, presetEntry: Entry? = null) {
        val acquired = deletingIdsMutex.withLock {
            if (deletingIds.contains(entryId)) false else deletingIds.add(entryId)
        }
        if (!acquired) return

        try {
            val entry = presetEntry ?: entryQueryRepository.getById(
                com.aozijx.passly.domain.entry.model.EntryId(entryId)
            )
            if (entry == null) return
            when (
                val result = entryCommandRepository.moveToTrash(
                    entry.id,
                    entry.version
                )
            ) {
                is AppResult.Success -> {
                    totp.clearSensitiveState(entryId)
                    onEntryDeleted(entryId)
                }

                is AppResult.Failure -> {
                    onError(result.error.code)
                }
            }
        } catch (e: AppError) {
            onError(e.code)
        } finally {
            deletingIdsMutex.withLock { deletingIds.remove(entryId) }
        }
    }
}
