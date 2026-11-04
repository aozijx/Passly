package com.aozijx.passly.core.observability

/** Platform-free summaries passed to Autofill log statements. */
object AutofillLogSnapshot {
    data class Classification(
        val fieldCount: Int,
        val roles: Roles,
    ) {
        val hasCredentials: Boolean
            get() = roles.username > 0 || roles.password > 0 || roles.otp > 0

        data class Roles(
            val username: Int,
            val password: Int,
            val otp: Int,
        )
    }

    object Legacy {
        data class FillRequest(
            val fillContextCount: Int,
            val inlineRequestAvailable: Boolean,
            val requestFlags: Int,
        )

        data class Structure(
            val stage: Stage,
            val fillContextCount: Int,
            val traversal: Traversal,
            val fields: Fields,
            val target: Target,
        ) {
            enum class Stage {
                FILL,
                SAVE,
            }

            data class Traversal(
                val windowCount: Int,
                val visitedNodeCount: Int,
                val truncated: Boolean,
            )

            data class Fields(
                val editableCount: Int,
                val hintedCount: Int,
                val focusedCount: Int,
                val passwordInputTypeCount: Int,
            )

            data class Target(
                val hasPackage: Boolean,
                val hasWebDomain: Boolean,
            )
        }

        data class Response(
            val present: Boolean,
            val datasetCount: Int,
            val hasSaveInfo: Boolean,
        )
    }
}
