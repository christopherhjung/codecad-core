package com.codecad.core

import kotlin.math.cos
import kotlin.math.sin

interface Span{
    fun getPoint(t: Double): Point
}

class LineSpan(val line: LineSegment) : Span {
    override fun getPoint(t: Double) : Point {
        return (line.difference * t + line.p0).copy()
    }
}

class CircleSpan(val circle: Circle) : Span {
    override fun getPoint(t: Double): Point {
        if(t == 0.0 || t==1.0){
            return circle.center + Point(circle.radius, Literal.ZERO)
        }

        val currentAngle = 2 * Math.PI * t
        val x = (circle.center.x.evalDouble() + circle.radius.evalDouble() * cos(currentAngle))
        val y = (circle.center.y.evalDouble() + circle.radius.evalDouble() * sin(currentAngle))
        return Point(Literal(x), Literal(y))
    }
}

class ArcSpan(val arc: Arc) : Span {
    val start = arc.center.absoluteAngle(arc.p0)
    var end = arc.center.absoluteAngle(arc.p1)
    val diff: Double

    init{
        if(end < start){
            end += 2*Math.PI
        }
        diff = end - start
    }
    override fun getPoint(t: Double): Point {
        if(t == 0.0){
            return arc.p0
        }else if(t==1.0) {
            return arc.p1
        }

        val currentAngle = start + diff * t
        val x = (arc.center.x.evalDouble() + arc.radius.evalDouble() * cos(currentAngle))
        val y = (arc.center.y.evalDouble() + arc.radius.evalDouble() * sin(currentAngle))
        return Point(Literal(x), Literal(y))
    }
}

class FunctionSpan(func: FunctionFigure) : Span {
    private val t: Param = Param(0.0)
    private val formula: Point = func.function(t)

    override fun getPoint(t: Double): Point {

        this.t.value = if(t == 1.0){
            0.0
        }else t
        return formula.copy()
    }
}
