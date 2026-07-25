package com.aozijx.passly.core.autofill.model

/**
 * ResponseFactory 的单参数输入：已解析的 [ResolvedCandidate] + 字段角色映射。
 *
 * 由 Dispatcher 在 strategy.match() 之后构建，
 * MatchResult.roleMap 在这里预先与每个 Candidate 绑定。
 */
data class ResponseContext(
    val candidates: List<ResolvedCandidate>,
    /** key = FieldDescriptor.id, value = FieldRole */
    val roleMap: Map<String, FieldRole>,
    val parentPackage: String,
)
