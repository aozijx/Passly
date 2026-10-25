package com.aozijx.passly.feature.autofill.legacy.service.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.text.InputType
import android.view.autofill.AutofillId
import com.aozijx.passly.feature.autofill.core.AutofillInputTypeClassifier

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
    /** Current text used by field-role matching. */
    val value: String? = null,
)

data class ParsedStructure(
    val packageName: String? = null,
    val webDomain: String? = null,
    /** All editable input fields on the page. */
    val editableFields: List<EditableFieldInfo> = emptyList(),
) {
    val allIds: List<AutofillId>
        get() = editableFields.map { it.autofillId }
}

object AutofillStructureParser {

    private const val MAX_TREE_DEPTH = 64
    private const val MAX_NODE_COUNT = 10_000

    fun parse(contexts: List<FillContext>): ParsedStructure {
        val structure = contexts.lastOrNull()?.structure ?: return ParsedStructure()
        val parser = ParserImpl(structure)

        return ParsedStructure(
            packageName = parser.packageName,
            webDomain = parser.webDomain
                ?.takeUnless { it == "127.0.0.1" || it == "localhost" },
            editableFields = parser.editableFields,
        )
    }

    private class ParserImpl(structure: AssistStructure) {
        var packageName: String? = structure.activityComponent?.packageName
        var webDomain: String? = null
        val editableFields = mutableListOf<EditableFieldInfo>()

        init {
            val roots = buildList {
                for (index in 0 until structure.windowNodeCount) {
                    add(structure.getWindowNodeAt(index).rootViewNode)
                }
            }
            traverseBoundedPreOrder(
                roots = roots,
                maxDepth = MAX_TREE_DEPTH,
                maxNodes = MAX_NODE_COUNT,
                children = ::childrenOf,
                visit = ::parseNode,
            )
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

        }

        private fun childrenOf(node: AssistStructure.ViewNode): List<AssistStructure.ViewNode> =
            buildList(node.childCount) {
                for (index in 0 until node.childCount) {
                    node.getChildAt(index)?.let(::add)
                }
            }

        private fun inputTypeNames(inputType: Int): String =
            AutofillInputTypeClassifier.describe(inputType)

        private fun isEditableTextField(
            node: AssistStructure.ViewNode,
            inputType: Int,
            className: String
        ): Boolean {
            val isPwdType = AutofillInputTypeClassifier.isPassword(inputType)

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
