package com.codecad.core.brep

import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line

enum class Sense{
    None, CW, CCW;

    fun inverse() : Sense{
        return when(this){
            None -> None
            CW -> CCW
            CCW -> CW
        }
    }
}

class EdgeBound(val start: Vertex, val end : Vertex, val sense: Sense = Sense.None)

open class Edge(var curve: Curve, val bound : EdgeBound? = null){

    companion object{
        fun line(start: Vertex, end: Vertex) : Edge {
            return Edge(Line.fromTo(start.point, end.point), EdgeBound(start, end))
        }

        fun arc(workplane: WorkplaneExpr, start: Vertex, end: Vertex, sense: Sense) : Edge {
            return Edge(Circle(workplane, workplane.origin.distanceTo(start.point)), EdgeBound(start, end, sense))
        }
    }
}