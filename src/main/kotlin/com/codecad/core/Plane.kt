package com.codecad.core

import com.codecad.core.sketch.Expr
import com.codecad.core.sketch.World
import com.codecad.core.sketch.minus
import com.codecad.core.Point3.Companion.times
import kotlin.math.pow

class Line(val origin: Point3, val direction: Point3){
    companion object{
        operator fun Double.times(other: Point3): Point3 {
            return Point3(other.x * this, other.y * this, other.z * this)
        }

        private fun projectPlane(a: Plane, b: Plane) : Point3 {
            return (a.distance - b.distance * ( a.normal.dot(b.normal) ))/
                    (1.0 - a.normal.dot(b.normal).pow(2.0)) * a.normal
        }

        fun projectPlaneFull(a: Plane, b: Plane) : Point3 {
            return (a.distance * b.normal.squaredLength() - b.distance * ( a.normal.dot(b.normal) ))/
                    (a.normal.squaredLength() * b.normal.squaredLength() - a.normal.dot(b.normal).pow(2.0)) * a.normal
        }

        fun fromPlanes(a: Plane, b: Plane) : Line {
            val a = a.normalized()
            val b = b.normalized()
            val direction = a.normal.cross(b.normal)
            val origin = projectPlane(a,b) + projectPlane(b,a)
            return Line(origin, direction)
        }
    }
}

class Plane(val normal : Point3, val distance: Expr){
    companion object{
        fun fromPoints(a: Point3, b: Point3, c: Point3) : Plane {
            val ab = b - a
            val ac = c - a
            val normal = ab.cross(ac).normalized()
            val distance = normal.dot(a)
            return Plane(normal, distance)
        }

        fun fromPoints(points : List<Point3>) : Plane {
            return fromPoints(points[0], points[1], points[2])
        }

        fun fromConvexPoints(points : Iterable<Point3>) : Plane {
            return fromConvexPoints(points.iterator())
        }

        fun fromConvexPoints(points : Iterator<Point3>) : Plane {
            return fromPoints(getNextOr(points), getNextOr(points), getNextOr(points))
        }

        fun getNextOr(points : Iterator<Point3>) : Point3{
            return points.next()
        }
    }

    fun normalized() : Plane {
        val length = normal.length()
        return Plane(normal / length, distance / length)
    }

    fun flip(offset: Double = 0.0) : Plane{
        return Plane(normal * -1.0, distance * -1 + offset)
    }

    fun move( offset: Double ) : Plane{
        return Plane(normal, distance + offset)
    }

    fun distanceTo(p: Point3) : Expr{
        return normal.dot(p) - distance
    }

    fun projectTo(p: Point3) : Point3 {
        return p - normal * distanceTo(p)
    }
}
