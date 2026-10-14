package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.IdentifierNamespace

/** Platform port. Implementations must return null rather than expose raw input on failure. */
fun interface IdentifierHasher {
    fun hash(namespace: IdentifierNamespace, rawValue: String): HashedIdentifier?
}
