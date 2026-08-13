package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupOtpType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
import java.security.MessageDigest

object BackupBundleValidator {
    const val MAX_RESOURCE_BYTES = 16 * 1024 * 1024
    const val MAX_TOTAL_RESOURCE_BYTES = 128 * 1024 * 1024
    const val MAX_ENTRIES = 100_000
    const val MAX_RESOURCES = 100_000
    private val SAFE_ID = Regex("[A-Za-z0-9_-]{1,160}")
    private val SHA_256 = Regex("[A-Fa-f0-9]{64}")

    fun validate(bundle: BackupBundle, requireResourceData: Boolean) {
        val document = bundle.document
        require(document.format == BackupDocument.FORMAT) {
            "不支持的备份文档格式: ${document.format}"
        }
        require(document.version == BackupDocument.CURRENT_VERSION) {
            "不支持的备份文档版本: ${document.version}"
        }
        require(document.entries.size <= MAX_ENTRIES) { "备份条目数量过多" }
        require(document.resources.size <= MAX_RESOURCES) { "备份资源数量过多" }
        require(document.exportedAt >= 0) { "备份导出时间无效" }
        require(document.entries.map { it.id }.toSet().size == document.entries.size) {
            "备份包含重复条目 ID"
        }
        require(document.resources.map { it.id }.toSet().size == document.resources.size) {
            "备份包含重复资源 ID"
        }

        val entryIds = document.entries.mapTo(hashSetOf()) { it.id }
        val entryTypesById = document.entries.associate { it.id to EntryType.valueOf(it.type) }
        val resourceIds = document.resources.mapTo(hashSetOf()) { it.id }
        require(
            entryIds.all(SAFE_ID::matches) &&
                resourceIds.all(SAFE_ID::matches)
        ) {
            "备份包含不安全的条目或资源 ID"
        }
        require(document.resources.all { it.entryId in entryIds }) {
            "备份资源引用了不存在的条目"
        }
        val validTypes = EntryType.entries.mapTo(hashSetOf()) { it.name }
        document.entries.forEach { entry ->
            require(entry.type in validTypes) { "未知条目类型: ${entry.type}" }
            if (entry.type == EntryType.ACCOUNT.name) {
                require(entry.secret == com.aozijx.passly.feature.backup.internal.archive.model.BackupSecretRecord()) {
                    "ACCOUNT 不能包含敏感 payload: ${entry.id}"
                }
            }
            requireAtomicSecret(entry)
            require(entry.version >= 1) { "条目版本无效: ${entry.id}" }
            require(entry.createdAt >= 0 && entry.updatedAt >= entry.createdAt) {
                "条目时间无效: ${entry.id}"
            }
            require(entry.deletedAt == null || entry.deletedAt >= entry.createdAt) {
                "条目删除时间无效: ${entry.id}"
            }
            require(entry.attachmentIds.size == entry.attachmentIds.toSet().size) {
                "条目包含重复附件 ID: ${entry.id}"
            }
            entry.secret.otp?.config?.let { otp ->
                require(otp.secret.isNotBlank()) { "OTP 密钥为空: ${entry.id}" }
                require(otp.digits in 5..10) { "OTP 位数无效: ${entry.id}" }
                when (otp.type) {
                    BackupOtpType.HOTP -> require(otp.counter != null && otp.counter >= 0) {
                        "HOTP 计数器无效: ${entry.id}"
                    }

                    BackupOtpType.TOTP, BackupOtpType.STEAM ->
                        require(otp.periodSeconds != null && otp.periodSeconds in 1..300) {
                            "OTP 周期无效: ${entry.id}"
                        }
                }
            }
        }
        val relationTypes = EntryRelationType.entries.mapTo(hashSetOf()) { it.name }
        require(document.links.map { it.id }.toSet().size == document.links.size) {
            "备份包含重复关系 ID"
        }
        require(
            document.links.map { Triple(it.sourceEntryId, it.targetEntryId, it.relationType) }
                .toSet().size == document.links.size
        ) {
            "备份包含重复关系"
        }
        document.links.forEach { link ->
            require(SAFE_ID.matches(link.id)) { "关系 ID 无效: ${link.id}" }
            require(link.sourceEntryId in entryIds && link.targetEntryId in entryIds) {
                "关系引用了不存在的条目: ${link.id}"
            }
            require(link.sourceEntryId != link.targetEntryId) { "条目不能关联自身: ${link.id}" }
            require(link.relationType in relationTypes) { "未知关系类型: ${link.relationType}" }
            require(
                EntryLinkPolicy.isAllowed(
                    relationType = EntryRelationType.valueOf(link.relationType),
                    sourceType = requireNotNull(entryTypesById[link.sourceEntryId]),
                    targetType = requireNotNull(entryTypesById[link.targetEntryId]),
                )
            ) { "关系方向或条目类型无效: ${link.id}" }
            require(link.createdAt >= 0 && link.updatedAt >= link.createdAt) {
                "关系时间无效: ${link.id}"
            }
        }
        document.resources.groupBy { it.entryId }.forEach { (entryId, resources) ->
            require(resources.count { it.kind == BackupResourceKind.ICON } <= 1) {
                "条目包含多个图标资源: $entryId"
            }
        }
        require(bundle.resourceData.keys.all { it in resourceIds }) {
            "备份包含未声明的资源数据"
        }
        val attachmentsByEntry = document.resources
            .filter { it.kind == BackupResourceKind.ATTACHMENT }
            .groupBy({ it.entryId }, { it.id })
        require(document.entries.all { entry ->
            entry.attachmentIds.toSet() == attachmentsByEntry[entry.id].orEmpty().toSet()
        }) {
            "条目附件清单与资源清单不一致"
        }
        if (requireResourceData) {
            require(bundle.resourceData.keys == resourceIds) {
                "备份资源元数据与资源内容不完整"
            }
        }

        var totalBytes = 0L
        document.resources.forEach { record ->
            require(record.size in 0..MAX_RESOURCE_BYTES.toLong()) {
                "备份资源大小无效: ${record.id}"
            }
            require(SHA_256.matches(record.sha256)) {
                "备份资源 SHA-256 无效: ${record.id}"
            }
            require(record.createdAt == null || record.createdAt >= 0) {
                "备份资源创建时间无效: ${record.id}"
            }
            require(record.fileName == null || record.fileName.length <= 512) {
                "备份资源文件名过长: ${record.id}"
            }
            require(record.mimeType == null || record.mimeType.length <= 255) {
                "备份资源 MIME 类型过长: ${record.id}"
            }
            val content = bundle.resourceData[record.id] ?: return@forEach
            require(content.size.toLong() == record.size) {
                "备份资源大小不匹配: ${record.id}"
            }
            val expectedHash = record.sha256
            require(sha256Hex(content).equals(expectedHash, ignoreCase = true)) {
                "备份资源校验失败: ${record.id}"
            }
            totalBytes += content.size
            require(totalBytes <= MAX_TOTAL_RESOURCE_BYTES) { "备份资源总大小超限" }
        }
    }

    fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data)
            .joinToString("") { "%02x".format(it) }

    private fun requireAtomicSecret(
        entry: com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
    ) {
        val secret = entry.secret
        val populated = buildSet {
            if (secret.login != null) add("login")
            if (secret.card != null) add("card")
            if (secret.identity != null) add("identity")
            if (secret.ssh != null) add("ssh")
            if (secret.wifi != null) add("wifi")
            if (secret.passkey != null) add("passkey")
            if (secret.otp != null) add("otp")
        }
        val allowed = when (EntryType.valueOf(entry.type)) {
            EntryType.ACCOUNT, EntryType.NOTE -> null
            EntryType.BANK_CARD, EntryType.BANK_CARD -> "card"
            EntryType.ID_CARD,
            EntryType.PASSPORT,
            EntryType.DRIVER_LICENSE,
            EntryType.ID_CARD,
            EntryType.SEED_PHRASE,
            EntryType.RECOVERY_CODE -> "identity"

            EntryType.SSH_KEY -> "ssh"
            EntryType.WIFI -> "wifi"
            EntryType.PASSKEY -> "passkey"
            EntryType.OTP -> "otp"
            EntryType.LOGIN,
            EntryType.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL,
            EntryType.API_KEY,
            EntryType.CRYPTO_WALLET -> "login"
        }
        require(populated.size <= 1) {
            "条目包含多个凭据 payload: ${entry.id}"
        }
        require(populated.isEmpty() || populated.single() == allowed) {
            "条目类型与凭据 payload 不匹配: ${entry.id}"
        }
    }
}
