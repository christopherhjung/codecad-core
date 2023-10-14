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

    companion object{
        fun new(eventPos: Double, edge: Edge<Vec2>, origin: Boolean) : Event{
            val dir = if(origin){ -1 }else{ 1 }
            val offset = eventPos + dir * OFFSET
            return Event(offset, edge, origin)
        }
    }
}

fun isForward(start: Vec2, end : Vec2) : Boolean{
    return when{
        start.x < end.x -> true
        start.x > end.x -> false
        else -> start.y < end.y
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

fun events(edges: List<Edge<Vec2>>) : List<Event>{
    val events = ArrayList<Event>()

    fun addEvent(first : Vec2, second : Vec2, edge : Edge<Vec2>){
        val orient = isForward(first, second)
        events.add(Event.new(first.x, edge, orient))
        events.add(Event.new(second.x, edge, !orient))
    }

    for (edge in edges) {
        when(edge.curve){
            is Line -> edge.bound.let { addEvent(it.start.point, it.end.point, edge) }
            is Circle -> {
                val bb = circleBB(edge)

                events.add(Event.new(bb.first, edge, true))
                events.add(Event.new(bb.second, edge, false))
            }
        }
    }
    events.sort()
    return events
}