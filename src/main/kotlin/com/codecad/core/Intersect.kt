package com.codecad.core

import com.codecad.core.ast.vec.Vec2
import kotlin.math.abs
import kotlin.math.sqrt

object Intersect {
    private const val epsilon = 1e-8
    private fun insideUnitInterval(value: Double) : Boolean{
        return value - epsilon > 0.0 && value + epsilon < 1.0
    }

    fun of(line1: LineSegment, line2: LineSegment): Vec2? {
        val s1 = line1.p1 - line1.p0
        val s2 = line2.p1 - line2.p0
        val sd = line1.p0 - line2.p0

        val a = s1.crossZ(s2)
        if(a < epsilon) return null

        val s = s1.crossZ(sd) / a
        if( insideUnitInterval(s) ) {
            val t = s2.crossZ(sd) / a
            if(insideUnitInterval(t)){
                return line1.p0 + s1 * t
            }
        }

        return null
    }

    fun of(line: LineSegment, circle: Circle2d) : Array<Vec2>{
        val r1 = circle.radius
        val start2c = circle.center - line.p0
        val lineDir = line.p1 - line.p0
        val l2projC = Vec2.project(start2c, lineDir)
        val c2l = l2projC - circle.center
        val c2lDistance = c2l.length() - r1
        return if(c2lDistance >= 0.0){
            emptyArray()
        }else{
            val p3 = line.p0 + l2projC
            if(abs(c2lDistance) < epsilon){
                arrayOf(p3)
            }else{
                val h = lineDir * sqrt(r1 * r1 - c2l.squaredLength())
                arrayOf(p3 - h, p3 + h)
            }
        }
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

            if(abs(distance - radiusSum) < epsilon){
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