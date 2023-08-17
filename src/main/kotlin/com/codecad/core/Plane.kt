package com.codecad.core

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr

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
