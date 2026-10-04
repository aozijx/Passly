package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind

internal object KeyedBackupBundleValidator {
    private val safeId = Regex("[A-Za-z0-9_-]{1,160}")
    private val sha256 = Regex("[A-Fa-f0-9]{64}")
    private val archiveOnlyTextFields = setOf("icon_name", "icon_color")
    private val commonFields = setOf(
        FieldKey.TITLE,
        FieldKey.USERNAME,
        FieldKey.PRIMARY_URL,
        FieldKey.DOMAINS,
        FieldKey.APPLICATION_IDS,
        FieldKey.EXPIRES_AT,
        FieldKey.FAVORITE,
        FieldKey.TAGS,
        FieldKey.CUSTOM_FIELDS,
    )

    fun validate(bundle: BackupBundle, requireResourceData: Boolean) {
        val document = bundle.document
        require(document.format == BackupDocument.FORMAT) { "不支持的备份文档格式: ${document.format}" }
        require(document.version == BackupDocument.CURRENT_VERSION) { "不支持的备份文档版本: ${document.version}" }
        require(document.exportedAt >= 0) { "备份导出时间无效" }
        require(document.entries.size <= BackupBundleValidator.MAX_ENTRIES) { "备份条目数量过多" }
        require(document.resources.size <= BackupBundleValidator.MAX_RESOURCES) { "备份资源数量过多" }
        require(document.entries.map(BackupEntryRecord::id).distinct().size == document.entries.size) {
            "备份包含重复条目 ID"
        }
        require(document.resources.map { it.id }.distinct().size == document.resources.size) {
            "备份包含重复资源 ID"
        }

        val entryTypes = document.entries.associate { entry ->
            require(safeId.matches(entry.id)) { "备份包含不安全的条目 ID: ${entry.id}" }
            val type = requireNotNull(BackupArchiveKeyRegistry.entryType(entry.type)) {
                "未知条目类型: ${entry.type}"
            }
            validateEntry(entry, type)
            entry.id to type
        }
        val entryIds = entryTypes.keys
        val resourceIds = document.resources.mapTo(linkedSetOf()) { resource ->
            require(safeId.matches(resource.id)) { "备份包含不安全的资源 ID: ${resource.id}" }
            require(resource.entryId in entryIds) { "备份资源引用了不存在的条目" }
            resource.id
        }

        validateLinks(document.links, entryTypes)
        validateResourceIntegrity(
            bundle = bundle,
            entryIds = entryIds,
            requireResourceData = requireResourceData,
            resourceIds = resourceIds,
        )
    }

    private fun validateEntry(
        entry: BackupEntryRecord,
        type: com.aozijx.passly.domain.entry.model.EntryType,
    ) {
        require(entry.revision >= 1) { "条目版本无效: ${entry.id}" }
        require(entry.createdAt >= 0 && entry.updatedAt >= entry.createdAt) { "条目时间无效: ${entry.id}" }
        require(entry.deletedAt == null || entry.deletedAt >= entry.createdAt) { "条目删除时间无效: ${entry.id}" }
        require(entry.attachmentIds.distinct().size == entry.attachmentIds.size) {
            "条目包含重复附件 ID: ${entry.id}"
        }
        val keys = entry.fields.map { it.key }
        require(keys.distinct().size == keys.size) { "条目包含重复字段: ${entry.id}" }
        val definition = EntryTypeDefinitions[type]
        val knownFields = linkedMapOf<FieldKey, BackupFieldValue>()
        entry.fields.forEach { field ->
            validateFieldValue(field.value, entry.id, field.key)
            val domainKey = BackupArchiveKeyRegistry.field(field.key)
            if (domainKey == null) {
                require(field.key in archiveOnlyTextFields) { "未知字段: ${field.key} (${entry.id})" }
                require(field.value.kind == BackupFieldValue.KIND_TEXT) {
                    "字段值类型错误: ${field.key} (${entry.id})"
                }
            } else {
                require(domainKey in commonFields || definition.supports(domainKey)) {
                    "条目类型不支持字段: ${field.key} (${entry.id})"
                }
                require(field.value.kind == expectedKind(domainKey, definition[domainKey]?.valueType)) {
                    "字段值类型错误: ${field.key} (${entry.id})"
                }
                knownFields[domainKey] = field.value
            }
        }
        definition.requiredFields.forEach { required ->
            require(knownFields[required]?.isNonEmpty() == true) {
                "条目缺少必填字段: ${BackupArchiveKeyRegistry.fieldKey(required)} (${entry.id})"
            }
        }
        validateOtp(entry, knownFields)
    }

