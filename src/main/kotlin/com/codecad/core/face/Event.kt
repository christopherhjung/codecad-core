package com.codecad.core.face

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line

const val OFFSET = 1e-8

data class Event(
    private val eventPos: Double,
    val edge: Edge<Vec2>,
    val origin: Boolean
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        return when{
            origin != other.origin -> other.origin.compareTo(origin)
            else -> eventPos.compareTo(other.eventPos)
        }
    }
}

fun lineBB(edge: Edge<Vec2>) : Pair<Double, Double>{
    val bound = edge.bound
    val start = bound.start.point
    val end = bound.end.point

    return if(start.x < end.x){
        Pair(start.x, end.x)
    }else{
        Pair(end.x, start.x)
    }
}

fun circleBB(edge: Edge<Vec2>) : Pair<Double, Double>{
    val bound = edge.bound.align()

    val circle = edge.curve as Circle
    val center = circle.workplane.origin
    val radius = circle.radius
    val start = bound.start.point - center
    val end = bound.end.point - center

    fun left(start: Vec2, end: Vec2) : Double{
        return if(start.y < 0.0 && start.x < end.x){
            start.x
        }else if(end.y >= 0.0){
            end.x
        }else{
            center.x - radius
        }
    }

    return Pair(left(start, end), left(start.negateX(), end.negateX()))
}

fun bb(edge: Edge<Vec2>) : Pair<Double, Double>{
    return when(edge.curve){
        is Line -> lineBB(edge)
        is Circle -> circleBB(edge)
        else -> throw RuntimeException("not impl")
    }
}

fun events(edges: List<Edge<Vec2>>) : List<Event>{
    val events = ArrayList<Event>()

    for (edge in edges) {
        val bb = bb(edge)
        events.add(Event(bb.first - OFFSET, edge, true))
        events.add(Event(bb.second + OFFSET, edge, false))
    }

    events.sort()
    return events
}