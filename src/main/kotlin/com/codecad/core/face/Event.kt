package com.codecad.core.face

import com.codecad.common.LineD
import com.codecad.common.PointD


data class Event(
    val pos: PointD,
    val line: LineD,
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

fun normalizeLine(line : LineD) : LineD {
    return if (line.p0.x > line.p1.x) {
        LineD(line.p1, line.p0)
    }else{
        line
    }
}

fun isForward(line: LineD) : Boolean{
    return if(line.p0.x < line.p1.x){
        true
    }else if(line.p0.x > line.p1.x){
        false
    }else{
        line.p0.y < line.p1.y
    }
}

fun events(lines: List<LineD>) : List<Event>{
    val events = ArrayList<Event>()
    for (line in lines) {
        val forwards = isForward(line)
        events.add(Event(line.p0, line, forwards))
        events.add(Event(line.p1, line, !forwards))
    }
    events.sort()
    return events
}