    private fun expectedKind(key: FieldKey, definitionType: EntryFieldValueType?): String = when (key) {
        FieldKey.CUSTOM_FIELDS -> BackupFieldValue.KIND_CUSTOM_FIELDS
        else -> when (definitionType ?: when (key) {
            FieldKey.FAVORITE -> EntryFieldValueType.BOOLEAN
            FieldKey.EXPIRES_AT -> EntryFieldValueType.LONG
            FieldKey.TAGS, FieldKey.DOMAINS, FieldKey.APPLICATION_IDS -> EntryFieldValueType.TEXT_LIST
            else -> EntryFieldValueType.TEXT
        }) {
            EntryFieldValueType.TEXT -> BackupFieldValue.KIND_TEXT
            EntryFieldValueType.BOOLEAN -> BackupFieldValue.KIND_BOOLEAN
            EntryFieldValueType.INTEGER -> BackupFieldValue.KIND_INTEGER
            EntryFieldValueType.LONG -> BackupFieldValue.KIND_LONG
            EntryFieldValueType.TEXT_LIST -> BackupFieldValue.KIND_TEXT_LIST
        }
    }

    private fun validateFieldValue(value: BackupFieldValue, entryId: String, key: String) {
        val populated = listOfNotNull(
            value.text,
            value.texts,
            value.booleanValue,
            value.integerValue,
            value.longValue,
            value.customFields,
        ).size
        require(populated == 1) { "字段必须且只能包含一个值: $key ($entryId)" }
        val matchingPayload = when (value.kind) {
            BackupFieldValue.KIND_TEXT -> value.text != null
            BackupFieldValue.KIND_TEXT_LIST -> value.texts != null
            BackupFieldValue.KIND_BOOLEAN -> value.booleanValue != null
            BackupFieldValue.KIND_INTEGER -> value.integerValue != null
            BackupFieldValue.KIND_LONG -> value.longValue != null
            BackupFieldValue.KIND_CUSTOM_FIELDS -> value.customFields != null
            else -> false
        }
        require(matchingPayload) { "未知或不匹配的字段值类型: ${value.kind} ($key, $entryId)" }
        value.texts?.let { values -> require(values.none(String::isBlank)) { "列表字段包含空值: $key ($entryId)" } }
        value.customFields?.forEach { field ->
            require(field.name.isNotBlank()) { "自定义字段名称为空: $entryId" }
            require(field.kind == BackupCustomFieldValue.KIND_TEXT || field.kind == BackupCustomFieldValue.KIND_HIDDEN) {
                "未知自定义字段类型: ${field.kind} ($entryId)"
            }
        }
    }

    private fun BackupFieldValue.isNonEmpty(): Boolean = when (kind) {
        BackupFieldValue.KIND_TEXT -> text?.isNotBlank() == true
        BackupFieldValue.KIND_TEXT_LIST -> texts?.any(String::isNotBlank) == true
        BackupFieldValue.KIND_BOOLEAN -> booleanValue != null
        BackupFieldValue.KIND_INTEGER -> integerValue != null
        BackupFieldValue.KIND_LONG -> longValue != null
        BackupFieldValue.KIND_CUSTOM_FIELDS -> customFields?.isNotEmpty() == true
        else -> false
    }

    private fun validateOtp(entry: BackupEntryRecord, fields: Map<FieldKey, BackupFieldValue>) {
        if (BackupArchiveKeyRegistry.entryType(entry.type) != com.aozijx.passly.domain.entry.model.EntryType.OTP) return
        val type = fields[FieldKey.OTP_TYPE]?.text ?: "totp"
        val digits = fields[FieldKey.OTP_DIGITS]?.integerValue ?: 6
        require(digits in 5..10) { "OTP 位数无效: ${entry.id}" }
        when (type) {
            "hotp" -> require((fields[FieldKey.OTP_COUNTER]?.longValue ?: -1L) >= 0) {
                "HOTP 计数器无效: ${entry.id}"
            }
            "totp", "steam" -> require((fields[FieldKey.OTP_PERIOD]?.integerValue ?: 30) in 1..300) {
                "OTP 周期无效: ${entry.id}"
            }
            else -> throw IllegalArgumentException("未知 OTP 类型: $type (${entry.id})")
        }
    }

