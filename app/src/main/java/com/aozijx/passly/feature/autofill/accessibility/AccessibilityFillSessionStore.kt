package com.aozijx.passly.feature.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import com.aozijx.passly.feature.autofill.core.AutofillInputTypeClassifier
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AccessibilityCapture {
    data class Ready(val token: String, val request: AutofillRequest) : AccessibilityCapture
    data object Unavailable : AccessibilityCapture
}

@Singleton
class AccessibilityFillSessionStore @Inject constructor() {
    private class Session(
        val token: String,
        val request: AutofillRequest,
        val windowId: Int,
        val capturedAtMs: Long,
        val nodes: Map<String, AccessibilityNodeInfo>,
    )

    private sealed interface Stored {
        data object Empty : Stored
        class Captured(val session: Session) : Stored
        class Pending(
            val session: Session,
            val roles: Map<String, FieldRole>,
            val payload: AutofillCredentialPayload,
            val receipt: AutofillSelectionReceipt,
        ) : Stored
    }

    class DeliveryClaim internal constructor(
        val receipt: AutofillSelectionReceipt,
        val targetPackage: String,
        private val commitAction: () -> Boolean,
    ) {
        fun commit(): Boolean = commitAction()
    }

    private var service = WeakReference<AccessibilityService>(null)
    private val expiryHandler = Handler(Looper.getMainLooper())
    private var stored: Stored = Stored.Empty
    private var onDeliveryExpired: ((AutofillSelectionReceipt) -> Unit)? = null

    @Synchronized
    fun bind(
        service: AccessibilityService,
        onDeliveryExpired: (AutofillSelectionReceipt) -> Unit,
    ) {
        this.service = WeakReference(service)
        this.onDeliveryExpired = onDeliveryExpired
    }

    @Synchronized
    fun unbind() {
        service.clear()
        stored = Stored.Empty
        onDeliveryExpired = null
        expiryHandler.removeCallbacksAndMessages(null)
    }

