package com.codecad.core.brep

import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line

enum class Sense{
    Same/*CW*/, Opposite;/*CCW*/

    fun invert() : Sense{
        return when(this){
            Same -> Opposite
            Opposite -> Same
        }
    }
}

class EdgeBound(val start: Vertex, val end : Vertex, val sense: Sense = Sense.Same){
    init {
        assert(start !== end)
    }
}

class Edge(var curve: Curve, val bound : EdgeBound? = null){
    companion object{
        fun line(start: Vertex, end: Vertex) : Edge {
            return Edge(Line.fromTo(start.point, end.point), EdgeBound(start, end))
        }

        fun arc(workplane: Workplane, start: Vertex, end: Vertex, sense: Sense) : Edge {
            return Edge(Circle(workplane, workplane.origin.distanceTo(start.point)), EdgeBound(start, end, sense))
        }
    }
}