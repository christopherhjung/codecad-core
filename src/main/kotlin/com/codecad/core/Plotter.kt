package com.codecad.core

import com.codecad.core.ast.primitive.ParamExpr
import kotlin.math.cos
import kotlin.math.sin


interface Plotter{
    fun hasNext() : Boolean
    fun next(): Vec2
}

class PointPlotter(val vec: Vec2) : Plotter {
    var first = true

    override fun hasNext(): Boolean {
        return first
    }

    override fun next(): Vec2 {
        first = false
        return vec
    }
}

class LinePlotter(val line: Segment2) : Plotter {
    var count = 0

    override fun hasNext(): Boolean {
        return count < 2
    }

    override fun next(): Vec2 {
        return when(count++){
            0 -> line.p0
            1 -> line.p1
            else -> throw Error("Line has only two points")
        }
    }
}

abstract class SegmentedPlotter(val segments : Int) : Plotter{
    var count : Int = 0
    private val diff = 1.0 / segments

    abstract fun eval(t : Double) : Vec2

    override fun hasNext(): Boolean {
        return count <= segments
    }

    override fun next(): Vec2 {
        val curr = count++
        return eval(when(curr){
            0 -> 0.0
            segments -> 1.0
            else -> diff * curr
        })
    }
}

class CirclePlotter(val circle: Circle) : SegmentedPlotter(500) {
    override fun eval(t: Double): Vec2 {
        return when(t){
            0.0, 1.0 -> circle.center + Vec2(circle.radius, circle.radius.world.ZERO)
            else -> {
                val currentAngle = 2 * Math.PI * t
                val x = (circle.center.x.evalDouble() + circle.radius.evalDouble() * cos(currentAngle))
                val y = (circle.center.y.evalDouble() + circle.radius.evalDouble() * sin(currentAngle))
                val world = circle.center.x.world
                return Vec2(world.literal(x), world.literal(y))
            }
        }
    }
}

class ArcPlotter(val arc: Arc) : SegmentedPlotter(200) {
    val start : Double
    val diff: Double
    val p0: Vec2
    val p1: Vec2

    init{
        val p0Angle = arc.center.absoluteAngle(arc.p0)
        val p1Angle = arc.center.absoluteAngle(arc.p1)
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

    override fun eval(t: Double): Vec2 {
        return when(t){
            0.0 -> p0
            1.0 -> p1
            else -> {
                val currentAngle = start + diff * t
                val x = arc.radius.evalDouble() * cos(currentAngle)
                val y = arc.radius.evalDouble() * sin(currentAngle)
                val world = arc.center.x.world
                arc.center + Vec2(world.literal(x), world.literal(y))
            }
        }
    }
}

class FunctionPlotter(func: FunctionFigure) : SegmentedPlotter(500) {
    private val t = ParamExpr(World(), 0.0)
    private val formula: Vec2 = func.function(t)

    override fun eval(t: Double): Vec2 {
        this.t.value = if(t == 1.0){ 0.0 }else t
        return formula.eval()
    }
}
