package com.codecad.core

import com.codecad.core.ast.primitive.ParamExpr
import kotlin.math.cos
import kotlin.math.sin

interface Plotter{
    fun getPoint(t: Double): Vec2
}

class LinePlotter(val line: Segment2) : Plotter {
    override fun getPoint(t: Double) : Vec2 {
        return (line.difference * t + line.p0).eval()
    }
}

class CirclePlotter(val circle: Circle) : Plotter {
    override fun getPoint(t: Double): Vec2 {
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

class ArcPlotter(val arc: Arc) : Plotter {
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

    override fun getPoint(t: Double): Vec2 {
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

class FunctionPlotter(func: FunctionFigure) : Plotter {
    private val t = ParamExpr(World(), 0.0)
    private val formula: Vec2 = func.function(t)

    override fun getPoint(t: Double): Vec2 {
        this.t.value = if(t == 1.0){ 0.0 }else t
        return formula.eval()
    }
}