    @Synchronized
    fun capture(): AccessibilityCapture {
        if (stored != Stored.Empty) return AccessibilityCapture.Unavailable
        val activeService = service.get() ?: return AccessibilityCapture.Unavailable
        val root = activeService.rootInActiveWindow ?: return AccessibilityCapture.Unavailable
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return AccessibilityCapture.Unavailable
        val targetPackage = focused.packageName?.toString().orEmpty()
        if (!focused.isEditable || !focused.isVisibleToUser || targetPackage.isBlank() ||
            targetPackage == activeService.packageName ||
            targetPackage == "android" || targetPackage == "com.android.systemui" ||
            root.packageName?.toString() != targetPackage
        ) return AccessibilityCapture.Unavailable

        val nodes = linkedMapOf<String, AccessibilityNodeInfo>()
        val fields = mutableListOf<AutofillField>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES && fields.size < MAX_FIELDS) {
            val node = queue.removeFirst()
            if (node.packageName?.toString() == targetPackage &&
                node.isEditable && node.isVisibleToUser
            ) {
                val id = "field-${fields.size}"
                nodes[id] = node
                fields += AutofillField(
                    id = id,
                    hints = emptySet(),
                    inputType = AutofillInputTypeClassifier.describe(node.inputType),
                    isFocused = node.isFocused,
                    hint = node.hintText?.toString(),
                    resourceId = node.viewIdResourceName,
                    contentDescription = node.contentDescription?.toString(),
                    className = node.className?.toString(),
                )
            }
            for (index in 0 until node.childCount) {
                if (visited + queue.size >= MAX_NODES) break
                node.getChild(index)?.let(queue::addLast)
            }
        }
        if (fields.isEmpty()) return AccessibilityCapture.Unavailable
        val token = AutofillInteractionId.new().value
        val request = AutofillRequest(targetPackage, null, fields, AutofillSource.ACCESSIBILITY)
        stored = Stored.Captured(
            Session(token, request, focused.windowId, SystemClock.elapsedRealtime(), nodes),
        )
        expiryHandler.postDelayed({ clearCaptured(token) }, MAX_AGE_MS)
        return AccessibilityCapture.Ready(token, request)
    }

    @Synchronized
    fun pendingCapture(): AccessibilityCapture =
        (stored as? Stored.Captured)?.session
            ?.takeIf { SystemClock.elapsedRealtime() - it.capturedAtMs <= MAX_AGE_MS }
            ?.let { AccessibilityCapture.Ready(it.token, it.request) }
            ?: AccessibilityCapture.Unavailable

    @Synchronized
    fun prepareDelivery(
        token: String,
        roles: Map<String, FieldRole>,
        payload: AutofillCredentialPayload,
        receipt: AutofillSelectionReceipt,
    ): Boolean {
        val captured = (stored as? Stored.Captured)?.session
            ?.takeIf { it.token == token } ?: return false
        val remainingMs = MAX_AGE_MS - (SystemClock.elapsedRealtime() - captured.capturedAtMs)
        if (remainingMs <= 0) return false
        stored = Stored.Pending(captured, roles, payload, receipt)
        expiryHandler.postDelayed(
            { expirePending(token) },
            remainingMs.coerceAtMost(DELIVERY_WAIT_MS),
        )
        return true
    }

    @Synchronized
    fun claimWhenTargetActive(): DeliveryClaim? {
        val pending = stored as? Stored.Pending ?: return null
        val activeService = service.get() ?: return null
        val root = activeService.rootInActiveWindow ?: return null
        val captured = pending.session
        if (!AccessibilityTargetPolicy.canFill(
                captured.request.packageName,
                captured.windowId,
                captured.capturedAtMs,
                root.packageName?.toString().orEmpty(),
                root.windowId,
                SystemClock.elapsedRealtime(),
            )
        ) return null
        stored = Stored.Empty
        return DeliveryClaim(pending.receipt, captured.request.packageName) { fill(pending) }
    }

    private fun fill(pending: Stored.Pending): Boolean {
        val captured = pending.session
        val activeService = service.get() ?: return false
        val root = activeService.rootInActiveWindow ?: return false
        if (!AccessibilityTargetPolicy.canFill(
                captured.request.packageName,
                captured.windowId,
                captured.capturedAtMs,
                root.packageName?.toString().orEmpty(),
                root.windowId,
                SystemClock.elapsedRealtime(),
            )
        ) return false

        val actions = pending.roles.mapNotNull { (id, role) ->
            val value = when (role) {
                FieldRole.USERNAME -> pending.payload.username
                FieldRole.PASSWORD -> pending.payload.password
                FieldRole.OTP -> pending.payload.otpCode
                FieldRole.SUBMIT, FieldRole.UNKNOWN -> null
            }?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val node = captured.nodes[id] ?: return@mapNotNull null
            node to value
        }
        if (actions.isEmpty() || actions.any { (node, _) ->
                !node.refresh() || !node.isEditable ||
                    node.packageName?.toString() != captured.request.packageName ||
                    node.windowId != captured.windowId ||
                    node.actionList.none { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
            }
        ) return false

        return actions.all { (node, value) ->
            if (!node.refresh() || node.packageName?.toString() != captured.request.packageName ||
                node.windowId != captured.windowId || !node.isEditable
            ) return@all false
            // The platform requires this one transient Bundle for ACTION_SET_TEXT.
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            try {
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } finally {
                arguments.clear()
            }
        }
    }

    @Synchronized
    fun clearCaptured(token: String) {
        val captured = stored as? Stored.Captured ?: return
        if (captured.session.token == token) stored = Stored.Empty
    }

    @Synchronized
    fun abortPending(token: String) {
        val pending = stored as? Stored.Pending ?: return
        if (pending.session.token == token) stored = Stored.Empty
    }

    @Synchronized
    private fun expirePending(token: String) {
        val pending = stored as? Stored.Pending ?: return
        if (pending.session.token != token) return
        stored = Stored.Empty
        onDeliveryExpired?.invoke(pending.receipt)
    }

    private companion object {
        const val MAX_NODES = 256
        const val MAX_FIELDS = 32
        const val MAX_AGE_MS = 30_000L
        const val DELIVERY_WAIT_MS = 10_000L
    }
}
