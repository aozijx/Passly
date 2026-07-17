package com.aozijx.passly.core.autofill.dispatcher

import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.autofill.model.ResponseContext
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.security.session.SessionStateProvider

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
    private val sessionState: SessionStateProvider,
    private val candidateResolver: CandidateResolver,
    private val fieldMatchStrategy: FieldMatchStrategy,
    private val responseFactory: ResponseFactory,
) {

    companion object {
        private const val TAG = "FillDispatcher"
    }

    /**
     * 根据请求执行填充 Pipeline。
     *
     * @param request 已转换为内部模型的填充请求
     * @return InternalFillResponse。若 vault 锁定或管道任一阶段无结果，返回空 entries。
     */
    fun dispatch(request: InternalFillRequest): InternalFillResponse {
        if (sessionState.isLocked()) {
            Logcat.i(TAG, "Vault locked, skip fill for ${request.parentPackage}")
            return InternalFillResponse()
        }

        val candidates = candidateResolver.resolve(request)
        if (candidates.isEmpty()) {
            Logcat.i(TAG, "No candidates for ${request.parentPackage}")
            return InternalFillResponse()
        }

        val matchResult = fieldMatchStrategy.match(request)
        if (!matchResult.hasCredentials) {
            Logcat.i(TAG, "No credential fields matched for ${request.parentPackage}")
            return InternalFillResponse()
        }

        val response = responseFactory.build(
            ResponseContext(
                candidates = candidates,
                roleMap = matchResult.roleMap,
                parentPackage = request.parentPackage,
            ),
        )
        Logcat.i(
            TAG,
            "Dispatched ${response.candidates.size} candidates for ${request.parentPackage}"
        )
        return response
    }
}