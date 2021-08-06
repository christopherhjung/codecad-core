package com.codecad.core

import com.codecad.common.PointD
import de.lighti.clipper.Clipper
import de.lighti.clipper.Clipper.JoinType
import de.lighti.clipper.Point.LongPoint


open class PolyNode {
    private var parent: PolyNode? = null
    val polygon = mutableListOf<PointD>()
    private var index = 0
    var joinType: JoinType? = null
    var endType: Clipper.EndType? = null
    val childs = mutableListOf<PolyNode>()
    var isOpen = false

    fun addChild(child: PolyNode) {
        val cnt = childs.size
        childs.add(child)
        child.parent = this
        child.index = cnt
    }

    val childCount: Int
        get() = childs.size

    val contour: List<PointD>
        get() = polygon
    val next: PolyNode?
        get() = if (!childs.isEmpty()) childs[0] else nextSiblingUp
    private val nextSiblingUp: PolyNode?
        private get() = if (parent == null) {
            null
        } else {
            if (index == parent!!.childs.size - 1) parent!!.nextSiblingUp else parent!!.childs[index + 1]
        }

    fun getParent(): PolyNode? {
        return parent
    }

    val isHole: Boolean
        get() = isHoleNode
    private val isHoleNode: Boolean
        private get() {
            var result = true
            var node = parent
            while (node != null) {
                result = !result
                node = node.parent
            }
            return result
        }

    fun setParent(n: PolyNode?) {
        parent = n
    }

    internal enum class NodeType {
        ANY, OPEN, CLOSED
    }
}
