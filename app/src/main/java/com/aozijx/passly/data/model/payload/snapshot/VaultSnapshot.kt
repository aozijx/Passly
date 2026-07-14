package com.aozijx.passly.data.model.payload.snapshot

import com.aozijx.passly.data.model.entity.EntryType
import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.data.model.payload.backup.BackupSchema
import com.aozijx.passly.data.model.payload.credential.CredentialPayload
import com.aozijx.passly.data.model.payload.metadata.MetadataPayload
import kotlinx.serialization.Serializable

enum class Platform {
    ANDROID,
    DESKTOP,
    IOS
}

/**
 * 通用快照模型，用于历史恢复、备份导出/导入。
 *
 * 不直接存数据库。仅在快照/备份流中使用。
 * 对象组合：metadata + credential + attachments 各自独立序列化。
 * AES-GCM 已提供 Authentication Tag 保证完整性和认证，无需额外校验和。
 *
 * 扩展方式：新增子 Payload 在此追加字段，不修改现有字段签名。
 *
 *          VaultSnapshot
 *              │
 *     ┌────────┼────────┐
 *     ▼        ▼        ▼
 *  Metadata Credential Attachments(List)
 *
 * @param schemaVersion  快照格式版本，用于未来导入兼容
 * @param deletedAt      逻辑删除时间戳。默认导出时跳过已删除条目
 * @param source         来源标识（如 "Passly Android 2.0.0"）
 * @param appVersion     导出时的应用版本
 * @param platform       导出平台（ANDROID/DESKTOP/IOS）
 * @param revision       条目修订版本号，用于历史恢复和同步
 */
@Serializable
data class VaultSnapshot(
    val id: String,
    val vaultId: String = "default",
    val entryType: EntryType = EntryType.LOGIN,

    val schemaVersion: Int = BackupSchema.VERSION,
    val deletedAt: Long? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastUsedAt: Long? = null,

    val revision: Int = 1,

    val source: String? = null,
    val appVersion: String? = null,
    val platform: Platform? = null,

    val metadata: MetadataPayload,
    val credential: CredentialPayload = CredentialPayload(),
    val attachments: List<AttachmentPayload> = emptyList()
)
