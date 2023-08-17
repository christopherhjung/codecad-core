package com.codecad.core.face

/*

interface Sweep{
    fun hasNext() : Boolean
    fun next(): Vec2Expr
}

class PointSweep(val vec: Vec2Expr) : Sweep {
    var first = true

    override fun hasNext(): Boolean {
        return first
    }

    override fun next(): Vec2Expr {
        first = false
        return vec
    }
}

class LineSweep(val line: SketchSegment) : Sweep {
    var count = 0

    override fun hasNext(): Boolean {
        return count < 2
    }

    override fun next(): Vec2Expr {
        return when(count++){
            0 -> line.p0
            1 -> line.p1
            else -> throw Error("Line has only two points")
        }
    }
}

abstract class SegmentedSweep(val segments : Int) : Sweep{
    var count : Int = 0
    private val diff = 1.0 / segments

    abstract fun eval(t : Double) : Vec2Expr

    override fun hasNext(): Boolean {
        return count <= segments
    }

    override fun next(): Vec2Expr {
        val curr = count++
        return eval(when(curr){
            0 -> 0.0
            segments -> 1.0
            else -> diff * curr
        })
    }
}

class CircleSweep(val circle: Circle) : SegmentedSweep(100) {
    private val root = circle.position + Vec2Expr(circle.radius, circle.radius.world.ZERO)

    override fun eval(t: Double): Vec2Expr {
        return when(t){
            0.0, 1.0 -> root
            else -> {
                val currentAngle = 2 * Math.PI * t
                val x = (circle.position.x.evalDouble() + circle.radius.evalDouble() * cos(currentAngle))
                val y = (circle.position.y.evalDouble() + circle.radius.evalDouble() * sin(currentAngle))
                val world = circle.position.x.world
                return Vec2Expr(world.literal(x), world.literal(y))
            }
        }
    }
}

class ArcSweep(val arc: SketchArc) : SegmentedSweep(50) {
    val start : Double
    val diff: Double
    val p0: Vec2Expr
    val p1: Vec2Expr

    init{
        val p0Angle = arc.position.absoluteAngle(arc.p0)
        val p1Angle = arc.position.absoluteAngle(arc.p1)
        var end = if(arc.h.evalDouble() > 0.0){
            this.start = p0Angle
            p0 = arc.p0
            p1 = arc.p1
            p1Angle
        }else{
            this.start = p1Angle
            p0 = arc.p1
            p1 = arc.p0
            p0Angle
        }

        if(end < start) {
            end += 2 * Math.PI
        }

        diff = end - start
    }

    override fun eval(t: Double): Vec2Expr {
        return when(t){
            0.0 -> p0
            1.0 -> p1
            else -> {
                val currentAngle = start + diff * t
                val x = arc.radius.evalDouble() * cos(currentAngle)
                val y = arc.radius.evalDouble() * sin(currentAngle)
                val world = arc.position.x.world
                arc.position + Vec2Expr(world.literal(x), world.literal(y))
            }
        }
    }
}

class FunctionSweep(func: FunctionFigure) : SegmentedSweep(500) {
    private val t = ParamExpr(World(), 0.0)
    private val formula: Vec2Expr = func.function(t)

    override fun eval(t: Double): Vec2Expr {
        this.t.value = if(t == 1.0){ 0.0 }else t
        return formula.eval()
    }
}
*/