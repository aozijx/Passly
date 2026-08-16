package com.aozijx.passly.core.autofill.dispatcher

import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.model.FillAvailability
import com.aozijx.passly.core.autofill.model.FillRequestSource
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResponseContext
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.first

/**
 * 统一填充 Pipeline：纯编排调度，不负责查询、匹配、组装的实现细节。
 *
 * 职责仅限于流程控制：
 * 1. 检查 Vault 锁定状态
 * 2. 委托 [CandidateResolver] 查找候选项
 * 3. 委托 [com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy] 匹配字段
 * 4. 委托 [ResponseFactory] 组装响应
 *
 * 新增 Passkey / OTP / CreditCard / Identity 等凭据类型时只需替换组件实现，
 * Dispatcher 本身无需修改。
 *
 * 严禁引用 AutofillService、CredentialProviderService 或任何 android.service 包。
 */
class FillRequestDispatcher(
    private val sessionState: SecureSessionAccessState,
    private val candidateResolver: CandidateResolver,
    private val fieldMatchStrategy: FieldMatchStrategy,
    private val responseFactory: ResponseFactory,
    private val settingsRepository: AppSettingsRepository,
) {

    /**
     * 根据请求执行填充 Pipeline。
     *
     * @param request 已转换为内部模型的填充请求
     * @return InternalFillResponse。若 vault 锁定或管道任一阶段无结果，返回空 entries。
     *
     * 触发条件为"识别出凭据字段"（username/password/otp）而非"存在任意输入框"：
     * 搜索框、普通单输入表单不会触发自动填充。样式化页面的密码框通过
     * inputType 可靠识别，不会因 id/hint 非标准而漏掉。
     */
    suspend fun dispatch(request: InternalFillRequest): InternalFillResponse {
        val policy = settingsRepository.settings.first().interaction.autofill
        if (!policy.enabled ||
            (request.source == FillRequestSource.CREDENTIAL_MANAGER &&
                    !policy.credentialManagerEnabled)
        ) {
            return InternalFillResponse(availability = FillAvailability.DISABLED)
        }

        val matchResult = fieldMatchStrategy.match(request)
        // 未识别出任何凭据字段（非登录/注册表单）→ 不触发。
        if (!matchResult.hasCredentials) {
            return InternalFillResponse(availability = FillAvailability.UNSUPPORTED_FIELDS)
        }

        // 有凭据字段但 vault 锁定 → 给出解锁入口。
        if (!sessionState.hasFullSecureSessionAccess()) {
            return InternalFillResponse(
                availability = FillAvailability.LOCKED,
                requireAuthentication = policy.requireAuthentication,
                presentation = policy.presentation,
            )
        }

        val candidates = candidateResolver.resolve(request, policy)
        if (candidates.isEmpty()) {
            return InternalFillResponse(
                availability = FillAvailability.NO_MATCH,
                requireAuthentication = policy.requireAuthentication,
                savePromptsEnabled = policy.savePromptsEnabled,
                presentation = policy.presentation,
            )
        }

        val response = responseFactory.build(
            ResponseContext(
                candidates = candidates,
                roleMap = matchResult.roleMap,
                parentPackage = request.parentPackage,
            ),
        )
        return response.copy(
            requireAuthentication = policy.requireAuthentication,
            savePromptsEnabled = policy.savePromptsEnabled,
            presentation = policy.presentation,
        )
    }
}
