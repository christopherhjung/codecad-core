package com.codecad.core

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object Intersect {
    fun of(line1: LineSegment, line2: LineSegment): Vec2? {
        val (p0_x, p0_y) = line1.p0
        val (p2_x, p2_y) = line2.p0

        val s1 = line1.p1 - line1.p0
        val s2 = line2.p1 - line2.p0

        val a = 1.0 / s1.crossZ(s2)
        val s = (-s1.y * (p0_x - p2_x) + s1.x * (p0_y - p2_y)) * a
        val t = (s2.x * (p0_y - p2_y) - s2.y * (p0_x - p2_x)) * a

        val epsilon = 1e-5
        if (s - epsilon > 0 && s + epsilon < 1 && t - epsilon > 0 && t + epsilon < 1) {
            return line1.p0 + s1 * t
        }

        return null
    }

    fun of(lhs: Circle2d, rhs: Circle2d) : Array<Vec2>{
        val r1 = lhs.radius
        val r2 = rhs.radius
        val p1 = lhs.center
        val p2 = rhs.center
        val distance = p2.distance(p1)
        val radiusSum = r1 + r2
        return if(distance > radiusSum){
            emptyArray()
        }else {
            val dir = (p2 - p1) / distance

            if(abs(distance - radiusSum) < 1e-10){
                arrayOf(p1 + dir * r1)
            }else{
                val a = 0.5 * (r1*r1 - r2*r2 + distance*distance) / distance
                val p3 = p1 + dir * a
                val h = dir * sqrt(r1*r1 - a*a)
                val i1 = p3.rightTurn(h)
                val i2 = p3.leftTurn(h)
                arrayOf(i1, i2)
            }
        }
    }
}