package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.model.ResponseContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 响应工厂：将 [ResponseContext] 组装为 [InternalFillResponse]。
 *
 * 接收已由 [CandidateResolver] 解析的 [ResolvedCandidate] 列表，
 * 根据 [FieldRole] 映射填充 fields，供下游（Legacy/Modern Factory、BottomSheet）统一消费。
 *
 * 不再依赖 [com.aozijx.passly.domain.model.credential.CredentialCandidate] 或 [com.aozijx.passly.domain.entry.model.EntryAggregate]。
 */
@Singleton
class ResponseFactory @Inject constructor() {

    companion object {
        private const val TAG = "ResponseFactory"
    }

    fun build(context: ResponseContext): InternalFillResponse {
        val resolved = context.candidates.map { candidate ->
            candidate.copy(fields = buildFieldValueMap(candidate, context.roleMap))
        }
        if (resolved.isEmpty()) {
            AppTelemetry.w(TAG, "No candidates could be resolved")
            return InternalFillResponse()
        }
        AppTelemetry.i(TAG, "Built ${resolved.size} candidates")
        return InternalFillResponse(candidates = resolved, origin = context.parentPackage)
    }

    private fun buildFieldValueMap(
        candidate: ResolvedCandidate,
        roleMap: Map<String, FieldRole>,
    ): Map<FieldRole, String> {
        val map = mutableMapOf<FieldRole, String>()
        if (roleMap.values.any { it == FieldRole.USERNAME }) {
            map[FieldRole.USERNAME] = candidate.username
        }
        if (roleMap.values.any { it == FieldRole.PASSWORD }) {
            map[FieldRole.PASSWORD] = candidate.password
        }
        if (roleMap.values.any { it == FieldRole.OTP }) {
            candidate.totpCode?.let { map[FieldRole.OTP] = it }
        }
        return map
    }
}
