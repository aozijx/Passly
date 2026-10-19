package com.aozijx.passly.feature.autofill.legacy.service.parser

internal data class BoundedTraversalResult(
    val visitedCount: Int,
    val truncated: Boolean,
)

private data class TraversalNode<T>(
    val value: T,
    val depth: Int,
)

/** Iterative pre-order traversal with explicit work limits for untrusted platform trees. */
internal fun <T> traverseBoundedPreOrder(
    roots: List<T>,
    maxDepth: Int,
    maxNodes: Int,
    children: (T) -> List<T>,
    visit: (T) -> Unit,
): BoundedTraversalResult {
    require(maxDepth >= 0) { "maxDepth must not be negative" }
    require(maxNodes > 0) { "maxNodes must be positive" }

    val pending = ArrayDeque<TraversalNode<T>>()
    roots.asReversed().forEach { pending.addLast(TraversalNode(it, depth = 0)) }
    var visitedCount = 0
    var depthTruncated = false

    while (pending.isNotEmpty() && visitedCount < maxNodes) {
        val current = pending.removeLast()
        visit(current.value)
        visitedCount += 1

        val childNodes = children(current.value)
        if (current.depth >= maxDepth) {
            if (childNodes.isNotEmpty()) depthTruncated = true
            continue
        }
        childNodes.asReversed().forEach { child ->
            pending.addLast(TraversalNode(child, current.depth + 1))
        }
    }

    return BoundedTraversalResult(
        visitedCount = visitedCount,
        truncated = depthTruncated || pending.isNotEmpty(),
    )
}
