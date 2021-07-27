package com.codecad.core

import kotlin.math.cos
import kotlin.math.sin

interface Span{
    fun getPoint(t: Double): Point
}

class LineSpan(val line: Line) : Span {
    override fun getPoint(t: Double) : Point {
        return (line.difference * t + line.p0).copy()
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
        val x = (arc.center.x.value + arc.radius.value * cos(currentAngle))
        val y = (arc.center.y.value + arc.radius.value * sin(currentAngle))
        return Point(Const(x), Const(y))
    }
}

class FunctionSpan(func: FunctionFigure) : Span {
    private val t: Parameter = Parameter(0.0)
    private val formula: Point = func.function(t)

    override fun getPoint(t: Double): Point {

        this.t.value = if(t == 1.0){
            0.0
        }else t
        return formula.copy()
    }
}
