package com.aozijx.passly.feature.autofill.accessibility

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import com.aozijx.passly.R
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ManualAccessibilityFillService : AccessibilityService() {
    @Inject lateinit var sessions: AccessibilityFillSessionStore
    @Inject lateinit var finishInteraction: FinishAutofillInteraction
    @Inject lateinit var logger: Logger
    @Inject lateinit var traceContinuations: TraceContinuationStore
    @Inject lateinit var resultDelivery: AutofillResultDelivery
    @Inject lateinit var launchTarget: AutofillLaunchTarget

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var floatingButton: View? = null
    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) = requestFill()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessions.bind(this, ::onDeliveryExpired)
        accessibilityButtonController.registerAccessibilityButtonCallback(buttonCallback)
        val button = Button(this).apply {
            text = getString(R.string.accessibility_fill_button)
            contentDescription = getString(R.string.accessibility_fill_button_description)
            setOnClickListener { requestFill() }
        }
        val size = (56 * resources.displayMetrics.density).toInt()
        val parameters = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        try {
            getSystemService(WindowManager::class.java).addView(button, parameters)
            floatingButton = button
        } catch (error: Exception) {
            scope.launch {
                logger.withRootTrace(Operations.AutofillAccessibilityRequest) {
                    logger.log(LogStatements.Autofill.fallbackFailed(error))
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        floatingButton?.visibility = if (rootInActiveWindow?.packageName?.toString() == packageName) {
            View.GONE
        } else {
            View.VISIBLE
        }
        val claim = sessions.claimWhenTargetActive() ?: return
        scope.launch {
            traceContinuations.continueOrRoot(
                claim.receipt.interactionId.value,
                Operations.AutofillAccessibilityRequest,
            ) {
                val fields = LogStatements.Autofill.context(
                    AutofillInteractionSource.ACCESSIBILITY,
                    claim.targetPackage,
                )
                try {
                    resultDelivery.commitResult(claim.receipt) {
                        check(claim.commit()) { "Accessibility target is unavailable" }
                    }
                    logger.log(LogStatements.Autofill.fallbackDelivered(fields))
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) { finishInteraction.cancel(claim.receipt.interactionId) }
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.fallbackFailed(error, fields))
                    finishInteraction.cancel(claim.receipt.interactionId)
                    traceContinuations.discard(claim.receipt.interactionId.value)
                }
            }
        }
    }

    private fun onDeliveryExpired(receipt: AutofillSelectionReceipt) {
        scope.launch {
            traceContinuations.continueOrRoot(
                receipt.interactionId.value,
                Operations.AutofillAccessibilityRequest,
            ) {
                logger.log(LogStatements.Autofill.fallbackUnavailable(Reasons.PlatformCallbackFailed))
                finishInteraction.cancel(receipt.interactionId)
                traceContinuations.discard(receipt.interactionId.value)
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun requestFill() {
        when (val captured = sessions.capture()) {
            AccessibilityCapture.Unavailable -> scope.launch {
                logger.withRootTrace(Operations.AutofillAccessibilityRequest) {
                    logger.log(LogStatements.Autofill.fallbackUnavailable(Reasons.NoFillableFields))
                }
            }
            is AccessibilityCapture.Ready -> {
                launchAccessibilityFill(
                    scope = scope,
                    token = captured.token,
                    discardTrace = traceContinuations::discard,
                    clearCaptured = sessions::clearCaptured,
                    onFailure = { error ->
                        logger.withRootTrace(Operations.AutofillAccessibilityRequest) {
                            logger.log(LogStatements.Autofill.fallbackFailed(error))
                        }
                    },
                ) {
                    logger.withRootTrace(Operations.AutofillAccessibilityRequest) {
                        logger.log(LogStatements.Autofill.fallbackStarted(
                            LogStatements.Autofill.context(
                                AutofillInteractionSource.ACCESSIBILITY,
                                captured.request.packageName,
                            ),
                        ))
                        traceContinuations.capture(captured.token)
                        withContext(Dispatchers.Main) {
                            startActivity(launchTarget.accessibilityFillIntent(
                                this@ManualAccessibilityFillService,
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        floatingButton?.let { button ->
            try {
                getSystemService(WindowManager::class.java).removeView(button)
            } catch (error: Exception) {
                runBlocking {
                    logger.withRootTrace(Operations.AutofillAccessibilityRequest) {
                        logger.log(LogStatements.Autofill.fallbackFailed(error))
                    }
                }
            }
        }
        floatingButton = null
        sessions.unbind()
        runBlocking { finishInteraction.expireAll(AutofillInteractionSource.ACCESSIBILITY) }
        scope.cancel()
        super.onDestroy()
    }
}
