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
    fun isUnbounded() : Boolean {
        return start === end
    }

    fun align() : EdgeBound<T>{
        return if(sense == Sense.Same){
            this
        }else{
            EdgeBound(end, start, Sense.Same)
        }
    }

    fun invert() : EdgeBound<T>{
        return EdgeBound(end, start, sense.invert())
    }

    override fun toString(): String {
        return "EdgeBound(start=$start, end=$end)"
    }
}

class Marker
class Edge<T : Vec<T>>(var curve: Curve<T>, val bound : EdgeBound<T>){

    override fun toString(): String {
        return "Edge($bound)"
    }

    companion object{
        fun <T : Vec<T>> line(start: Vertex<T>, end: Vertex<T>) : Edge<T> {
            return Edge(Line.fromTo(start.point, end.point), EdgeBound(start, end))
        }

        fun <T : Vec<T>> line(start: T, end: T) : Edge<T> {
            return line(Vertex(start), Vertex(end))
        }

        fun <T : Vec<T>> circle(workplane: Workplane<T>, radius: Double, sense: Sense = Sense.Same) : Edge<T> {
            val outerPoint = Vertex( workplane.origin + workplane.x * radius )
            return Edge(Circle(workplane, radius), EdgeBound(outerPoint, outerPoint, sense))
        }

        fun circle(center: Vec2, radius: Double, sense: Sense = Sense.Same) : Edge<Vec2> {
            return circle(Workplane(center, Vec2.DirY, Vec2.DirX), radius, sense)
        }

        fun <T : Vec<T>> arc(workplane: Workplane<T>, start: Vertex<T>, end: Vertex<T>, sense: Sense) : Edge<T> {
            return Edge(Circle(workplane, workplane.origin.distanceTo(start.point)), EdgeBound(start, end, sense))
        }

        fun arc(center: Vec2, start: Vec2, end: Vec2, sense: Sense) : Edge<Vec2> {
            return arc(Workplane(center, Vec2.DirY, Vec2.DirX), Vertex(start), Vertex(end), sense)
        }
    }
}

fun Loop<Vec2>.length() : Double{
    var length = 0.0

    for( loop in this ){
        length += loop.edge.edge.length()
    }

    return length
}
fun Edge<Vec2>.length() : Double{
    val start = bound.start
    val end = bound.end
    return when(val curve = curve){
        is Line -> start.point.distanceTo(end.point)
        is Circle -> {
            val radius = curve.radius
            val angle = if (start === end){
                2.0 * Math.PI
            }else{
                val center = curve.workplane.origin
                val startDiff = start.point - center
                val endDiff = end.point - center

                if(bound.sense == Sense.Same){
                    startDiff.angleTo(endDiff)
                }else{
                    endDiff.angleTo(startDiff)
                }
            }

            angle * radius
        }
        else -> throw NotImplementedError()
    }
}
