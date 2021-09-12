package com.codecad.core

import com.codecad.common.PointD
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.sign

class RaycastResult(val hit: PointD, val distance: Double, val valid: Boolean = true){
    constructor(valid: Boolean) : this(PointD.ZERO, 0.0, valid)
}

class Ray(val origin : PointD, val direction : PointD){
    fun at(ratio: Double): RaycastResult {
        return RaycastResult(
             direction * ratio + origin,
            direction.length() * ratio
        )
    }
}

class Raycast {
    fun raycast(triangle: TriangleFace, ray: Ray): RaycastResult {
        val edge1 = triangle.positions[1].point - triangle.positions[0].point
        val edge2 = triangle.positions[2].point - triangle.positions[0].point

        val normal = edge1.cross(edge2)

        var d = ray.direction.dot(normal)
        if (d == 0.0) return RaycastResult(false)

        val sign = sign(d).toInt()
        d = abs(d)

        //Calculate diatance between ray origin and triangle a point

        val diff = ray.origin - triangle.positions[0].point

        //Check if Ray pass the triangle
       /*val check1 = sign * ray.direction.dot(diff.cross(edge2, edge2))
        if (check1 < 0) return RaycastResult(false)
        val check2 = sign * ray.direction.dot(edge1.cross(diff, edge1))
        if (check2 < 0 || check1 + check2 > d) return RaycastResult(false)
*/
        //Calculate ratio
        val r = -sign * diff.dot(normal)
        //check if position is behind origin
        return if (r < 0) RaycastResult(false) else ray.at(r / d)
    }
}
