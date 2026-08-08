package com.aozijx.passly.core.platform

import android.content.Context
import java.io.File

/**
 * Vault 资源目录统一路径管理。
 *
 * 清理、备份、恢复、附件存储、图标存储共享同一路径规则，
 * 禁止在业务代码中硬编码 "attachments"、"vault_images" 等字符串。
 */
object VaultResourcePaths {

    const val ATTACHMENTS = "attachments"
    const val VAULT_IMAGES = "vault_images"

    /** 所有 Vault 资源目录名称（用于批量清理/恢复） */
    val RESOURCE_DIRECTORY_NAMES = listOf(ATTACHMENTS, VAULT_IMAGES)

    fun attachmentDir(context: Context): File = File(context.filesDir, ATTACHMENTS)
    fun vaultImagesDir(context: Context): File = File(context.filesDir, VAULT_IMAGES)

    fun resourceDirectories(context: Context): List<File> =
        RESOURCE_DIRECTORY_NAMES.map { File(context.filesDir, it) }
}