package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Ellipse
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.brep.surface.Surface
import kotlin.math.pow
import kotlin.math.sqrt

class SurfaceIntersect {
    fun intersect(lhs: Surface, rhs: Surface) : List<Curve<Vec3>>{
        return when(lhs){
            is PlaneSurface -> {
                when(rhs){
                    is PlaneSurface -> intersectPlanePlane(lhs, rhs)
                    is CylindricalSurface -> intersectPlaneCylindrical(lhs, rhs)
                    else -> throw NotImplementedError()
                }
            }

            is CylindricalSurface -> {
                when(rhs){
                    is PlaneSurface -> intersectPlaneCylindrical(rhs, lhs)
                    else -> throw NotImplementedError()
                }
            }

            else -> throw NotImplementedError()
        }
    }

    fun intersectPlanePlane(lhs: PlaneSurface, rhs: PlaneSurface) : List<Curve<Vec3>>{
        val lhsPlane = lhs.workplane.toPlane()
        val rhsPlane = rhs.workplane.toPlane()

        if(lhsPlane.normal.cross(rhsPlane.normal).squaredLength() < 1e-10.pow(1.0)){
            return emptyList()
        }

        return listOf(Plane.intersect(lhsPlane, rhsPlane))
    }

    fun intersectPlaneCylindrical(pln: PlaneSurface, cyl: CylindricalSurface) : List<Curve<Vec3>>{
        val planeWorkplane = pln.workplane
        val cylWorkplane = cyl.workplane
        val radius = cyl.radius

        val normalDot = planeWorkplane.normal.dot(cylWorkplane.normal)
        return if( normalDot < 1e-10){
            val cylOrigin = cylWorkplane.origin
            val projCylOrigin = planeWorkplane.project3d(cylOrigin)
            val offset = cylOrigin.distanceTo(projCylOrigin) - radius
            return if( offset > 1e-10 ){
                emptyList()
            }else if( offset > -1e-10 ){
                listOf(Line(planeWorkplane.origin, cylWorkplane.normal))
            }else{
                val dir = planeWorkplane.normal.cross(cylWorkplane.normal)
                val spanVec = dir * sqrt(radius.pow(2) - offset)
                val first = projCylOrigin + spanVec
                val second = projCylOrigin - spanVec

                listOf(Line(first, cylWorkplane.normal), Line(second, cylWorkplane.normal))
            }
        }else{
            val newOrigin = planeWorkplane.project3d(cylWorkplane.origin)
            if(planeWorkplane.normal.cross(cylWorkplane.normal).squaredLength() < 1e-10.pow(1.0)){
                val circleWorkplane = Workplane(newOrigin, planeWorkplane.normal, cylWorkplane.x)
                listOf(Circle(circleWorkplane, radius))
            }else{
                val newX = Vec3.project(cylWorkplane.normal, planeWorkplane.normal) - cylWorkplane.normal
                val ellipseWorkplane = Workplane(newOrigin, planeWorkplane.normal, newX)
                val major = radius / normalDot
                listOf(Ellipse(ellipseWorkplane, major, radius))
            }
        }
    }
}