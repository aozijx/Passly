package com.aozijx.passly.core.autofill.model

/**
 * 字段描述符：描述视图树中单个字段的静态属性。
 *
 * 核心层不持 Android 具体类型（AutofillId/AssistStructure），
 * 所有字段值都以纯 Kotlin 类型保存。
 */
data class FieldDescriptor(
    /** ViewNode 的 AutofillId（由适配器层转换为字符串保存） */
    val viewId: String,
    /** Android autofill hints 常量名列表（如 ["PASSWORD", "USERNAME"]） */
    val autofillHints: List<String> = emptyList(),
    /** View 的 android:id 资源名（如 "@id/edit_text_password"） */
    val resourceId: String? = null,
    /** inputType 整数值的字符串表示（核心层以字符串形式保存，避免引用系统常量） */
    val inputType: String? = null,
    /** 输入框 hint 文本 */
    val hint: String? = null,
    /** ContentDescription 文本 */
    val contentDescription: String? = null,
    /** Class name（如 "android.widget.EditText"） */
    val className: String? = null,
)
