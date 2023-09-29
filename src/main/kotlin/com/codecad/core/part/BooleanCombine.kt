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

                        cutLines(lhsFace, rhsFace)
                        //Intersection(lhsFace, rhsFace, curve)
                    }
                }
            }
        }

        return lhsVolume
    }

    fun cutLines(lhsFace: Face, rhsFace: Face){
        val planeSurface = rhsFace.surface as PlaneSurface
        val plane = planeSurface.workplane.toPlane()

        for( lhsBound in lhsFace.bounds ) {
            for (lhsEdgeLoop in lhsBound.loop) {
                val lhsOrientedEdge = lhsEdgeLoop.edge
                val lhsEdge = lhsOrientedEdge.edge

                cutEdge(lhsEdge, rhsFace, plane)
            }
        }
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
        val points = bound.loop.map { workplane.project2d(it.edge.start!!.point) }
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

