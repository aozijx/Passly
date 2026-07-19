package com.aozijx.passly.domain.model.backup

// 导入模式：负责“当文件已存在时，如何处理”
enum class ImportMode {
    APPEND,     // 追加（合并）
    OVERWRITE   // 覆盖（替换）
}

// 导出格式：负责“数据以什么形式保存”
enum class ExportFormat {
    PLAINTEXT,  // 明文（可读，方便调试）
    ENCRYPTED   // 加密（安全，推荐）
}

// 用数据类统一管理备份配置（或者放在方法参数里）
data class BackupConfig(
    val importMode: ImportMode = ImportMode.APPEND,
    val exportFormat: ExportFormat = ExportFormat.ENCRYPTED,
    val fileNameLength: Int = 32
)