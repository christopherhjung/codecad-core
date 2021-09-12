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

open class ConvexFace( val positions: List<Node>) : Face(), HasSide {
    override var side: Side = Side.Unknown

    override fun generateTriangles()  : List<TriangleFace>{
        val result = mutableListOf<TriangleFace>()
        for( i in 0 until positions.size - 2 ){
            result.add(TriangleFace(positions[0], positions[i + 1], positions[i + 2]))
        }

        return result
    }

    override fun toPlane() : Plane{
        return Plane.fromPoints(positions[0].point, positions[1].point, positions[2].point)
    }
}

interface HasSide{
    var side : Side
}

class PolygonFace( val positions: List<Node>, val plane: Plane) : Face(), HasSide {
    var parent: PolygonFace? = null
    val holes = mutableSetOf<PolygonFace>()
    var clockwise: Boolean = false
    var area: Double = 0.0
    override var side: Side = Side.Unknown

    val type: FaceType
        get() = if(!clockwise) FaceType.Surface else FaceType.Hole

    override fun generateTriangles()  : List<TriangleFace>{
        return generateTriangles(positions.map { it.point }, holes.map { it.positions.map { it.point } })
    }

    override fun toPlane() : Plane{
        return plane
    }
}

fun generateTriangles(outline: List<PointD>, holes: List<List<PointD>>) : List<TriangleFace>{
    if(outline.size < 3){
        return listOf()
    }

    val plane = Plane.fromPoints(outline)
    val directedPlane = DirectedPlane.from(plane)

    val map = mutableMapOf<Double, PointD>()

    var current = 0.0

    fun createPoint(point: PointD) : PolygonPoint{
        val x = directedPlane.first.dot(point) / directedPlane.first.squaredLength()
        val y = directedPlane.second.dot(point) / directedPlane.second.squaredLength()
        current++

        map[current] = point
        return PolygonPoint(x,y , current)
    }

    fun pointsToPolygon(points: List<PointD>) : org.poly2tri.geometry.polygon.Polygon{
        val list = mutableListOf<PolygonPoint>()
        for( point in points ){
            list.add(createPoint(point))
        }
        return org.poly2tri.geometry.polygon.Polygon(list)
    }

    val parent = pointsToPolygon(outline)

    for( child in holes ){
        if(outline.size < 3){
            continue
        }

        parent.addHole(pointsToPolygon(child))
    }

    try{
        Poly2Tri.triangulate(parent)
    }catch (e: Exception){
        e.printStackTrace()
        throw e
    }

    val triangles = mutableListOf<TriangleFace>()

    fun createPoint(trianglePoint: TriangulationPoint) : Node {
        return Node( directedPlane.first * trianglePoint.x + directedPlane.second * trianglePoint.y + directedPlane.normal * directedPlane.distance )
        //return Node(map[trianglePoint.z]!!)
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

class RoutedFace(val root : Edge, val holes: List<Edge>, val plane: Plane, val original: Face? = null) : Face(), Iterable<Edge>{
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
        return generateTriangles(root.points().toList(), holes.map { it.points().toList() })
    }

    override fun toPlane(): Plane {
        return plane
    }
}
