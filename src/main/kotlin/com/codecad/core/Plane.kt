package com.codecad.core

import com.codecad.core.Point3.Companion.times
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.minus



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

    fun distanceTo(p: Point3) : Expr {
        return normal.dot(p) - distance
    }

    fun projectTo(p: Point3) : Point3 {
        return p - normal * distanceTo(p)
    }
}
