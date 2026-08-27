package com.aozijx.passly.domain.autofill.port

/** Resolves an optional display label without exposing platform package-manager types. */
fun interface ApplicationLabelResolver {
    fun labelFor(packageName: String): String?
}
