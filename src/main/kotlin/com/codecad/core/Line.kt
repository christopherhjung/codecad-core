package com.codecad.core

import com.codecad.core.Point3.Companion.times
import com.codecad.core.ast.primitive.minus

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