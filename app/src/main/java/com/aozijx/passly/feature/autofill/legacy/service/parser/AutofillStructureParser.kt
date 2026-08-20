package com.aozijx.passly.feature.autofill.legacy.service.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.text.InputType
import android.view.autofill.AutofillId

/** Full static properties of an editable input field (for heuristic matching). */
data class EditableFieldInfo(
    val autofillId: AutofillId,
    val resourceId: String? = null,
    val inputType: String? = null,
    val hint: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val autofillHints: List<String> = emptyList(),
    /** Current focus state. */
    val isFocused: Boolean = false,
    /** Current value (used during SaveRequest). */
    val value: String? = null,
)

data class ParsedStructure(
    val packageName: String? = null,
    val webDomain: String? = null,
    val pageTitle: String? = null,
    /** All editable input fields on the page. */
    val editableFields: List<EditableFieldInfo> = emptyList(),
) {
    val allIds: List<AutofillId>
        get() = editableFields.map { it.autofillId }
}

object AutofillStructureParser {

    fun parse(contexts: List<FillContext>): ParsedStructure {
        val structure = contexts.lastOrNull()?.structure ?: return ParsedStructure()
        val parser = ParserImpl(structure)

        return ParsedStructure(
            packageName = parser.packageName,
            webDomain = parser.webDomain
                ?.takeUnless { it == "127.0.0.1" || it == "localhost" },
            pageTitle = parser.pageTitle,
            editableFields = parser.editableFields,
        )
    }

    private class ParserImpl(structure: AssistStructure) {
        var packageName: String? = structure.activityComponent?.packageName
        var webDomain: String? = null
        var pageTitle: String? = null
        val editableFields = mutableListOf<EditableFieldInfo>()

        init {
            val windowCount = structure.windowNodeCount
            for (i in 0 until windowCount) {
                val window = structure.getWindowNodeAt(i)
                if (pageTitle == null) pageTitle = window.title?.toString()
                parseNode(window.rootViewNode)
            }
        }

        private fun parseNode(node: AssistStructure.ViewNode) {
            if (packageName == null) {
                val idPkg = node.idPackage
                if (!idPkg.isNullOrBlank()) packageName = idPkg
            }

            if (webDomain == null) {
                val nodeDomain = node.webDomain
                if (!nodeDomain.isNullOrBlank()) webDomain = nodeDomain
            }

            val inputType = node.inputType
            val className = node.className?.lowercase() ?: ""
            val isEditableText = isEditableTextField(node, inputType, className)
            val autofillId = node.autofillId

            if (isEditableText && autofillId != null) {
                val value = node.autofillValue?.let {
                    if (it.isText) it.textValue.toString() else null
                } ?: node.text?.toString()

                editableFields.add(
                    EditableFieldInfo(
                        autofillId = autofillId,
                        resourceId = node.idEntry,
                        inputType = inputTypeNames(inputType),
                        hint = node.hint,
                        contentDescription = node.contentDescription?.toString(),
                        className = node.className,
                        autofillHints = node.autofillHints?.toList().orEmpty(),
                        isFocused = node.isFocused,
                        value = value
                    )
                )
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChildAt(i) ?: continue
                parseNode(child)
            }
        }

        private fun inputTypeNames(inputType: Int): String {
            if (inputType == InputType.TYPE_NULL) return "TYPE_NULL"
            val names = mutableListOf<String>()
            val cls = inputType and InputType.TYPE_MASK_CLASS
            when (cls) {
                InputType.TYPE_CLASS_TEXT -> names += "TYPE_CLASS_TEXT"
                InputType.TYPE_CLASS_NUMBER -> names += "TYPE_CLASS_NUMBER"
                InputType.TYPE_CLASS_PHONE -> names += "TYPE_CLASS_PHONE"
                InputType.TYPE_CLASS_DATETIME -> names += "TYPE_CLASS_DATETIME"
            }
            val varPart = inputType and InputType.TYPE_MASK_VARIATION
            when (varPart) {
                InputType.TYPE_TEXT_VARIATION_NORMAL -> names += "TEXT_VARIATION_NORMAL"
                InputType.TYPE_TEXT_VARIATION_PASSWORD -> names += "TEXT_VARIATION_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> names += "TEXT_VARIATION_VISIBLE_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> names += "TEXT_VARIATION_WEB_PASSWORD"
                InputType.TYPE_NUMBER_VARIATION_PASSWORD -> names += "NUMBER_VARIATION_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> names += "TEXT_VARIATION_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> names += "TEXT_VARIATION_PERSON_NAME"
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> names += "TEXT_VARIATION_WEB_EMAIL_ADDRESS"
                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> names += "TEXT_VARIATION_WEB_EDIT_TEXT"
            }
            return names.joinToString(" ")
        }

        private fun isEditableTextField(
            node: AssistStructure.ViewNode,
            inputType: Int,
            className: String
        ): Boolean {
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val isPwdType = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

            if (isPwdType) return true
            if (!node.autofillHints.isNullOrEmpty()) return true

            val cls = inputType and InputType.TYPE_MASK_CLASS
            if (cls == InputType.TYPE_CLASS_TEXT ||
                cls == InputType.TYPE_CLASS_NUMBER ||
                cls == InputType.TYPE_CLASS_PHONE ||
                cls == InputType.TYPE_CLASS_DATETIME
            ) return true

            if (className.contains("edittext") || className.contains("textinput")) return true

            if (className.contains("button")) return false

            val hasPrompt = !node.hint.isNullOrBlank() || !node.contentDescription.isNullOrBlank()
            return (node.isFocusable && hasPrompt) || (node.isFocused && className.contains("view"))
        }
    }
}
