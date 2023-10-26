package com.codecad.core.part

import com.codecad.core.ast.vec.Vec2
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
import com.codecad.core.sketch.Intersect
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object CurveSurfaceIntersect {
    fun intersect(lhs: Curve<Vec3>, rhs: Surface) : List<Vec3>{
        return when(lhs){
            is Line -> {
                when(rhs){
                    is PlaneSurface -> intersectLinePlane(lhs, rhs)
                    is CylindricalSurface -> intersectLineCylindrical(lhs, rhs)
                    else -> throw NotImplementedError()
                }
            }

            is Circle -> {
                when(rhs){
                    is PlaneSurface -> intersectCirclePlane(lhs, rhs)
                    is CylindricalSurface -> intersectCircleCylindrical(lhs, rhs)
                    else -> throw NotImplementedError()
                }
            }

            else -> throw NotImplementedError()
        }
    }

    private fun intersectLinePlane(lhs: Line<Vec3>, rhs: PlaneSurface) : List<Vec3>{
        val plane = rhs.workplane.toPlane()
        val intersection = plane.intersect(lhs)
        return if(intersection != null){
            listOf(intersection)
        }else{
            emptyList()
        }
    }

    private fun intersectLineCylindrical(lhs: Line<Vec3>, rhs: CylindricalSurface) : List<Vec3>{
        val wkpl = rhs.workplane

        val center = wkpl.project2d(wkpl.origin)
        val projOrigin = wkpl.project2d(lhs.origin)
        val projDir = wkpl.projectDir2d(lhs.direction).normalized()

        val intersections =
            Intersect.of(Line(projOrigin, projDir), Circle(Workplane(center, Vec2.DirY, Vec2.DirX), rhs.radius))

        return intersections.map { wkpl.unproject(it) }
    }

    private fun intersectCirclePlane(lhs: Circle<Vec3>, rhs: PlaneSurface) : List<Vec3>{
        val line = Workplane.intersect(lhs.workplane, rhs.workplane)

        val wkpl = lhs.workplane
        val center = wkpl.project2d(wkpl.origin)
        val projOrigin = wkpl.project2d(line.origin)
        val projDir = wkpl.projectDir2d(line.direction).normalized()

        val intersections =
            Intersect.of(Line(projOrigin, projDir), Circle(Workplane(center, Vec2.DirY, Vec2.DirX), lhs.radius))

        return intersections.map { wkpl.unproject(it) }
    }

    private fun intersectCircleCylindrical(lhs: Circle<Vec3>, rhs: CylindricalSurface) : List<Vec3>{
        return emptyList()
    }

}