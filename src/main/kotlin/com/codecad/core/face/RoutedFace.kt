package com.codecad.core.face

import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.face.entity.*


class RoutedFace(val root : Edge, override var children: MutableList<RoutedFace>, var area : Double, override val plane: Plane) : Face(), Iterable<Edge>{

    override val points: Iterable<PointD>
        get() = points()

    override fun iterator(): Iterator<Edge> {
        var current : Edge = root
        var first = true

        return object : Iterator<Edge>{
            override fun hasNext(): Boolean {
                return first || current != root
            }

            override fun next(): Edge {
                first = false
                val result =  current//.source!!//.node.p
                current = current.next!!
                return result
            }
        }
    }

    fun points() : Iterable<PointD>{
        return Iterable {
            var current : Edge = root
            var first = true
            object : Iterator<PointD>{
                override fun hasNext(): Boolean {
                    return first || current != root
                }

                override fun next(): PointD {
                    first = false
                    val result =  current.source.point
                    current = current.next!!
                    return result
                }
            }
        }
    }

    fun corners() : Iterable<Corner>{
        return Iterable {
            var current : Edge = root
            var first = true
            object : Iterator<Corner>{
                override fun hasNext(): Boolean {
                    return first || current != root
                }

                override fun next(): Corner {
                    first = false
                    val result = current.source
                    current = current.next!!
                    return result
                }
            }
        }
    }

    override fun generateTriangles(): List<TriangleFace> {
        return generateTriangles(root.pointsIter().toList(), children.map { it.points().toList() })
    }
}
