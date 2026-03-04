package com.example.poop.ui.screens.vault.utils

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId

/**
 * 自动填充结构解析器 - 增强版 (支持 TOTP 识别)
 */
class AutofillParser(structure: AssistStructure) {

    var usernameId: AutofillId? = null
    var passwordId: AutofillId? = null
    var otpId: AutofillId? = null // 新增：2FA/OTP 字段 ID
    var packageName: String? = null
    var webDomain: String? = null

    init {
        val windowCount = structure.windowNodeCount
        for (i in 0 until windowCount) {
            parseNode(structure.getWindowNodeAt(i).rootViewNode)
        }
    }

    private fun parseNode(node: AssistStructure.ViewNode) {
        if (packageName == null) packageName = node.idPackage
        if (webDomain == null) webDomain = node.webDomain

        // 1. 优先通过系统 Hints 识别
        val hints = node.autofillHints
        if (!hints.isNullOrEmpty()) {
            if (hints.any { it.contains("username", true) || it.contains("email", true) }) {
                usernameId = node.autofillId
            } else if (hints.any { it.contains("password", true) }) {
                passwordId = node.autofillId
            } else if (hints.any { it.contains("otp", true) || it.contains("2fa", true) }) {
                otpId = node.autofillId
            }
        }

        // 2. 兜底逻辑：通过 Resource ID 或内容描述识别
        if (usernameId == null || passwordId == null || otpId == null) {
            val idRes = node.idEntry?.lowercase() ?: ""
            val hintText = node.hint?.lowercase() ?: ""

            if (usernameId == null && (idRes.contains("user") || idRes.contains("email") || hintText.contains("账号") || hintText.contains("邮箱"))) {
                usernameId = node.autofillId
            } else if (passwordId == null && (idRes.contains("password") || idRes.contains("pwd") || hintText.contains("密码"))) {
                passwordId = node.autofillId
            } else if (otpId == null && (idRes.contains("code") || idRes.contains("otp") || idRes.contains("token") || hintText.contains("验证码"))) {
                otpId = node.autofillId
            }
        }

        for (i in 0 until node.childCount) {
            parseNode(node.getChildAt(i))
        }
    }
}