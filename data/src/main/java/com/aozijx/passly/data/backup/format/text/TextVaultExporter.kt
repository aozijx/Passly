package com.aozijx.passly.data.backup.format.text

import com.aozijx.passly.data.backup.model.BackupEntryRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文本导出器。
 *
 * 将条目列表渲染为纯文本格式。
 * 只用于导出（export），不实现导入（import）。
 */
@Singleton
class TextVaultExporter @Inject constructor(
    private val renderer: TextVaultRenderer
) {
    fun export(
        records: List<BackupEntryRecord>,
        options: TextExportOptions = TextExportOptions()
    ): String = renderer.render(records, options = options)
}
