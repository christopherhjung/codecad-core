package com.codecad.core

import com.codecad.core.parser.ast.ParamExpr
import com.codecad.core.sketch.World
import kotlin.math.cos
import kotlin.math.sin

interface Plotter{
    fun getPoint(t: Double): Point2
}

class LinePlotter(val line: Segment2) : Plotter {
    override fun getPoint(t: Double) : Point2 {
        return (line.difference * t + line.p0).eval()
    }
}

class CirclePlotter(val circle: Circle) : Plotter {
    override fun getPoint(t: Double): Point2 {
        if(t == 0.0 || t==1.0){
            return circle.center + Point2(circle.radius, circle.radius.world.ZERO)
        }

        val currentAngle = 2 * Math.PI * t
        val x = (circle.center.x.evalDouble() + circle.radius.evalDouble() * cos(currentAngle))
        val y = (circle.center.y.evalDouble() + circle.radius.evalDouble() * sin(currentAngle))
        val world = circle.center.x.world
        return Point2(world.literal(x), world.literal(y))
    }
}

class ArcPlotter(val arc: Arc) : Plotter {
    val start = arc.center.absoluteAngle(arc.p0)
    var end = arc.center.absoluteAngle(arc.p1)
    val diff: Double

    init{
        if(end < start){
            end += 2*Math.PI
        }
        diff = end - start
    }
    override fun getPoint(t: Double): Point2 {
        if(t == 0.0){
            return arc.p0
        }else if(t==1.0) {
            return arc.p1
        }

        val currentAngle = start + diff * t
        val x = (arc.center.x.evalDouble() + arc.radius.evalDouble() * cos(currentAngle))
        val y = (arc.center.y.evalDouble() + arc.radius.evalDouble() * sin(currentAngle))
        val world = arc.center.x.world
        return Point2(world.literal(x), world.literal(y))
    }
}

class FunctionPlotter(func: FunctionFigure) : Plotter {
    private val t = ParamExpr(World(), 0.0)
    private val formula: Point2 = func.function(t)

    override fun getPoint(t: Double): Point2 {
        this.t.value = if(t == 1.0){ 0.0 }else t
        return formula.eval()
    }
}
