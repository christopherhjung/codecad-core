package com.codecad.core.face

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.face.*
import java.util.*


fun findIntersection(line1: LineD, line2: LineD): PointD? {
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

    val a = 1 / (-s2_x * s1_y + s1_x * s2_y)
    val s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) * a
    val t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) * a

    val epsilon = 1e-5
    if (s - epsilon > 0 && s + epsilon < 1 && t - epsilon > 0 && t + epsilon < 1) {
        val x = p0_x + (t * s1_x)
        val y = p0_y + (t * s1_y)
        return PointD(x, y)
    }

    return null
}

val Comp2D = Comparator.comparing<PointD, Double> { it.x }.then(Comparator.comparing { it.y });
fun cutLines(lines: List<LineD>): List<LineD> {
    val events = events(lines)

    val sectionMap = HashMap<LineD, MutableList<PointD>>()
    fun addSection(line: LineD, pos : PointD ){
        sectionMap.computeIfAbsent(line){ mutableListOf() }.add(pos)
    }

    val actives = HashMap<LineD, Event>()
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

    val result = mutableListOf<LineD>()
    for( line in lines ){
        val sections = sectionMap[line]
        if( sections != null ){
            sections.sortWith(Comp2D)
            var left = line.p0
            for( split in sections ){
                result.add(LineD(left, split))
                left = split
            }
            result.add(LineD(left, line.p1))
        }else{
            result.add(line)
        }
    }

    return result
}
