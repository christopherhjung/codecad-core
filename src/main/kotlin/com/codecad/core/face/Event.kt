package com.codecad.core.face

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.EdgeBound
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line


data class Event(
    val pos: Vec2,
    val edge: Edge<Vec2>,
    val origin: Boolean
) : Comparable<Event> {
    override fun compareTo(other: Event): Int {
        return when{
            origin != other.origin -> other.origin.compareTo(origin)
            pos.x == other.pos.x -> pos.y.compareTo(other.pos.y)
            else -> pos.x.compareTo(other.pos.x)
        }
    }
}

fun isForward(edge: EdgeBound<Vec2>) : Boolean{
    val start = edge.start.point
    val end = edge.start.point

    return when{
        start.x < end.x -> true
        start.x > end.x -> false
        else -> start.y < end.y
    }
}

fun events(edges: List<Edge<Vec2>>) : List<Event>{
    val events = ArrayList<Event>()
    for (edge in edges) {
        val curve = edge.curve
        when(curve){
            is Line -> {
                val bound = edge.bound!!
                val first = isForward(bound)
                events.add(Event(bound.start.point, edge, first))
                events.add(Event(bound.end.point, edge, !first))
            }

            is Circle -> {
                val center = curve.workplane.origin
                val radius = curve.radius

                events.add(Event(center - Vec2.DirX * radius, edge, true))
                events.add(Event(center + Vec2.DirX * radius, edge, false))
            }
        }
    }
    events.sort()
    return events
}