package com.codecad.core

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import kotlin.math.pow

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class Entity(vararg val params: Expr)

class LineSegmentExpr(val p0: Vec2Expr, val p1: Vec2Expr) : Entity(p0.x, p0.y, p1.x, p1.y) {
    val squaredLength : Expr
        get() = ((p1.x - p0.x).pow(2) + (p1.y - p0.y).pow(2))

    val length : Expr
        get() = squaredLength.sqrt()

    val midPoint : Vec2Expr
        get() = (p0 + p1) / 2.0

    val difference : Vec2Expr
        get() = p1 - p0

    val direction : Vec2Expr
        get() = difference.normalized()
}

class LineSegment(var p0: Vec2, var p1: Vec2) {
    val squaredLength : Double
        get() = ((p1.x - p0.x).pow(2.0) + (p1.y - p0.y).pow(2.0))

    val length : Double
        get() = kotlin.math.sqrt(squaredLength)

    val midPoint : Vec2
        get() = (p0 + p1) / 2.0

    val difference : Vec2
        get() = p1 - p0

    val direction : Vec2
        get() = difference.normalized()
}

abstract class Conic2d(val center : Vec2, val radius: Double)
class Circle2d(center : Vec2, radius: Double) : Conic2d(center, radius)
class Arc2d(val p0 : Vec2, val p1: Vec2, center : Vec2) : Conic2d(center, (center - p0).length())

abstract class SketchConic(vararg params: Expr) : Entity(*params){
    abstract val center : Vec2Expr
    abstract val radius : Expr
}

open class SketchCircle(override val center: Vec2Expr, override val radius: Expr) :
    SketchConic(center.x, center.y, radius)

class SketchArc(val p0: Vec2Expr, val p1: Vec2Expr, private val h: Expr) : SketchConic(p0.x, p0.y, p1.x, p1.y, h){
    private val radiusSign: Expr = run{
        val s = (p1 - p0).length()
        (h.pow(2) * 4 + s.pow(2)) / (h * 8)
    }

    override val radius: Expr = run{
        Expr.abs(radiusSign)
    }

    override val center: Vec2Expr = run{
        val direction = p1 - p0
        val middle = (p0 + p1) * 0.5
        val normalizedDirection = direction.normalized()
        val positive = p0.world.vec2(normalizedDirection.y, -normalizedDirection.x)
        middle + positive * (h - radiusSign)
    }
}

