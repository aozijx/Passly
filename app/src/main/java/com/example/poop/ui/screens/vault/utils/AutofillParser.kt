package com.example.poop.ui.screens.vault.utils

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId

/**
 * 自动填充结构解析器 - 增强版
 * 优化了包名 (PackageName) 和 域名 (WebDomain) 的抓取逻辑
 */
class AutofillParser(structure: AssistStructure) {

    var usernameId: AutofillId? = null
    var passwordId: AutofillId? = null
    var packageName: String? = null
    var webDomain: String? = null
    
    var pageTitle: String? = null

    // 获取输入值（SaveRequest 时使用）
    var usernameValue: String? = null
    var passwordValue: String? = null

    init {
        // 1. 优先从 ActivityComponent 获取包名，这是最可靠的来源
        packageName = structure.activityComponent?.packageName

        val windowCount = structure.windowNodeCount
        for (i in 0 until windowCount) {
            val window = structure.getWindowNodeAt(i)
            
            // 记录页面标题
            if (pageTitle == null) {
                pageTitle = window.title?.toString()
            }
            
            parseNode(window.rootViewNode)
        }
    }

    private fun parseNode(node: AssistStructure.ViewNode) {
        // 2. 如果包名依然为空（极少数情况），尝试从节点 ID 属性中提取
        if (packageName == null && !node.idPackage.isNullOrBlank()) {
            packageName = node.idPackage
        }
        
        // 3. 抓取 Web 域名（通常存在于 WebView 相关的节点上）
        if (webDomain == null && !node.webDomain.isNullOrBlank()) {
            webDomain = node.webDomain
        }

        // --- 识别逻辑 ---
        
        // A. 通过 AutofillHints 识别
        val hints = node.autofillHints
        if (!hints.isNullOrEmpty()) {
            if (hints.any { it.contains("username", true) || it.contains("email", true) }) {
                usernameId = node.autofillId
            } else if (hints.any { it.contains("password", true) }) {
                passwordId = node.autofillId
            }
        }

        // B. 通过资源 ID 或 Hint 文本模糊识别
        if (usernameId == null || passwordId == null) {
            val idRes = node.idEntry?.lowercase() ?: ""
            val hintText = node.hint?.lowercase() ?: ""
            if (usernameId == null && (idRes.contains("user") || idRes.contains("email") || hintText.contains("账号") || hintText.contains("手机"))) {
                usernameId = node.autofillId
            } else if (passwordId == null && (idRes.contains("password") || idRes.contains("pwd") || hintText.contains("密码"))) {
                passwordId = node.autofillId
            }
        }

        // --- 抓取输入值 (SaveRequest 专用) ---
        // 关键点：值可能存在于 autofillValue.textValue 或直接在 text 属性中
        val nodeValue = node.autofillValue?.textValue?.toString() ?: node.text?.toString()
        
        if (node.autofillId == usernameId) {
            usernameValue = nodeValue
        } else if (node.autofillId == passwordId) {
            passwordValue = nodeValue
        }

        // 递归遍历子节点
        for (i in 0 until node.childCount) {
            parseNode(node.getChildAt(i))
        }
    }
}
