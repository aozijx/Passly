package com.aozijx.passly.presentation.feature.autofill.credential

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.feature.autofill.credential.CredentialAuthenticationExceptionMapper
import com.aozijx.passly.feature.autofill.credential.CredentialManagerCreateResult
import com.aozijx.passly.feature.autofill.credential.CredentialManagerFlow
import com.aozijx.passly.feature.autofill.credential.CredentialManagerPasswordResult
import com.aozijx.passly.feature.autofill.credential.CredentialRequestParser
import com.aozijx.passly.feature.autofill.credential.PasswordCreateParseResult
import com.aozijx.passly.feature.autofill.credential.PasswordGetParseResult
import com.aozijx.passly.feature.autofill.credential.UnlockParseResult
import com.aozijx.passly.feature.autofill.credential.service.CredentialBeginGetHandler
import com.aozijx.passly.feature.autofill.credential.service.CredentialResponseFactory
import com.aozijx.passly.feature.autofill.credential.service.CredentialUnlockResponse
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialResponseViewModel @Inject constructor(
    private val flow: CredentialManagerFlow,
    private val beginGetHandler: CredentialBeginGetHandler,
    private val logger: Logger,
    private val traceContinuations: TraceContinuationStore,
    @param:ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow<CredentialResponseUiState>(
        CredentialResponseUiState.Loading,
    )
    val state: StateFlow<CredentialResponseUiState> = mutableState.asStateFlow()

    private val requestStarted = AtomicBoolean(false)
    private var activeInteractionId: AutofillInteractionId? = null

    fun onAction(action: CredentialResponseUiAction) {
        when (action) {
            is CredentialResponseUiAction.PasswordGet -> handlePasswordGet(action.sourceIntent)
            is CredentialResponseUiAction.Unlock -> handleUnlock(action.sourceIntent)
            is CredentialResponseUiAction.PasswordCreate -> handlePasswordCreate(action.sourceIntent)
            CredentialResponseUiAction.UnknownAction -> rejectUnknownAction()
        }
    }

    fun cancelActiveInteraction() {
        val interactionId = activeInteractionId ?: return
        activeInteractionId = null
        viewModelScope.launch {
            withContext(NonCancellable) {
                flow.cancel(interactionId)
                traceContinuations.discard(interactionId.value)
            }
        }
    }

    private fun handlePasswordGet(sourceIntent: Intent) = launchOnce(
        sourceIntent = sourceIntent,
        failureIntent = {
            CredentialResponseFactory.buildGetException(
                GetCredentialUnknownException("Credential resolution failed"),
            )
        },
    ) {
        when (val request = CredentialRequestParser.parsePasswordGet(sourceIntent)) {
            is PasswordGetParseResult.Failed -> {
                cancelTrackedInteraction()
                completeGetError(request.exception)
            }
            is PasswordGetParseResult.Ready -> {
                activeInteractionId = request.interactionId
                val authenticationStartedAt = monotonicTimeMs()
                logger.log(LogStatements.Autofill.authenticationStarted())
                when (
                    val result = flow.selectPassword(
                        interactionId = request.interactionId,
                        scope = AutofillGrantContext(request.packageName, null),
                        entryId = request.entryId,
                        allowedUserIds = request.option.allowedUserIds,
                    )
                ) {
                    is CredentialManagerPasswordResult.Success -> {
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.authenticationCompleted(
                                monotonicTimeMs() - authenticationStartedAt,
                            ),
                        )
                        traceContinuations.capture(request.interactionId.value)
                        completeCommittedSelection(
                            CredentialResponseFactory.buildPasswordResponse(
                                result.username,
                                result.password,
                            ),
                            receipt = result.receipt,
                        )
                    }

                    CredentialManagerPasswordResult.NotAvailable -> {
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry),
                        )
                        traceContinuations.discard(request.interactionId.value)
                        completeGetError(NoCredentialException("Credential is no longer available"))
                    }
                }
            }
        }
    }

    private fun handleUnlock(sourceIntent: Intent) = launchOnce(
        sourceIntent = sourceIntent,
        failureIntent = {
            CredentialResponseFactory.buildGetException(
                GetCredentialUnknownException("Credential unlock failed"),
            )
        },
    ) {
        when (val request = CredentialRequestParser.parseUnlock(sourceIntent)) {
            is UnlockParseResult.Failed -> {
                cancelTrackedInteraction()
                completeGetError(request.exception)
            }
            is UnlockParseResult.Ready -> {
                activeInteractionId = request.interactionId
                val authenticationStartedAt = monotonicTimeMs()
                logger.log(LogStatements.Autofill.authenticationStarted())
                when (
                    val result = beginGetHandler.resolveAfterUnlock(
                        request = request.request,
                        interactionId = request.interactionId,
                        context = appContext,
                    )
                ) {
                    is CredentialUnlockResponse.Ready -> {
                        val intent = Intent()
                        PendingIntentHandler.setBeginGetCredentialResponse(intent, result.response)
                        // The same interaction remains active for the picker selection.
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.authenticationCompleted(
                                monotonicTimeMs() - authenticationStartedAt,
                            ),
                        )
                        traceContinuations.capture(request.interactionId.value)
                        completeImmediately(intent)
                    }

                    is CredentialUnlockResponse.NotAuthorized -> {
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.discoveryUnavailable(
                                Reasons.AuthenticationRejected,
                            ),
                        )
                        traceContinuations.discard(request.interactionId.value)
                        completeGetError(
                            CredentialAuthenticationExceptionMapper.toGetException(
                                result.result.authorization,
                            ),
                        )
                    }

                    CredentialUnlockResponse.Unavailable -> {
                        flow.cancel(request.interactionId)
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry),
                        )
                        traceContinuations.discard(request.interactionId.value)
                        completeGetError(NoCredentialException("Credential is no longer available"))
                    }
                }
            }
        }
    }

    private fun handlePasswordCreate(sourceIntent: Intent) = launchOnce(
        sourceIntent = sourceIntent,
        failureIntent = {
            CredentialResponseFactory.buildCreateException(
                CreateCredentialUnknownException("Credential creation failed"),
            )
        },
    ) {
        when (val request = CredentialRequestParser.parsePasswordCreate(sourceIntent)) {
            is PasswordCreateParseResult.Failed -> {
                cancelTrackedInteraction()
                completeCreateError(request.exception)
            }
            is PasswordCreateParseResult.Ready -> {
                activeInteractionId = request.interactionId
                val authenticationStartedAt = monotonicTimeMs()
                logger.log(LogStatements.Autofill.authenticationStarted())
                when (
                    val result = flow.createPassword(
                        interactionId = request.interactionId,
                        scope = AutofillGrantContext(request.packageName, null),
                        username = request.username,
                        password = request.password,
                    )
                ) {
                    CredentialManagerCreateResult.Saved -> {
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.authenticationCompleted(
                                monotonicTimeMs() - authenticationStartedAt,
                            ),
                        )
                        logger.log(LogStatements.Autofill.saveCompleted())
                        traceContinuations.discard(request.interactionId.value)
                        completeImmediately(CredentialResponseFactory.buildPasswordCreateResponse())
                    }

                    CredentialManagerCreateResult.NotSaved -> {
                        activeInteractionId = null
                        logger.log(LogStatements.Autofill.saveUnavailable(Reasons.StorageFailure))
                        traceContinuations.discard(request.interactionId.value)
                        completeCreateError(CreateCredentialUnknownException("Credential was not saved"))
                    }

                    is CredentialManagerCreateResult.NotAuthorized -> {
                        activeInteractionId = null
                        logger.log(
                            LogStatements.Autofill.saveUnavailable(Reasons.AuthenticationRejected),
                        )
                        traceContinuations.discard(request.interactionId.value)
                        completeCreateError(
                            CredentialAuthenticationExceptionMapper.toCreateException(
                                result.authorization,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun launchOnce(
        sourceIntent: Intent,
        failureIntent: (Exception) -> Intent,
        block: suspend () -> Unit,
    ) {
        if (!requestStarted.compareAndSet(false, true)) return
        activeInteractionId = CredentialRequestParser.parseInteractionId(sourceIntent)
        viewModelScope.launch {
            continueTrace(activeInteractionId) {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.discoveryFailed(error))
                    cancelTrackedInteraction()
                    completeImmediately(failureIntent(error))
                }
            }
        }
    }

    private fun rejectUnknownAction() {
        if (requestStarted.compareAndSet(false, true)) {
            mutableState.value = CredentialResponseReducer.reduce(
                mutableState.value,
                CredentialResponseMutation.Unrecoverable,
            )
        }
    }

    private fun completeImmediately(
        resultIntent: Intent,
    ) {
        mutableState.value = CredentialResponseReducer.reduce(
            mutableState.value,
            CredentialResponseMutation.Completed(
                CredentialResponseCompletion.Immediate(resultIntent),
            ),
        )
    }

    private fun completeCommittedSelection(
        resultIntent: Intent,
        receipt: AutofillSelectionReceipt,
    ) {
        mutableState.value = CredentialResponseReducer.reduce(
            mutableState.value,
            CredentialResponseMutation.Completed(
                CredentialResponseCompletion.CommittedSelection(resultIntent, receipt),
            ),
        )
    }

    private fun completeGetError(exception: GetCredentialException) {
        completeImmediately(CredentialResponseFactory.buildGetException(exception))
    }

    private fun completeCreateError(exception: CreateCredentialException) {
        completeImmediately(CredentialResponseFactory.buildCreateException(exception))
    }

    private suspend fun cancelTrackedInteraction() {
        val interactionId = activeInteractionId ?: return
        activeInteractionId = null
        flow.cancel(interactionId)
        traceContinuations.discard(interactionId.value)
    }

    private suspend fun <T> continueTrace(
        interactionId: AutofillInteractionId?,
        block: suspend com.aozijx.passly.core.observability.TraceScope.() -> T,
    ): T {
        if (interactionId != null) {
            return traceContinuations.continueOrRoot(
                interactionId.value,
                Operations.AutofillCredentialRequest,
                block,
            )
        }
        return logger.withRootTrace(Operations.AutofillCredentialRequest) {
            block()
        }
    }
}
