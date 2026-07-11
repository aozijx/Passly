package com.aozijx.passly.core.autofill.model

import com.aozijx.passly.domain.model.CredentialCandidate

/**
 * ResponseFactory 的单参数输入：候选项 + 已解析的字段角色映射。
 *
 * 由 Dispatcher 在 strategy.match() 之后构建，
 * MatchResult.roleMap 在这里预先与每个 Candidate 绑定。
 */
data class ResponseContext(
    val candidates: List<CredentialCandidate>,
    /** key = FieldDescriptor.id, value = FieldRole */
    val roleMap: Map<String, FieldRole>,
    val parentPackage: String,
)