package com.aozijx.passly.domain.autofill.port

/** Resolves an optional display label without exposing platform package-manager types. */
fun interface ApplicationLabelResolver {
    /** Implementations keep blocking platform lookups off the caller's dispatcher. */
    suspend fun labelFor(packageName: String): String?
}
