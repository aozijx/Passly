package com.example.poop.ui.screens.vault.utils

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.autofill.AutofillId

/**
 * 自动填充结构解析器 - 增强版
 * 结合了 Autofill 原生提示与无障碍级别的模糊识别
 */
class AutofillParser(structure: AssistStructure) {

    var usernameId: AutofillId? = null
    var passwordId: AutofillId? = null
    var otpId: AutofillId? = null
    var submitId: AutofillId? = null // 关键：识别登录/提交按钮
    
    var packageName: String? = null
    var webDomain: String? = null
    var pageTitle: String? = null

    var usernameValue: String? = null
    var passwordValue: String? = null

    init {
        packageName = structure.activityComponent?.packageName
        val windowCount = structure.windowNodeCount
        for (i in 0 until windowCount) {
            val window = structure.getWindowNodeAt(i)
            if (pageTitle == null) pageTitle = window.title?.toString()
            parseNode(window.rootViewNode)
        }
    }

    private fun parseNode(node: AssistStructure.ViewNode) {
        if (packageName == null && !node.idPackage.isNullOrBlank()) packageName = node.idPackage
        if (webDomain == null && !node.webDomain.isNullOrBlank()) webDomain = node.webDomain

        // --- 1. 获取基础属性 ---
        val hints = node.autofillHints
        val idRes = node.idEntry?.lowercase() ?: ""
        val hintText = (node.hint ?: "").lowercase()
        val text = (node.text ?: "").toString().lowercase()
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.lowercase() ?: ""
        val isPwdType = isPasswordField(node)

        // --- 2. 识别逻辑：原生 Hints 优先 ---
        if (!hints.isNullOrEmpty()) {
            if (hints.any { it.contains("username", true) || it.contains("email", true) || it.contains("phone", true) }) {
                usernameId = node.autofillId
            } else if (hints.any { it.contains("password", true) }) {
                passwordId = node.autofillId
            }
        }

        // --- 3. 识别逻辑：模糊匹配 (模拟无障碍嗅探) ---
        // 账号匹配
        if (usernameId == null && (idRes.contains("user") || idRes.contains("email") || idRes.contains("phone") || idRes.contains("login") ||
            hintText.contains("账号") || hintText.contains("手机") || hintText.contains("邮箱") ||
            contentDesc.contains("账号") || contentDesc.contains("用户名"))) {
            if (!isPwdType) usernameId = node.autofillId
        }

        // 密码匹配
        if (passwordId == null && (isPwdType || idRes.contains("password") || idRes.contains("pwd") || 
            hintText.contains("密码") || contentDesc.contains("密码"))) {
            passwordId = node.autofillId
        }

        // 提交按钮匹配 (关键：用于触发保存)
        if (submitId == null && (className.contains("button") || className.contains("viewgroup") || node.isClickable)) {
            if (idRes.contains("login") || idRes.contains("submit") || idRes.contains("signin") || idRes.contains("confirm") ||
                text.contains("登录") || text.contains("进入") || text.contains("确定") || text.contains("提交") ||
                contentDesc.contains("登录") || contentDesc.contains("提交")) {
                submitId = node.autofillId
            }
        }

        // --- 4. 抓取值 (SaveRequest 时使用) ---
        val nodeValue = node.autofillValue?.textValue?.toString() ?: node.text?.toString()
        if (!nodeValue.isNullOrBlank()) {
            if (node.autofillId == usernameId) usernameValue = nodeValue
            else if (node.autofillId == passwordId) passwordValue = nodeValue
        }

        // 递归
        for (i in 0 until node.childCount) {
            parseNode(node.getChildAt(i))
        }
    }

    private fun isPasswordField(node: AssistStructure.ViewNode): Boolean {
        val inputType = node.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
               variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
               variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
               variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }
}
