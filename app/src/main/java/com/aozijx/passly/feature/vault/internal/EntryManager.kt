package com.aozijx.passly.feature.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.repository.entry.EntryCommands
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.usecase.vault.VaultQueryUseCases
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EntryManager(
    private val scope: CoroutineScope,
    private val entryCommandHandler: EntryCommands,
    private val vaultQueryUseCases: VaultQueryUseCases,
    private val faviconRepository: FaviconRepository,
    private val iconHelper: EntryIconHelper,
    private val detail: DetailCoordinator,
    private val totp: TotpCoordinator,
    private val onError: (String) -> Unit = {},
    private val onRefreshItems: () -> Unit = {}
) {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        AppLog.e("EntryManager", "Operation failed", throwable)
        onError("操作失败: ${throwable.message ?: "未知错误"}")
    }
    private val deletingIds = mutableSetOf<String>()
    private val deletingIdsMutex = Mutex()

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            val insertResult = entryCommandHandler.createEntry(entry)
            when (insertResult) {
                is AppResult.Success -> {
                    if (!domain.isNullOrBlank()) {
                        val outcome = downloadFavicon(domain)
                        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                            val savedEntry = vaultQueryUseCases.getById(entry.id)
                            if (savedEntry != null) {
                                val iconSummary = savedEntry.summary.copy(icon = outcome.filePath)
                                entryCommandHandler.updateEntry(
                                    savedEntry.id,
                                    savedEntry.entryVersion,
                                    EntryChanges(summary = iconSummary)
                                )
                            }
                        }
                    }
                    detail.setAddType(null)
                    onRefreshItems()
                    onComplete()
                }

                is AppResult.Failure -> {
                    onError(insertResult.error.message)
                    detail.setAddType(null)
                    onComplete()
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
            val current = vaultQueryUseCases.getById(entry.id) ?: return@launch

            val metaChanged = current.summary != entry.summary
            val credChanged = current.secret != entry.secret

            if (!metaChanged && !credChanged) return@launch

            val changes = EntryChanges(
                summary = if (metaChanged) entry.summary else null,
                secret = if (credChanged) entry.secret else null
            )

            entryCommandHandler.updateEntry(entry.id, current.entryVersion, changes)
                .onSuccess {
                    detail.updateEntry(entry)
                    totp.onEntryUpdated(entry.id)
                    onRefreshItems()
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
            val entry = presetEntry ?: vaultQueryUseCases.getById(entryId)
            if (entry == null) return
            if (detail.isViewingEntry(entryId)) {
                detail.dismissDetail()
            }
            iconHelper.cleanupIcon(entry.iconCustomPath)
            entryCommandHandler.moveToTrash(entry.id, entry.entryVersion)
            detail.setItemToDelete(null)
            totp.clearSensitiveState(entryId)
        } catch (e: AppError) {
            onError(e.message)
        } finally {
            deletingIdsMutex.withLock { deletingIds.remove(entryId) }
        }
    }

    fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri, onFailed: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            try {
                val internalPath = iconHelper.saveCustomIcon(context, item, uri)
                if (internalPath != null) {
                    val iconMeta = item.summary.copy(icon = internalPath)
                    entryCommandHandler.updateEntry(
                        item.id, item.entryVersion, EntryChanges(summary = iconMeta)
                    ).onSuccess {
                        detail.updateEntry(item.copy(summary = item.summary.copy(icon = internalPath)))
                        totp.onEntryUpdated(item.id)
                    }.onFailure { error ->
                        onError(error.message)
                        onFailed()
                    }
                } else {
                    onFailed()
                }
            } catch (e: AppError) {
                onError(e.message)
                onFailed()
            }
        }
    }

    fun downloadMissingIcons(summaries: List<VaultEntry>) {
        scope.launch(Dispatchers.IO + handler) {
            summaries
                .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
                .forEach { summary ->
                    val domain = summary.associatedDomain ?: return@forEach
                    val outcome = downloadFavicon(domain)
                    if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                        vaultQueryUseCases.getById(summary.id)?.let { entry ->
                            val iconSummary = entry.summary.copy(icon = outcome.filePath)
                            entryCommandHandler.updateEntry(
                                entry.id,
                                entry.entryVersion,
                                EntryChanges(summary = iconSummary)
                            )
                        }
                    }
                }
        }
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }
}
