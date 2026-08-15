package com.aozijx.passly.feature.autofill.framework.service.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.text.InputType
import android.view.autofill.AutofillId

data class ParsedStructure(
    val usernameId: AutofillId? = null,
    val passwordId: AutofillId? = null,
    val otpId: AutofillId? = null,
    val submitId: AutofillId? = null,
    val usernameResourceId: String? = null,
    val passwordResourceId: String? = null,
    val otpResourceId: String? = null,
    val packageName: String? = null,
    val webDomain: String? = null,
    val pageTitle: String? = null,
    val usernameValue: String? = null,
    val passwordValue: String? = null,
) {
    val normalizedPackageName: String?
        get() = packageName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val normalizedWebDomain: String?
        get() = webDomain?.trim()?.lowercase()
            ?.removePrefix("https://")?.removePrefix("http://")
            ?.substringBefore('/')?.removePrefix("www.")
            ?.takeIf(String::isNotBlank)

    val allIds: List<AutofillId>
        get() = listOfNotNull(usernameId, passwordId, otpId, submitId)
}

object AutofillStructureParser {

    fun parse(contexts: List<FillContext>): ParsedStructure {
        val structure = contexts.lastOrNull()?.structure ?: return ParsedStructure()
        val parser = ParserImpl(structure)
        return ParsedStructure(
            usernameId = parser.usernameId,
            passwordId = parser.passwordId,
            otpId = parser.otpId,
            submitId = parser.submitId,
            usernameResourceId = parser.usernameResourceId,
            passwordResourceId = parser.passwordResourceId,
            otpResourceId = parser.otpResourceId,
            packageName = parser.packageName,
            webDomain = parser.webDomain,
            pageTitle = parser.pageTitle,
            usernameValue = parser.usernameValue,
            passwordValue = parser.passwordValue,
        )
    }

    private class ParserImpl(structure: AssistStructure) {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var otpId: AutofillId? = null
        var submitId: AutofillId? = null
        var usernameResourceId: String? = null
        var passwordResourceId: String? = null
        var otpResourceId: String? = null

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

            val hints = node.autofillHints
            val idRes = node.idEntry?.lowercase() ?: ""
            val hintText = (node.hint ?: "").lowercase()
            val text = (node.text ?: "").toString().lowercase()
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val className = node.className?.lowercase() ?: ""
            val isPwdType = isPasswordField(node)

            if (!hints.isNullOrEmpty()) {
                if (hints.any {
                        it.contains("username", true) || it.contains(
                            "email",
                            true
                        ) || it.contains("phone", true)
                    }) {
                    usernameId = node.autofillId
                    usernameResourceId = node.idEntry
                } else if (hints.any { it.contains("password", true) }) {
                    passwordId = node.autofillId
                    passwordResourceId = node.idEntry
                }
            }

            if (usernameId == null && (idRes.contains("user") || idRes.contains("email") || idRes.contains(
                    "phone"
                ) || idRes.contains("login") ||
                        idRes.contains("account") || idRes.contains("acct") ||
                        hintText.contains("账号") || hintText.contains("账户") || hintText.contains(
                    "手机"
                ) || hintText.contains("邮箱") ||
                        hintText.contains("account") || hintText.contains("email") || hintText.contains(
                    "phone"
                ) ||
                        contentDesc.contains("账号") || contentDesc.contains("用户名") || contentDesc.contains(
                    "账户"
                ) ||
                        contentDesc.contains("account") || contentDesc.contains("email"))
            ) {
                if (!isPwdType) {
                    usernameId = node.autofillId
                    usernameResourceId = node.idEntry
                }
            }

            if (passwordId == null && (isPwdType || idRes.contains("password") || idRes.contains("pwd") ||
                        hintText.contains("密码") || contentDesc.contains("密码"))
            ) {
                passwordId = node.autofillId
                passwordResourceId = node.idEntry
            }

            if (submitId == null && (className.contains("button") || className.contains("viewgroup") || node.isClickable)) {
                if (idRes.contains("login") || idRes.contains("submit") || idRes.contains("signin") || idRes.contains(
                        "confirm"
                    ) ||
                    text.contains("登录") || text.contains("进入") || text.contains("确定") || text.contains(
                        "提交"
                    ) ||
                    contentDesc.contains("登录") || contentDesc.contains("提交")
                ) {
                    submitId = node.autofillId
                }
            }

            val nodeValue =
                node.autofillValue?.let { if (it.isText) it.textValue.toString() else null }
                    ?: node.text?.toString()
            if (!nodeValue.isNullOrBlank()) {
                if (node.autofillId == usernameId) usernameValue = nodeValue
                else if (node.autofillId == passwordId) passwordValue = nodeValue
            }

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
}
