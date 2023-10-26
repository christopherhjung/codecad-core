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
import com.codecad.core.sketch.EPSILON
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object SurfaceIntersect {
    fun intersect(lhs: Surface, rhs: Surface) : List<Curve<Vec3>>{
        return when(lhs){
            is PlaneSurface -> {
                when(rhs){
                    is PlaneSurface -> intersectPlane(lhs, rhs)
                    is CylindricalSurface -> intersectPlaneCylindrical(lhs, rhs)
                    else -> intersectMarching(lhs, rhs)
                }
            }

            is CylindricalSurface -> {
                when(rhs){
                    is PlaneSurface -> intersectPlaneCylindrical(rhs, lhs)
                    is CylindricalSurface -> intersectCylindrical(lhs, rhs)
                    else -> intersectMarching(lhs, rhs)
                }
            }

            else -> intersectMarching(lhs, rhs)
        }
    }

    private fun intersectPlane(lhs: PlaneSurface, rhs: PlaneSurface) : List<Curve<Vec3>>{
        val lhsPlane = lhs.workplane.toPlane()
        val rhsPlane = rhs.workplane.toPlane()

        if(lhsPlane.normal.cross(rhsPlane.normal).squaredLength() < 1e-10.pow(1.0)){
            return emptyList()
        }

        return listOf(Plane.intersect(lhsPlane, rhsPlane))
    }

    private fun intersectPlaneCylindrical(pln: PlaneSurface, cyl: CylindricalSurface) : List<Curve<Vec3>>{
        val planeWorkplane = pln.workplane
        val cylWorkplane = cyl.workplane
        val radius = cyl.radius
        val plnNormal = planeWorkplane.normal
        val cylNormal = cylWorkplane.normal

        val normalDot = plnNormal.dot(cylNormal)
        return if( normalDot < 1e-10 ){
            val cylOrigin = cylWorkplane.origin
            val projCylOrigin = planeWorkplane.project3d(cylOrigin)
            val segOffset = cylOrigin.distanceTo(projCylOrigin)
            val offset = segOffset - radius
            return if( offset > EPSILON ){
                emptyList()
            }else if( offset > -EPSILON ){
                listOf(Line(planeWorkplane.origin, cylNormal))
            }else{
                val dir = plnNormal.cross(cylNormal)
                val spanVec = dir * sqrt(radius.pow(2) - segOffset)
                val first = projCylOrigin + spanVec
                val second = projCylOrigin - spanVec

                listOf(Line(first, cylNormal), Line(second, cylNormal))
            }
        }else{
            val newOrigin = planeWorkplane.project3d(cylWorkplane.origin)
            if(abs(1.0 - normalDot) < EPSILON.pow(2)){
                val circleWorkplane = Workplane(newOrigin, plnNormal, cylWorkplane.x)
                listOf(Circle(circleWorkplane, radius))
            }else{
                val newX = Vec3.project(cylNormal, plnNormal) - cylNormal
                val ellipseWorkplane = Workplane(newOrigin, plnNormal, newX)
                val major = radius / normalDot
                listOf(Ellipse(ellipseWorkplane, major, radius))
            }
        }
    }

    private fun intersectCylindrical(lhs: CylindricalSurface, rhs: CylindricalSurface) : List<Curve<Vec3>>{
        return emptyList()
    }

    private fun intersectMarching(lhs: Surface, rhs: Surface) : List<Curve<Vec3>>{
        return emptyList()
    }
}