package com.aozijx.passly.core.autofill.dispatcher

import com.aozijx.passly.app.diagnostics.AppTelemetry
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

    companion object {
        private const val TAG = "FillDispatcher"
    }

    /**
     * 根据请求执行填充 Pipeline。
     *
     * @param request 已转换为内部模型的填充请求
     * @return InternalFillResponse。若 vault 锁定或管道任一阶段无结果，返回空 entries。
     *
     * 触发判定（是否有可编辑输入框）与角色识别（能否猜出 username/password）分离：
     * 样式化页面识别不出角色，但只要存在可编辑输入框就进入候选解析，
     * 由包名/域名过滤决定最终是否弹出填充入口。
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
        // 页面没有任何可编辑输入框才判定为不支持（不打扰无关页面）。
        if (!matchResult.hasEditableFields) {
            AppTelemetry.i(TAG, "No editable fields on page")
            return InternalFillResponse(availability = FillAvailability.UNSUPPORTED_FIELDS)
        }

        // 有输入框但 vault 锁定 → 给出解锁入口（未登录的无关页面已在上面被过滤）。
        if (!sessionState.hasFullSecureSessionAccess()) {
            AppTelemetry.i(TAG, "Vault locked; fill request requires unlock")
            return InternalFillResponse(
                availability = FillAvailability.LOCKED,
                requireAuthentication = policy.requireAuthentication,
                presentation = policy.presentation,
            )
        }

        val candidates = candidateResolver.resolve(request, policy)
        if (candidates.isEmpty()) {
            AppTelemetry.i(TAG, "No autofill candidates")
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
        AppTelemetry.i(TAG, "Dispatched ${response.candidates.size} candidates")
        return response.copy(
            requireAuthentication = policy.requireAuthentication,
            savePromptsEnabled = policy.savePromptsEnabled,
            presentation = policy.presentation,
        )
    }
}
