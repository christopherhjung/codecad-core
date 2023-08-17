package com.codecad.core.face.entity

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.face.isPointInPolygon

enum class FaceType{
    Root, Surface, Hole
}

abstract class Face{
    abstract fun generateTriangles() : List<TriangleFace>
    var surface = PlaneSurface()
    open var type : FaceType = FaceType.Surface
    var edgeBound : EdgeBound = EdgeBound()
    abstract val points : Iterable<Vec3>
    var children : MutableList<Face> = mutableListOf()
    var area: Double = 0.0

    open fun isInside(pos : Vec3) : Boolean{
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

class TriangleFace(a: Vec3, b: Vec3, c : Vec3) : ConvexFace(listOf(a,b,c)){
    override fun generateTriangles()  : List<TriangleFace>{
        return listOf(this)
    }
}
/*
fun generateTriangles(placement: AxisPlacement, outline: List<Vec2>, holes: List<List<Vec2>>) : List<TriangleFace>{
    if(outline.size < 3){
        return listOf()
    }

    val plane = Plane.fromPoints(outline)
    val directedPlane = DirectedPlane.from(plane)

    fun createPoint(point: Vec2) : PolygonPoint{
        val xy = directedPlane.extractXY(point)
        return PolygonPoint(xy.x, xy.y , 0.0)
    }

    fun pointsToPolygon(points: List<Vec2>) : org.poly2tri.geometry.polygon.Polygon{
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

    fun createPoint(trianglePoint: TriangulationPoint) : Vec2 {
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
*/