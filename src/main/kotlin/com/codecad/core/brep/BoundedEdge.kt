package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec
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

class EdgeBound<T : Vec<T>>(val start: Vertex<T>, val end : Vertex<T>, val sense: Sense = Sense.Same){
    init {
        assert(start !== end)
    }
}

class Edge<T : Vec<T>>(var curve: Curve<T>, val bound : EdgeBound<T>? = null){
    companion object{
        fun <T : Vec<T>> line(start: Vertex<T>, end: Vertex<T>) : Edge<T> {
            return Edge(Line.fromTo(start.point, end.point), EdgeBound(start, end))
        }

        fun <T : Vec<T>> arc(workplane: Workplane<T>, start: Vertex<T>, end: Vertex<T>, sense: Sense) : Edge<T> {
            return Edge(Circle(workplane, workplane.origin.distanceTo(start.point)), EdgeBound(start, end, sense))
        }
    }
}