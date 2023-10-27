package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.brep.surface.Surface
import com.codecad.core.sketch.EPSILON
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object CurveEdgeIntersect {
    fun intersect(lhsCurve: Curve<Vec3>, rhsSurface: Surface, rhsEdge: Edge<Vec3>) : List<Vec3>{
        val rhsCurve = rhsEdge.curve
        return when(lhsCurve){
            is Line -> {
                when(rhsSurface){
                    is PlaneSurface -> intersectLinePlane(lhsCurve, rhsCurve)
                    is CylindricalSurface -> intersectLineCylindrical(lhsCurve, rhsSurface, rhsCurve)
                    else -> throw NotImplementedError()
                }
            }

            is Circle -> {
                when(rhsSurface){
                    is PlaneSurface -> intersectCirclePlane(lhsCurve, rhsSurface, rhsCurve)
                    is CylindricalSurface -> intersectCircleCylindrical(lhsCurve, rhsSurface, rhsCurve)
                    else -> throw NotImplementedError()
                }
            }

            else -> throw NotImplementedError()
        }.filter { rhsEdge.inside(it) }
    }

    public fun intersectLinePlane(lhs: Line<Vec3>, rhsCurve: Curve<Vec3>) : List<Vec3>{
        return when(rhsCurve){
            is Line -> intersectPlaneLineLine(lhs, rhsCurve)
            is Circle -> intersectPlaneLineCircle(lhs, rhsCurve)
            else -> throw NotImplementedError()
        }
    }

    private fun intersectCirclePlane(circle: Circle<Vec3>, rhsSurface: PlaneSurface, rhsCurve: Curve<Vec3>) : List<Vec3>{
        return when(rhsCurve){
            is Line -> intersectPlaneLineCircle(rhsCurve, circle)
            is Circle -> intersectPlaneCircleCircle(circle, rhsCurve)
            else -> throw NotImplementedError()
        }
    }

    private fun intersectLineCylindrical(lhs: Line<Vec3>, rhsSurface: CylindricalSurface, rhsEdge: Curve<Vec3>) : List<Vec3>{
        return emptyList()
    }

    private fun intersectCircleCylindrical(lhs: Circle<Vec3>, rhsSurface: CylindricalSurface, rhsEdge: Curve<Vec3>) : List<Vec3>{
        return emptyList()
    }

    fun Edge<Vec3>.inside(p : Vec3) : Boolean{
        if(bound.isUnbounded()){
            return true
        }

        val p0 = bound.start.point
        val p1 = bound.end.point

        if(p0.near(p, EPSILON) || p1.near(p, EPSILON)){
            return true
        }

        return when(val curve = curve){
            is Line -> {
                ((p0.x - EPSILON <= p.x) == (p.x <= p1.x + EPSILON)) &&
                ((p0.y - EPSILON <= p.y) == (p.y <= p1.y + EPSILON)) &&
                ((p0.z - EPSILON <= p.z) == (p.z <= p1.z + EPSILON))
            }
            is Circle -> {
                val radius = curve.radius
                val center = curve.workplane.origin

                return abs(center.distanceTo(p) - radius) < EPSILON
            }
            else -> true
        }
    }


    private fun intersectPlaneLineLine(lhs: Line<Vec3>, rhs: Line<Vec3>) : List<Vec3>{
        val s1 = lhs.direction
        val s2 = rhs.direction
        val sd = lhs.origin - rhs.origin

        val normal = s1.cross(s2)
        val sqrLen = normal.squaredLength()
        if(sqrLen < EPSILON.pow(2)) return emptyList()

        val t = s2.cross(sd).dot(normal) / sqrLen
        return listOf(lhs.origin + s1 * t)
    }

    private fun intersectPlaneLineCircle(line: Line<Vec3>, circle: Circle<Vec3>) : List<Vec3>{
        val r1 = circle.radius
        val center = circle.workplane.origin
        val start2c = center - line.origin
        val dir = line.direction
        val l2projC = line.origin + Vec3.project(start2c, dir)
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

    private fun intersectPlaneCircleCircle(lhs: Circle<Vec3>, rhs: Circle<Vec3>) : List<Vec3>{
        val r1 = lhs.radius
        val r2 = rhs.radius
        val p1 = lhs.workplane.origin
        val p2 = rhs.workplane.origin
        val distance = p2.distanceTo(p1)
        val radiusSum = r1 + r2
        return if(distance > radiusSum || distance <= abs(r2 - r1)){
            emptyList()
        } else {
            val dir = (p2 - p1) / distance

            if(abs(distance - radiusSum) < EPSILON){
                listOf(p1 + dir * r1)
            }else{
                val normal = lhs.workplane.normal
                val a = 0.5 * (r1*r1 - r2*r2 + distance*distance) / distance
                val p3 = p1 + dir * a
                val h = dir.cross(normal) * sqrt(r1*r1 - a*a)
                val i1 = p3 + h
                val i2 = p3 - h
                listOf(i1, i2)
            }
        }
    }
}