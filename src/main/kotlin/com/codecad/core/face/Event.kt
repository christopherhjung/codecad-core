package com.codecad.core.face

import com.codecad.core.LineSegment
import com.codecad.core.ast.vec.Vec2


data class Event(
    val pos: Vec2,
    val line: LineSegment,
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

fun isForward(line: LineSegment) : Boolean{
    return when{
        line.p0.x < line.p1.x -> true
        line.p0.x > line.p1.x -> false
        else -> line.p0.y < line.p1.y
    }
}

fun events(lines: List<LineSegment>) : List<Event>{
    val events = ArrayList<Event>()
    for (line in lines) {
        val forwards = isForward(line)
        events.add(Event(line.p0, line, forwards))
        events.add(Event(line.p1, line, !forwards))
    }
    events.sort()
    return events
}