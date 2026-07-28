package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import com.aozijx.passly.domain.entry.repository.FaviconRepository
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
    private val faviconRepository: FaviconRepository,
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

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            when (val insertResult = entryCommandRepository.createEntry(entry)) {
                is AppResult.Success -> {
                    val entryId = insertResult.data.value
                    if (!domain.isNullOrBlank()) {
                        val outcome = downloadFavicon(domain)
                        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                            val savedEntry = entryQueryRepository.getById(entryId)
                            if (savedEntry != null) {
                                val iconSummary = savedEntry.summary.copy(icon = outcome.filePath)
                                entryCommandRepository.updateEntry(
                                    savedEntry.id,
                                    savedEntry.entryVersion,
                                    EntryChanges(summary = iconSummary)
                                )
                            }
                        }
                    }
                    onComplete()
                }

                is AppResult.Failure -> {
                    onError(insertResult.error.message)
                }
            }
        }
    }

    /**
     * 将来自详情页面的完整条目更新原子提交。
     * 比较当前数据库条目与传入条目导出变更集 [EntryChanges]，
     * 一次事务写入所有变化字段（Metadata + Credential + 版本 + 盲索引 + 快照）。
     * 覆盖 title、username、password、email、notes、otp、card、ssh、customFields 等全部字段。
     */
    fun updateEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            val current = entryQueryRepository.getById(entry.id) ?: return@launch

            val metaChanged = current.summary != entry.summary
            val credChanged = current.secret != entry.secret

            if (!metaChanged && !credChanged) return@launch

            val changes = EntryChanges(
                summary = if (metaChanged) entry.summary else null,
                secret = if (credChanged) entry.secret else null
            )

            entryCommandRepository.updateEntry(entry.id, current.entryVersion, changes)
                .onSuccess {
                    totp.onEntryUpdated(entry.id)
                }.onFailure { error ->
                    onError(error.message)
                }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entry.id, presetEntry = entry)
        }
    }

    fun deleteEntryById(entryId: String) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entryId)
        }
    }

    private suspend fun deleteEntryInternal(entryId: String, presetEntry: VaultEntry? = null) {
        val acquired = deletingIdsMutex.withLock {
            if (deletingIds.contains(entryId)) false else deletingIds.add(entryId)
        }
        if (!acquired) return

        try {
            val entry = presetEntry ?: entryQueryRepository.getById(entryId)
            if (entry == null) return
            when (
                val result = entryCommandRepository.moveToTrash(
                    entry.id,
                    entry.entryVersion
                )
            ) {
                is AppResult.Success -> {
                    totp.clearSensitiveState(entryId)
                    onEntryDeleted(entryId)
                }

                is AppResult.Failure -> {
                    onError(result.error.message)
                }
            }
        } catch (e: AppError) {
            onError(e.message)
        } finally {
            deletingIdsMutex.withLock { deletingIds.remove(entryId) }
        }
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }
}
