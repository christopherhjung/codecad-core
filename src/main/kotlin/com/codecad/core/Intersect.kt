package com.codecad.core

    import com.codecad.core.ast.vec.Vec2
    import kotlin.math.abs
    import kotlin.math.sqrt

object Intersect {
    private const val epsilon = 1e-8

    fun of(lhs: SketchEntity, rhs: SketchEntity) : List<Vec2>{
        return when(lhs){
            is SketchLine -> when(rhs){
                is SketchLine -> of(lhs, rhs)
                is SketchConic -> of(lhs, rhs)
                else -> null
            }

            is SketchConic -> when(rhs){
                is SketchLine -> of(rhs, lhs)
                is SketchConic -> of(lhs, rhs)
                else -> null
            }

            else -> null
        } ?: throw RuntimeException()
    }

    fun of(line1: SketchLine, line2: SketchLine): List<Vec2> {
        val s1 = line1.p1 - line1.p0
        val s2 = line2.p1 - line2.p0
        val sd = line1.p0 - line2.p0

        val a = s1.crossZ(s2)
        if(a < epsilon) return emptyList()

        fun insideUnitInterval(value: Double) : Boolean{
            return value - epsilon > 0.0 && value + epsilon < 1.0
        }

        val s = s1.crossZ(sd) / a
        if( insideUnitInterval(s) ) {
            val t = s2.crossZ(sd) / a
            if(insideUnitInterval(t)){
                return listOf(line1.p0 + s1 * t)
            }
        }

        return emptyList()
    }

    fun of(lhs: SketchLine, rhs: SketchConic) : List<Vec2>{
        return ofLineCircle(lhs, rhs).filter {
            lhs.inside(it) && rhs.inside(it)
        }
    }

    private fun ofLineCircle(line: SketchLine, circle: SketchConic) : List<Vec2>{
        val r1 = circle.radius
        val start2c = circle.center - line.p0
        val dir = (line.p1 - line.p0).normalized()
        val l2projC = line.p0 + Vec2.project(start2c, dir)
        val c2l = l2projC - circle.center
        val c2lDistance = c2l.length() - r1

        return if(c2lDistance > epsilon){
            emptyList()
        }else{
            if(abs(c2lDistance) < epsilon){
                if(line.inside(l2projC)){
                    listOf(l2projC)
                }else{
                    emptyList()
                }
            }else{
                val h = dir * sqrt(r1 * r1 - c2l.squaredLength())
                val first = l2projC - h
                val second = l2projC + h
                return listOf(first, second)
            }
        }
    }

    fun of(lhs: SketchConic, rhs: SketchConic) : List<Vec2>{
        return ofCircles(lhs, rhs).filter {
            lhs.inside(it) && rhs.inside(it)
        }
    }

    private fun ofCircles(lhs: SketchConic, rhs: SketchConic) : List<Vec2>{
        val r1 = lhs.radius
        val r2 = rhs.radius
        val p1 = lhs.center
        val p2 = rhs.center
        val distance = p2.distance(p1)
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