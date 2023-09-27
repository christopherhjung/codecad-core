package com.codecad.core

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec2Expr
import kotlin.math.atan2
import kotlin.math.pow

enum class LineType(val prio: Int){
    Normal(2), Construction(3)
}

abstract class SketchEntityExpr(vararg val params: Expr){
    abstract fun eval() : SketchEntity
}
abstract class SketchEntity(){
    abstract fun inside(p : Vec2) : Boolean
}

class SketchLineExpr(val p0: Vec2Expr, val p1: Vec2Expr) : SketchEntityExpr(p0.x, p0.y, p1.x, p1.y) {
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

    override fun eval(): SketchEntity {
        return SketchLine(p0.eval(), p1.eval())
    }
}

class SketchLine(var p0: Vec2, var p1: Vec2) : SketchEntity(){
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

    override fun inside(p : Vec2) : Boolean{
        return ((p0.x <= p.x) == (p.x <= p1.x)) && ((p0.y <= p.y) == (p.y <= p1.y))
    }
}



abstract class SketchConic(val center : Vec2, val radius: Double) : SketchEntity()
class SketchCircle(center : Vec2, radius: Double) : SketchConic(center, radius){
    override fun inside(p: Vec2): Boolean {
        return true
    }
}
class SketchArc(val p0 : Vec2, val p1: Vec2, center : Vec2) : SketchConic(center, (center - p0).length()){
    private val p0Dir = p0 - center
    private val p1Dir = p1 - center

    override fun inside(p : Vec2) : Boolean{
        val cmp = Vec2.rotaryCmp(p0Dir, p - center, p1Dir)
        return cmp != 1
    }
}

abstract class SketchConicExpr(vararg params: Expr) : SketchEntityExpr(*params){
    abstract val center : Vec2Expr
    abstract val radius : Expr
}

open class SketchCircleExpr(override val center: Vec2Expr, override val radius: Expr) :
    SketchConicExpr(center.x, center.y, radius){
    override fun eval(): SketchEntity {
        return SketchCircle(center.eval(), radius.evalDouble())
    }
}

class SketchArcExpr(val p0: Vec2Expr, val p1: Vec2Expr, private val h: Expr) : SketchConicExpr(p0.x, p0.y, p1.x, p1.y, h){
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

    override fun eval(): SketchEntity {
        return SketchArc(p0.eval(), p1.eval(), center.eval())
    }
}

