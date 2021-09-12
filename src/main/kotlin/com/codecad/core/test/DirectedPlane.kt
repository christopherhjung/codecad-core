package com.codecad.core.test

import com.codecad.common.Plane
import com.codecad.common.PointD

class DirectedPlane(val undirected: Plane, val first: PointD, val second: PointD){
    val normal get() = undirected.normal
    val distance get() = undirected.distance

    fun projectXYTo(point: PointD) : PointD{
        return projectXYTo(point.x, point.y)
    }

    fun projectXYTo(x: Double, y: Double) : PointD{
        return first * x + second * y + normal * distance
    }

    fun extractXY(point: PointD) : PointD{
        val x = first.dot(point) / first.squaredLength()
        val y = second.dot(point) / second.squaredLength()

        return PointD(x,y, 0.0)
    }

    companion object{
        fun from(plane: Plane) : DirectedPlane{
            val normal = plane.normal
            val first = if(normal.y != 0.0 || normal.x != 0.0){
                PointD(-normal.y, normal.x, 0.0)
            }else if(normal.z != 0.0){
                PointD(0.0, -normal.z, normal.y)
            }else{
                throw RuntimeException("normal vector has size 0")
            }

            return from(plane, first.normalized())
        }

        fun from(plane: Plane, first: PointD) : DirectedPlane{
            val second = plane.normal.cross(first)
            return DirectedPlane(plane, first, second)
        }

        val XY = from(Plane.XY, PointD(1.0))
    }
}
