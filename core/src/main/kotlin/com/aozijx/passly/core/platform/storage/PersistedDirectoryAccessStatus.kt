package com.aozijx.passly.core.platform.storage

enum class PersistedDirectoryAccessStatus {
    NOT_CONFIGURED,
    GRANTED,
    GRANT_MISSING,
    UNAVAILABLE
}

fun resolvePersistedDirectoryAccess(
    configured: Boolean,
    uriValid: Boolean,
    hasWriteGrant: Boolean,
    providerAvailable: Boolean,
): PersistedDirectoryAccessStatus = when {
    !configured -> PersistedDirectoryAccessStatus.NOT_CONFIGURED
    !uriValid || !providerAvailable -> PersistedDirectoryAccessStatus.UNAVAILABLE
    !hasWriteGrant -> PersistedDirectoryAccessStatus.GRANT_MISSING
    else -> PersistedDirectoryAccessStatus.GRANTED
}
