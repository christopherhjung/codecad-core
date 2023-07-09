package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.face.isPointInPolygon
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint

enum class FaceType{
    Root, Surface, Hole
}

abstract class Face{
    abstract fun generateTriangles() : List<TriangleFace>
    abstract val plane : Plane
    open val type : FaceType = FaceType.Surface
    abstract val points : Iterable<PointD>
    abstract val children : List<Face>

    fun isInside(pos : PointD) : Boolean{
        return if(isPointInPolygon(pos, points)){
            for( hole in children ){
                if(isPointInPolygon(pos, hole.points)){
                    return false
                }
            }

            true
        }else false
    }
}


class TriangleFace(vararg points: PointD) : ConvexFace(points.toList()){
    override fun generateTriangles()  : List<TriangleFace>{
        return listOf(this)
    }

    override val points: Iterable<PointD>
        get() = points

    override val children: List<Face>
        get() = emptyList()
}

fun generateTriangles(outline: List<PointD>, holes: List<List<PointD>>) : List<TriangleFace>{
    if(outline.size < 3){
        return listOf()
    }

    val plane = Plane.fromPoints(outline)
    val directedPlane = DirectedPlane.from(plane)

    fun createPoint(point: PointD) : PolygonPoint{
        val xy = directedPlane.extractXY(point)
        return PolygonPoint(xy.x, xy.y , 0.0)
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

    fun createPoint(trianglePoint: TriangulationPoint) : PointD {
        return directedPlane.projectXYTo(trianglePoint.x, trianglePoint.y)
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
