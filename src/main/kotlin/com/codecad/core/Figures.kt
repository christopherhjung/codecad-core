package com.codecad.core

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}
/*
interface Figure{
    fun plotter() : Sweep{
        throw NotImplementedError("Not implemented plotter")
    }
}*/

class SketchSegment(val p0: Vec2Expr, val p1: Vec2Expr) {
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

    /*
    override fun plotter(): Sweep {
        return LineSweep(this)
    }*/
}

interface SketchConic{
    val center : Vec2Expr
    val radius : Expr
}

open class SketchCircle(override val center: Vec2Expr, override val radius: Expr) :
    SketchConic

class SketchArc(val p0: Vec2Expr, val p1: Vec2Expr, val h: Expr) : SketchConic{
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
/*
class FunctionFigure( val function: (Expr) -> Vec2Expr) : Figure{
    override fun plotter(): Sweep {
        return FunctionSweep(this)
    }
}*/

