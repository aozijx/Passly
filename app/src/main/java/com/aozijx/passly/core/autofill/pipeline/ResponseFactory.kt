package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.autofill.model.ResponseContext
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.domain.model.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 响应工厂：将 FillContext 组装为 [com.aozijx.passly.core.autofill.model.InternalFillResponse]。
 * 负责解密凭据、生成 TOTP、构建字段映射和副标题。
 */
@Singleton
class ResponseFactory @Inject constructor() {

    companion object {
        private const val TAG = "ResponseFactory"
    }

    fun build(context: ResponseContext): InternalFillResponse {
        val resolved = context.candidates.mapNotNull { candidate ->
            buildCandidate(candidate, context.roleMap)
        }
        if (resolved.isEmpty()) {
            Logcat.w(TAG, "No entries could be resolved for ${context.parentPackage}")
            return InternalFillResponse()
        }
        Logcat.i(TAG, "Built ${resolved.size} candidates for ${context.parentPackage}")
        return InternalFillResponse(candidates = resolved, origin = context.parentPackage)
    }

    private fun buildCandidate(
        candidate: CredentialCandidate,
        roleMap: Map<String, FieldRole>,
    ): ResolvedCandidate? {
        val entry = candidate.entry
        return try {
            ResolvedCandidate(
                candidateId = entry.id,
                displayName = entry.title,
                username = entry.username,
                password = entry.password,
                totpCode = TwoFAUtils.generateCurrentTotpFromEntry(entry),
                associatedDomain = entry.associatedDomain,
                subtitle = buildSubtitle(candidate),
                fields = buildFieldValueMap(entry, roleMap),
            )
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to resolve entry ${entry.id}", e)
            null
        }
    }

    private fun buildFieldValueMap(
        entry: VaultEntry,
        roleMap: Map<String, FieldRole>,
    ): Map<FieldRole, String> {
        val map = mutableMapOf<FieldRole, String>()
        if (roleMap.values.any { it == FieldRole.USERNAME }) {
            map[FieldRole.USERNAME] = entry.username
        }
        if (roleMap.values.any { it == FieldRole.PASSWORD }) {
            map[FieldRole.PASSWORD] = entry.password
        }
        if (roleMap.values.any { it == FieldRole.OTP }) {
            TwoFAUtils.generateCurrentTotpFromEntry(entry)?.let {
                map[FieldRole.OTP] = it
            }
        }
        return map
    }

    private fun buildSubtitle(candidate: CredentialCandidate): String {
        val parts = mutableListOf<String>()
        when (candidate.matchedBy) {
            MatchType.PACKAGE_NAME -> {
                candidate.matchedPackage?.let { parts.add(it) }
            }

            MatchType.WEB_DOMAIN -> {
                candidate.matchedDomain?.let { parts.add(it) }
            }

            else -> { /* TITLE, URL, DIGITAL_ASSET_LINK 目前不展示匹配详情 */
            }
        }
        if (candidate.entry.totpSecret?.isNotBlank() == true) {
            parts.add("2FA")
        }
        return parts.joinToString(" · ")
    }
}