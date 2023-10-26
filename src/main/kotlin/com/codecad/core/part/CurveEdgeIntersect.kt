package com.codecad.core.part

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.brep.surface.Surface
import com.codecad.core.sketch.EPSILON
import kotlin.math.pow
import kotlin.math.sqrt

object CurveEdgeIntersect {
    fun intersect(lhs: Curve<Vec3>, rhsSurface: Surface, rhsEdge: Edge<Vec3>) : List<Vec3>{
        return when(lhs){
            is Line -> {
                when(rhsSurface){
                    is PlaneSurface -> intersectLinePlane(lhs, rhsEdge)
                    is CylindricalSurface -> intersectLineCylindrical(lhs, rhsSurface, rhsEdge)
                    else -> throw NotImplementedError()
                }
            }

            is Circle -> {
                when(rhsSurface){
                    is PlaneSurface -> intersectCirclePlane(lhs, rhsSurface, rhsEdge)
                    is CylindricalSurface -> intersectCircleCylindrical(lhs, rhsSurface, rhsEdge)
                    else -> throw NotImplementedError()
                }
            }

            else -> throw NotImplementedError()
        }
    }

    public fun intersectLinePlane(lhs: Line<Vec3>, rhsEdge: Edge<Vec3>) : List<Vec3>{
        val edgeCurve = rhsEdge.curve as Line<Vec3>

        val s1 = lhs.direction
        val s2 = edgeCurve.direction
        val sd = lhs.origin - edgeCurve.origin

        val normal = s1.cross(s2)
        val sqrLen = normal.squaredLength()
        if(sqrLen < EPSILON.pow(2)) return emptyList()

        val t = s2.cross(sd).dot(normal) / sqrLen
        return listOf(lhs.origin + s1 * t)
    }

    private fun intersectLineCylindrical(lhs: Line<Vec3>, rhsSurface: CylindricalSurface, rhsEdge: Edge<Vec3>) : List<Vec3>{
        return emptyList()
    }

    private fun intersectCirclePlane(circle: Circle<Vec3>, rhsSurface: PlaneSurface, rhsEdge: Edge<Vec3>) : List<Vec3>{
        val edgeCurve = rhsEdge.curve as Line<Vec3>

        val r1 = circle.radius
        val center = circle.workplane.origin
        val start2c = center - edgeCurve.origin
        val dir = edgeCurve.direction
        val l2projC = edgeCurve.origin + Vec3.project(start2c, dir)
        val c2l = l2projC - center
        val c2lDistance = c2l.length() - r1

        return if(c2lDistance > EPSILON){
            emptyList()
        }else if(c2lDistance > -EPSILON){
            listOf(l2projC)
        }else{
            val h = dir * sqrt(r1 * r1 - c2l.squaredLength())
            val first = l2projC - h
            val second = l2projC + h
            return listOf(first, second)
        }
    }

    private fun intersectCircleCylindrical(lhs: Circle<Vec3>, rhsSurface: CylindricalSurface, rhsEdge: Edge<Vec3>) : List<Vec3>{
        return emptyList()
    }

}