package com.codecad.core.sketch

import com.codecad.core.LineSegment
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.face.Event
import com.codecad.core.face.events


fun findIntersection(line1: LineSegment, line2: LineSegment): Vec2? {
    val p0_x = line1.p0.x
    val p0_y = line1.p0.y
    val p1_x = line1.p1.x
    val p1_y = line1.p1.y
    val p2_x = line2.p0.x
    val p2_y = line2.p0.y
    val p3_x = line2.p1.x
    val p3_y = line2.p1.y

    val s1_x = p1_x - p0_x
    val s1_y = p1_y - p0_y
    val s2_x = p3_x - p2_x
    val s2_y = p3_y - p2_y

    val a = 1.0 / (s1_x * s2_y - s2_x * s1_y)
    val s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) * a
    val t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) * a

    val epsilon = 1e-5
    if (s - epsilon > 0 && s + epsilon < 1 && t - epsilon > 0 && t + epsilon < 1) {
        val x = p0_x + (t * s1_x)
        val y = p0_y + (t * s1_y)
        return Vec2(x, y)
    }

    return null
}

val Comp2D = Comparator.comparing<Vec2, Double> { it.x }.then(Comparator.comparing { it.y });
fun cutLines(lines: List<LineSegment>): List<LineSegment> {
    val events = events(lines)

    val sectionMap = HashMap<LineSegment, MutableList<Vec2>>()
    fun addSection(line: LineSegment, pos : Vec2 ){
        sectionMap.computeIfAbsent(line){ mutableListOf() }.add(pos)
    }

    val actives = HashMap<LineSegment, Event>()
    for (event in events) {
        if (event.origin) {
            for (active in actives.values) {
                findIntersection(active.line, event.line)?.let {
                    addSection(event.line, it)
                    addSection(active.line, it)
                }
            }

            actives[event.line] = event
        } else {
            actives.remove(event.line)
        }
    }

    val result = mutableListOf<LineSegment>()
    for( line in lines ){
        val sections = sectionMap[line]
        if( sections != null ){
            sections.add(line.p0)
            sections.add(line.p1)
            sections.sortWith(Comp2D)
            for((lhs, rhs) in sections.zipWithNext()){
                result.add(LineSegment(lhs, rhs))
            }
        }else{
            result.add(line)
        }
    }

    return result
}
