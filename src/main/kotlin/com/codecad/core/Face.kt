package com.codecad.core

import com.codecad.common.*
import com.codecad.core.test.*
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint


abstract class Face{
    abstract fun generateTriangles() : List<TriangleFace>

    abstract fun toPlane() : Plane
}

class TriangleFace(vararg points: Node) : ConvexFace(points.toList()){
    override fun generateTriangles()  : List<TriangleFace>{
        return listOf(this)
    }
}

open class ConvexFace( val points: List<Node>) : Face() {
    override fun generateTriangles()  : List<TriangleFace>{
        val result = mutableListOf<TriangleFace>()
        for( i in 0 until points.size - 2 ){
            result.add(TriangleFace(points[0], points[i + 1], points[i + 2]))
        }

        return result
    }

    override fun toPlane() : Plane{
        return Plane.fromPoints(points[0].point, points[1].point, points[2].point)
    }
}

class PolygonFace( val positions: List<Node>) : Face() {
    var parent: PolygonFace? = null
    val holes = mutableSetOf<PolygonFace>()
    var clockwise: Boolean = false
    var area: Double = 0.0
    var side: Side = Side.Unknown

    val type: FaceType
        get() = if(!clockwise) FaceType.Surface else FaceType.Hole

    override fun generateTriangles()  : List<TriangleFace>{
        fun pointsToPolygon(polygonFace: PolygonFace) : org.poly2tri.geometry.polygon.Polygon{
            val list = mutableListOf<PolygonPoint>()
            for( point in polygonFace.positions ){
                list.add(PolygonPoint(point.point.x, point.point.y, point.point.z))
            }
            return org.poly2tri.geometry.polygon.Polygon(list)
        }

        val parent = pointsToPolygon(this)

        for( child in this.holes ){
            parent.addHole(pointsToPolygon(child))
        }

        Poly2Tri.triangulate(parent)

        val triangles = mutableListOf<TriangleFace>()

        fun createPoint(trianglePoint: TriangulationPoint) : Node {
            return Node(PointD(trianglePoint.x, trianglePoint.y, trianglePoint.z))
        }

        val offset = 0//if(inverted) 1 else 0

        for( triangle in parent.triangles ){
            val points = triangle.points
            triangles.add(TriangleFace(
                createPoint(points[0]),
                createPoint(points[1 + offset]),
                createPoint(points[2 - offset])))
        }

        return triangles
    }

    override fun toPlane() : Plane{
        return Plane.fromPoints(positions[0].point, positions[1].point, positions[2].point)
    }
}

class RoutedFace(val root : Edge, val holes: List<Edge>) : Face(), Iterable<Edge>{
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
                    val result =  current.source.node.point
                    current = current.next!!
                    return result
                }
            }
        }
    }

    fun nodes() : Iterable<Node>{
        return Iterable {
            var current : Edge = root
            var first = true
            object : Iterator<Node>{
                override fun hasNext(): Boolean {
                    return first || current != root
                }

                override fun next(): Node {
                    first = false
                    val result =  current.source.node
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

    fun edges() : Iterable<Edge>{
        return Iterable {
            var current : Edge = root
            var first = true
            object : Iterator<Edge>{
                override fun hasNext(): Boolean {
                    return first || current != root
                }

                override fun next(): Edge {
                    first = false
                    val result =  current
                    current = current.next!!
                    return result
                }
            }
        }
    }

    override fun generateTriangles(): List<TriangleFace> {
        TODO("Not yet implemented")
    }

    override fun toPlane(): Plane {
        val a = root
        val b = a.next
        val c = b?.next
        return Plane.fromPoints(a.source.node.point, b!!.source.node.point, c!!.source.node.point)
    }
}
