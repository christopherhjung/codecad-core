package com.codecad.core

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.face.entity.Vec3Expr


class Plane(val normal : Vec3Expr, val distance: Expr){
    companion object{
        fun fromPoints(a: Vec3Expr, b: Vec3Expr, c: Vec3Expr) : Plane {
            val ab = b - a
            val ac = c - a
            val normal = ab.cross(ac).normalized()
            val distance = normal.dot(a)
            return Plane(normal, distance)
        }

        fun fromPoints(points : List<Vec3Expr>) : Plane {
            return fromPoints(points[0], points[1], points[2])
        }

        fun fromConvexPoints(points : Iterable<Vec3Expr>) : Plane {
            return fromConvexPoints(points.iterator())
        }

        fun fromConvexPoints(points : Iterator<Vec3Expr>) : Plane {
            return fromPoints(getNextOr(points), getNextOr(points), getNextOr(points))
        }

        fun getNextOr(points : Iterator<Vec3Expr>) : Vec3Expr {
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

    fun distanceTo(p: Vec3Expr) : Expr {
        return normal.dot(p) - distance
    }

    fun projectTo(p: Vec3Expr) : Vec3Expr {
        return p - normal * distanceTo(p)
    }
}
