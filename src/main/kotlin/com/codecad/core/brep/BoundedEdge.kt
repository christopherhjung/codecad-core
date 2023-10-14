package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
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

    fun align() : EdgeBound<T>{
        return if(sense == Sense.Same){
            this
        }else{
            EdgeBound(end, start, Sense.Same)
        }
    }
}

class Marker
class Edge<T : Vec<T>>(var curve: Curve<T>, val bound : EdgeBound<T>){

    companion object{
        fun <T : Vec<T>> line(start: Vertex<T>, end: Vertex<T>) : Edge<T> {
            return Edge(Line.fromTo(start.point, end.point), EdgeBound(start, end))
        }

        fun <T : Vec<T>> line(start: T, end: T) : Edge<T> {
            return line(Vertex(start), Vertex(end))
        }

        fun <T : Vec<T>> circle(workplane: Workplane<T>, radius: Double) : Edge<T> {
            val outerPoint = Vertex( workplane.origin + workplane.x * radius )
            return Edge(Circle(workplane, radius), EdgeBound(outerPoint, outerPoint, Sense.Same))
        }

        fun circle(center: Vec2, radius: Double) : Edge<Vec2> {
            return circle(Workplane(center, Vec2.DirY, Vec2.DirX), radius)
        }

        fun <T : Vec<T>> arc(workplane: Workplane<T>, start: Vertex<T>, end: Vertex<T>, sense: Sense) : Edge<T> {
            return Edge(Circle(workplane, workplane.origin.distanceTo(start.point)), EdgeBound(start, end, sense))
        }

        fun arc(center: Vec2, start: Vec2, end: Vec2, sense: Sense) : Edge<Vec2> {
            return arc(Workplane(center, Vec2.DirY, Vec2.DirX), Vertex(start), Vertex(end), sense)
        }
    }
}


