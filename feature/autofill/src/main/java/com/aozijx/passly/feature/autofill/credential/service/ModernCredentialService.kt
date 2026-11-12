package com.aozijx.passly.feature.autofill.credential.service

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.launchAutofillServiceCleanup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modern CredentialProviderService 薄适配器（API 34+）。
 *
 * 职责仅限于：
 * BeginGetCredentialRequest → [CredentialBeginGetHandler] → BeginGetCredentialResponse。
 * 严禁在此类中编写任何业务判断逻辑。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class ModernCredentialService : CredentialProviderService() {
    @Inject
    internal lateinit var beginGetHandler: CredentialBeginGetHandler

    @Inject
    internal lateinit var beginCreateHandler: CredentialBeginCreateHandler

    @Inject
    internal lateinit var finishInteraction: FinishAutofillInteraction

    @Inject
    lateinit var logger: Logger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ModernCred"
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        val job =
            scope.launch {
                logger.withRootTrace(Operations.AutofillCredentialRequest) {
                    logger.log(LogStatements.Autofill.discoveryStarted())
                    var pendingInteractionId: AutofillInteractionId? = null
                    try {
                        val result = beginGetHandler.resolve(request)
                        pendingInteractionId = result.interactionId
                        if (!cancellationSignal.isCanceled) {
                            callback.onResult(result.response)
                            pendingInteractionId = null
                        } else {
                            pendingInteractionId?.let { finishInteraction.cancel(it) }
                            pendingInteractionId = null
                            logger.log(LogStatements.Autofill.requestCancelled(AutofillInteractionSource.CREDENTIAL_MANAGER))
                        }
                    } catch (e: CancellationException) {
                        try {
                            withContext(NonCancellable) {
                                pendingInteractionId?.let { finishInteraction.cancel(it) }
                            }
                        } catch (cleanupFailure: Exception) {
                            e.addSuppressed(cleanupFailure)
                        }
                        logger.log(LogStatements.Autofill.requestCancelled(AutofillInteractionSource.CREDENTIAL_MANAGER))
                        throw e
                    } catch (e: Exception) {
                        try {
                            withContext(NonCancellable) {
                                pendingInteractionId?.let { finishInteraction.cancel(it) }
                            }
                        } catch (cleanupFailure: Exception) {
                            e.addSuppressed(cleanupFailure)
                        }
                        logger.log(LogStatements.Autofill.discoveryFailed(e))
                        if (!cancellationSignal.isCanceled) {
                            callback.onError(GetCredentialUnknownException("Credential discovery failed"))
                        }
                    }
                }
            }
        cancellationSignal.setOnCancelListener(job::cancel)
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val job =
            scope.launch {
                logger.withRootTrace(Operations.AutofillCredentialRequest) {
                    logger.log(LogStatements.Autofill.saveStarted())
                    try {
                        val response = beginCreateHandler.resolve(request, this@ModernCredentialService)
                        if (!cancellationSignal.isCanceled) {
                            callback.onResult(response)
                        }
                    } catch (e: CancellationException) {
                        logger.log(LogStatements.Autofill.requestCancelled(AutofillInteractionSource.CREDENTIAL_MANAGER))
                        throw e
                    } catch (e: CreateCredentialException) {
                        logger.log(LogStatements.Autofill.saveFailed(e, Reasons.Autofill.Matching.NoFillableFields))
                        if (!cancellationSignal.isCanceled) callback.onError(e)
                    } catch (e: Exception) {
                        logger.log(LogStatements.Autofill.saveFailed(e, Reasons.Application.Fallback.UnknownFailure))
                        if (!cancellationSignal.isCanceled) {
                            callback.onError(CreateCredentialUnknownException("Credential creation failed"))
                        }
                    }
                }
            }
        cancellationSignal.setOnCancelListener(job::cancel)
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, androidx.credentials.exceptions.ClearCredentialException>,
    ) {
        val job =
            launchAutofillServiceCleanup(
                scope = scope,
                cleanup = {
                    finishInteraction.expireAll(AutofillInteractionSource.CREDENTIAL_MANAGER)
                },
                afterCleanup = {
                    if (!cancellationSignal.isCanceled) callback.onResult(null)
                },
            )
        cancellationSignal.setOnCancelListener(job::cancel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
