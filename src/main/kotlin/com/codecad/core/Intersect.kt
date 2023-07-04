package com.codecad.core

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.test.Corner
import com.codecad.core.test.Edge
import com.codecad.core.test.generateFaces
import java.util.*
import kotlin.collections.ArrayList


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

fun normalizeLine(line : LineD) : LineD{
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



fun findFaces(lines: List<LineD>): List<PolygonFace> {
    val sections = cutLines(lines)

    val pointMap = HashMap<PointD, Corner>()
    val edges = HashSet<Edge>()

    fun corner(point: PointD) : Corner{
        return pointMap.computeIfAbsent(point) { Corner(it) }
    }

    for (section in sections) {
        val left = corner(section.p0)
        val right = corner(section.p1)

        val a = Edge(left, right)
        val b = Edge(right, left)

        a.twin = b
        b.twin = a

        left.edges.add(a)
        right.edges.add(b)
        edges.add(a)
        edges.add(b)
    }

    return generateFaces(pointMap.values, edges, Plane.XY)
}

fun findFace(segments: List<LineD>, point: PointD) : PolygonFace?{
    val faces = findFaces(segments)

    for( face in faces ){
        val triangles = face.generateTriangles()

        for( triangle in triangles ){
            val points = triangle.positions

            var found = true
            for( i in 0 until 3 ){
                val a = points[i]
                val b = points[(i + 1) % points.size]

                if((point - a).crossZ(b - a) > 0){
                    found = false
                    break
                }
            }

            if( found ){
                return face
            }
        }
    }

    return null
}
