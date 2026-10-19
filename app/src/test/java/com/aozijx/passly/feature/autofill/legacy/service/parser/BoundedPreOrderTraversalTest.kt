package com.aozijx.passly.feature.autofill.legacy.service.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedPreOrderTraversalTest {
    @Test
    fun `visits roots and children in pre-order without recursion`() {
        val first = node("first", node("first-child"), node("second-child"))
        val second = node("second")
        val visited = mutableListOf<String>()

        val result = traverseBoundedPreOrder(
            roots = listOf(first, second),
            maxDepth = 8,
            maxNodes = 16,
            children = Node::children,
        ) { visited += it.name }

        assertEquals(listOf("first", "first-child", "second-child", "second"), visited)
        assertEquals(4, result.visitedCount)
        assertFalse(result.truncated)
    }

    @Test
    fun `stops at the node budget and reports truncation`() {
        val visited = mutableListOf<String>()

        val result = traverseBoundedPreOrder(
            roots = listOf(node("one"), node("two"), node("three")),
            maxDepth = 8,
            maxNodes = 2,
            children = Node::children,
        ) { visited += it.name }

        assertEquals(listOf("one", "two"), visited)
        assertEquals(2, result.visitedCount)
        assertTrue(result.truncated)
    }

    @Test
    fun `does not descend past the depth budget`() {
        val visited = mutableListOf<String>()
        val root = node("root", node("child", node("too-deep")))

        val result = traverseBoundedPreOrder(
            roots = listOf(root),
            maxDepth = 1,
            maxNodes = 16,
            children = Node::children,
        ) { visited += it.name }

        assertEquals(listOf("root", "child"), visited)
        assertTrue(result.truncated)
    }

    private data class Node(
        val name: String,
        val children: List<Node>,
    )

    private fun node(name: String, vararg children: Node) = Node(name, children.toList())
}