    private fun validateLinks(
        links: List<com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord>,
        entryTypes: Map<String, com.aozijx.passly.domain.entry.model.EntryType>,
    ) {
        require(links.map { it.id }.distinct().size == links.size) { "备份包含重复关系 ID" }
        require(links.map { Triple(it.sourceEntryId, it.targetEntryId, it.relationType) }.distinct().size == links.size) {
            "备份包含重复关系"
        }
        links.forEach { link ->
            require(safeId.matches(link.id)) { "关系 ID 无效: ${link.id}" }
            val sourceType = requireNotNull(entryTypes[link.sourceEntryId]) { "关系引用了不存在的条目: ${link.id}" }
            val targetType = requireNotNull(entryTypes[link.targetEntryId]) { "关系引用了不存在的条目: ${link.id}" }
            require(link.sourceEntryId != link.targetEntryId) { "条目不能关联自身: ${link.id}" }
            val relationType: EntryRelationType = requireNotNull(
                BackupArchiveKeyRegistry.relationType(link.relationType),
            ) { "未知关系类型: ${link.relationType}" }
            require(EntryLinkPolicy.isAllowed(relationType, sourceType, targetType)) {
                "关系方向或条目类型无效: ${link.id}"
            }
            require(link.createdAt >= 0 && link.updatedAt >= link.createdAt) { "关系时间无效: ${link.id}" }
        }
    }

    internal fun validateResourceIntegrity(
        bundle: BackupBundle,
        entryIds: Set<String>,
        requireResourceData: Boolean,
        resourceIds: Set<String> = bundle.document.resources.mapTo(linkedSetOf()) { it.id },
    ) {
        val document = bundle.document
        document.resources.groupBy { it.entryId }.forEach { (entryId, resources) ->
            require(entryId in entryIds)
            require(resources.count { it.kind == BackupResourceKind.ICON } <= 1) {
                "条目包含多个图标资源: $entryId"
            }
        }
        require(bundle.resourceData.keys.all { it in resourceIds }) { "备份包含未声明的资源数据" }
        val attachments = document.resources.filter { it.kind == BackupResourceKind.ATTACHMENT }
            .groupBy({ it.entryId }, { it.id })
        require(document.entries.all { it.attachmentIds.toSet() == attachments[it.id].orEmpty().toSet() }) {
            "条目附件清单与资源清单不一致"
        }
        if (requireResourceData) require(bundle.resourceData.keys == resourceIds) {
            "备份资源元数据与资源内容不完整"
        }
        var totalBytes = 0L
        document.resources.forEach { record ->
            require(record.size in 0..BackupBundleValidator.MAX_RESOURCE_BYTES.toLong()) { "备份资源大小无效: ${record.id}" }
            require(sha256.matches(record.sha256)) { "备份资源 SHA-256 无效: ${record.id}" }
            require(record.createdAt == null || record.createdAt >= 0) { "备份资源创建时间无效: ${record.id}" }
            require(record.fileName == null || record.fileName.length <= 512) { "备份资源文件名过长: ${record.id}" }
            require(record.mimeType == null || record.mimeType.length <= 255) { "备份资源 MIME 类型过长: ${record.id}" }
            bundle.resourceData[record.id]?.let { content ->
                require(content.size.toLong() == record.size) { "备份资源大小不匹配: ${record.id}" }
                require(BackupBundleValidator.sha256Hex(content).equals(record.sha256, ignoreCase = true)) {
                    "备份资源校验失败: ${record.id}"
                }
                totalBytes += content.size
                require(totalBytes <= BackupBundleValidator.MAX_TOTAL_RESOURCE_BYTES) { "备份资源总大小超限" }
            }
        }
    }
}
