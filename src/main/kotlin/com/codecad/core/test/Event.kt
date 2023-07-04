package com.codecad.core.test

import com.codecad.common.LineD
import com.codecad.common.PointD


data class Event(
    val p: PointD,
    val line: LineD,
    val origin: Boolean
) : Comparable<Event> {
    override fun compareTo(other: Event): Int {
        return when{
            origin != other.origin -> other.origin.compareTo(origin)
            p.x == other.p.x -> p.y.compareTo(other.p.y)
            else -> p.x.compareTo(other.p.x)
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

fun events(lines: List<LineD>) : List<Event>{
    val lines = lines.map { normalizeLine(it) }
    val events = ArrayList<Event>()
    for (line in lines) {
        events.add(Event(line.p0, line, true))
        events.add(Event(line.p1, line, false))
    }
    events.sort()
    return events
}