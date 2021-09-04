package com.codecad.core

import com.codecad.common.*
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint


abstract class Face(val points: List<PointD>){
    abstract fun generateTriangles() : List<TriangleFace>
    abstract fun contains(point: PointD) : Boolean

    fun toPlane() : Plane{
        return Plane.fromPoints(points[0], points[1], points[2])
    }
}

class TriangleFace(vararg points: PointD) : ConvexFace(points.toList()){
    override fun generateTriangles()  : List<TriangleFace>{
        return listOf(this)
    }
}

open class ConvexFace( points: List<PointD>) : Face(points){
    override fun generateTriangles()  : List<TriangleFace>{
        val result = mutableListOf<TriangleFace>()
        for( i in 0 until points.size - 2 ){
            result.add(TriangleFace(points[0], points[i + 1], points[i + 2]))
        }

        return result
    }

    override fun contains(point: PointD): Boolean {
        /*for( (left, right) in points.overshoot() ){
            if((right - left).cross(point - left) > 0){

            }
        }*/
        return true
    }
}

class PolygonFace(points: List<PointD>, val inverted: Boolean = false) : Face(points){
    var parent: PolygonFace? = null
    val children = mutableSetOf<PolygonFace>()
    var clockwise: Boolean = false
    var area : Double = 0.0
    var leftmost: PointD? = null

    override fun contains(point: PointD): Boolean {
        val lines = mutableListOf<LineD>()
        for((left, right) in points.rollover()){
            if(left.x.compareTo(point.x) * point.x.compareTo(right.x) >= 0){
                println(left)
                lines.add(LineD(left, right))
            }
        }

        return true
    }

    override fun generateTriangles()  : List<TriangleFace>{
        fun pointsToPolygon(polygonFace: PolygonFace) : org.poly2tri.geometry.polygon.Polygon{
            val list = mutableListOf<PolygonPoint>()
            for( point in polygonFace.points ){
                list.add(PolygonPoint(point.x, point.y, point.z))
            }
            return org.poly2tri.geometry.polygon.Polygon(list)
        }

        val parent = pointsToPolygon(this)

        for( child in this.children ){
            parent.addHole(pointsToPolygon(child))
        }

        Poly2Tri.triangulate(parent)

        val triangles = mutableListOf<TriangleFace>()

        fun createPoint(trianglePoint: TriangulationPoint) : PointD {
            return PointD(trianglePoint.x, trianglePoint.y, trianglePoint.z)
        }

        val offset = if(inverted) 1 else 0

        for( triangle in parent.triangles ){
            val points = triangle.points
            triangles.add(TriangleFace(
                createPoint(points[0]),
                createPoint(points[1 + offset]),
                createPoint(points[2 - offset])))
        }

        return triangles
    }
}
