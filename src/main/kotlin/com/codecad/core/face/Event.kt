package com.codecad.core.face

import com.codecad.core.SketchArc
import com.codecad.core.SketchCircle
import com.codecad.core.SketchLine
import com.codecad.core.SketchEntity
import com.codecad.core.ast.vec.Vec2


data class Event(
    val pos: Vec2,
    val entity: SketchEntity,
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

fun isForward(line: SketchLine) : Boolean{
    return when{
        line.p0.x < line.p1.x -> true
        line.p0.x > line.p1.x -> false
        else -> line.p0.y < line.p1.y
    }
}

fun events(figures: List<SketchEntity>) : List<Event>{
    val events = ArrayList<Event>()
    for (figure in figures) {
        when(figure){
            is SketchLine -> {
                val first = isForward(figure)
                events.add(Event(figure.p0, figure, first))
                events.add(Event(figure.p1, figure, !first))
            }

            is SketchCircle -> {
                events.add(Event(figure.center - Vec2.DirX * figure.radius, figure, true))
                events.add(Event(figure.center + Vec2.DirX * figure.radius, figure, false))
            }

            is SketchArc -> {
                events.add(Event(figure.center - Vec2.DirX * figure.radius, figure, true))
                events.add(Event(figure.center + Vec2.DirX * figure.radius, figure, false))
            }
        }
    }
    events.sort()
    return events
}