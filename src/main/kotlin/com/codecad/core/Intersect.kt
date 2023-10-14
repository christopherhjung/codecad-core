package com.codecad.core

    import com.codecad.core.ast.vec.Vec2
    import com.codecad.core.brep.Edge
    import com.codecad.core.brep.Sense
    import com.codecad.core.brep.curve.Circle
    import com.codecad.core.brep.curve.Line
    import kotlin.math.abs
    import kotlin.math.sqrt

const val EPSILON = 1e-10
fun Edge<Vec2>.inside(p : Vec2) : Boolean{
    return when(val curve = curve){
        is Line -> {
            val p0 = bound.start.point
            val p1 = bound.end.point
            ((p0.x - EPSILON <= p.x) == (p.x <= p1.x + EPSILON)) &&
            ((p0.y - EPSILON <= p.y) == (p.y <= p1.y + EPSILON))
        }
        is Circle -> {
            val center = curve.workplane.origin
            val alignedBound = bound.align()

            val start = alignedBound.start.point
            val end = alignedBound.end.point

            val p0 = start - center
            val p1 = end - center
            val cmp = Vec2.rotaryCmp(p0, p - center, p1)
            return cmp != 1
        }
        else -> true
    }
}

object Intersect {
    private const val epsilon = 1e-8

    fun hasCommonVertex(lhs: Edge<Vec2>, rhs: Edge<Vec2>) : Boolean{
        val lhsBound = lhs.bound
        val rhsBound = rhs.bound

        when(lhsBound.start){
            rhsBound.start, rhsBound.end -> return true
        }

        when(lhsBound.end){
            rhsBound.start, rhsBound.end -> return true
        }

        return false
    }

    fun of(lhs: Edge<Vec2>, rhs: Edge<Vec2>) : List<Vec2>{
        if(hasCommonVertex(lhs, rhs)) return emptyList()

        val lhsCurve = lhs.curve
        val rhsCurve = rhs.curve

        return when(lhsCurve){
            is Line -> when(rhsCurve){
                is Line -> ofLineLine(lhsCurve, rhsCurve)
                is Circle -> ofLineCircle(lhsCurve, rhsCurve)
                else -> throw RuntimeException()
            }

            is Circle -> when(rhsCurve){
                is Line -> ofLineCircle(rhsCurve, lhsCurve)
                is Circle -> ofCircles(lhsCurve, rhsCurve)
                else -> throw RuntimeException()
            }

            else -> throw RuntimeException()
        }.filter {
            lhs.inside(it) && rhs.inside(it)
        }
    }

    private fun ofLineLine(line1: Line<Vec2>, line2: Line<Vec2>): List<Vec2> {
        val s1 = line1.direction
        val s2 = line2.direction
        val sd = line1.origin - line2.origin

        val a = s1.crossZ(s2)
        if(abs(a) < epsilon) return emptyList()

        val t = s2.crossZ(sd) / a
        return listOf(line1.origin + s1 * t)
    }

    private fun ofLineCircle(line: Line<Vec2>, circle: Circle<Vec2>) : List<Vec2>{
        val r1 = circle.radius
        val center = circle.workplane.origin
        val start2c = center - line.origin
        val dir = line.direction
        val l2projC = line.origin + Vec2.project(start2c, dir)
        val c2l = l2projC - center
        val c2lDistance = c2l.length() - r1

        return if(c2lDistance > epsilon){
            emptyList()
        }else{
            if(abs(c2lDistance) < epsilon){
                listOf(l2projC)
            }else{
                val h = dir * sqrt(r1 * r1 - c2l.squaredLength())
                val first = l2projC - h
                val second = l2projC + h
                return listOf(first, second)
            }
        }
    }

    private fun ofCircles(lhs: Circle<Vec2>, rhs: Circle<Vec2>) : List<Vec2>{
        val r1 = lhs.radius
        val r2 = rhs.radius
        val p1 = lhs.workplane.origin
        val p2 = rhs.workplane.origin
        val distance = p2.distanceTo(p1)
        val radiusSum = r1 + r2
        return if(distance > radiusSum){
            emptyList()
        }else {
            val dir = (p2 - p1) / distance

            if(abs(distance - radiusSum) < epsilon){
                listOf(p1 + dir * r1)
            }else{
                val a = 0.5 * (r1*r1 - r2*r2 + distance*distance) / distance
                val p3 = p1 + dir * a
                val h = dir * sqrt(r1*r1 - a*a)
                val i1 = p3.rightTurn(h)
                val i2 = p3.leftTurn(h)
                listOf(i1, i2)
            }
        }
    }
}