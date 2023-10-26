package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.sketch.isPointInPolygon
import com.codecad.core.volume.Volume

enum class CombineKind{
    Add, Subtract, Intersect
}

class Intersection(var lhsFace : Face, var rhsFace : Face, var curve : Curve<Vec3>){
}

object BooleanCombine{
    val curves = arrayListOf<Curve<Vec3>>()

    fun combine(kind: CombineKind, lhsVolume : Volume, rhsVolume: Volume) : Volume{

        for( lhsShell in lhsVolume.shells ){
            for( lhsFace in lhsShell.faces ){

                for( rhsShell in rhsVolume.shells ){
                    for( rhsFace in rhsShell.faces ){

                        //val curve = combine(lhsFace, rhsFace)

                        intersectFace(lhsFace, rhsFace)
                        //Intersection(lhsFace, rhsFace, curve)
                    }
                }
            }
        }

        return lhsVolume
    }

    data class Intersection(val vertex: Vertex<Vec3>, val face: Face)

    fun intersectFace(lhsFace: Face, rhsFace: Face){
        val interCurves = SurfaceIntersect.intersect(lhsFace.surface, rhsFace.surface)

        for( interCurve in interCurves ){
            val points = findIters(interCurve, lhsFace, rhsFace)
            val edges = createEdges(points, interCurve, lhsFace, rhsFace)
        }
    }

    private fun findIters(
        interCurve: Curve<Vec3>,
        lhsFace: Face,
        rhsFace: Face
    ): ArrayList<Intersection> {
        val points = arrayListOf<Intersection>()

        fun scan(face: Face) {
            for (bound in face.bounds) {
                for (edgeLoop in bound.loop) {
                    val orientedEdge = edgeLoop.edge
                    val edge = orientedEdge.edge

                    val inters =
                        CurveEdgeIntersect.intersect(interCurve, face.surface, edge)
                    inters.forEach { points.add(Intersection(Vertex(it), face)) }
                }
            }
        }

        scan(lhsFace)
        scan(rhsFace)

        val line = interCurve as Line<Vec3>
        points.sortBy { line.direction.dot(it.vertex.point - line.origin) }
        return points
    }



    fun createEdges(points: List<Intersection>, curve: Curve<Vec3>, lhsFace: Face, rhsFace: Face) : List<Edge<Vec3>>{
        var lhsActive = false
        var rhsActive = false
        var last : Intersection? = null
        val edges = arrayListOf<Edge<Vec3>>()
        for( inter in points ){
            if(lhsActive && rhsActive){
                edges.add(Edge(curve, EdgeBound(last!!.vertex, inter.vertex)))
            }

            if(lhsFace === inter.face){
                lhsActive = !lhsActive
            }else if(rhsFace === inter.face){
                rhsActive = !rhsActive
            }

            last = inter
        }
        assert(!lhsActive && !rhsActive)
        return edges
    }

    var count = 0
    var test = 0
    fun cutEdge(lhsEdge: Edge<Vec3>, rhsFace: Face, rhsPlane : Plane){
        when(val curve = lhsEdge.curve){
            is Line -> {
                val intersection = rhsPlane.intersect(curve)
                if(intersection != null && isInside(intersection, rhsFace)){
                    println(intersection)
                }
            }
        }
    }

    fun isInside(point: Vec3, rhsFace: Face) : Boolean{
        val planeSurface = rhsFace.surface as PlaneSurface

        for(bound in rhsFace.bounds){
            if((bound.sense == FaceBoundKind.OuterBound) != isInside(point, bound, planeSurface.workplane)){
                return false
            }
        }

        return true
    }

    fun isInside(point: Vec3, bound: FaceBound<Vec3>, workplane : Workplane<Vec3>) : Boolean{
        val projPoint = workplane.project2d(point)
        val points = bound.loop.map { workplane.project2d(it.edge.start.point) }
        return isPointInPolygon(projPoint, points)
    }

    fun combine(lhsFace: Face, rhsFace: Face) : Curve<Vec3>{
        val lhsSurface = lhsFace.surface
        val rhsSurface = rhsFace.surface


        when(lhsSurface){
            is PlaneSurface -> {
                when(rhsSurface){
                    is PlaneSurface -> {
                        val line = combine(lhsSurface, rhsSurface)
                        curves.add(line)

                        return line
                    }
                }
            }
        }

        throw RuntimeException("Not implemented")
    }

    fun combine(lhsSurface: PlaneSurface, rhsSurface: PlaneSurface) : Line<Vec3>{
        return Workplane.intersect(lhsSurface.workplane, rhsSurface.workplane)
    }
}

