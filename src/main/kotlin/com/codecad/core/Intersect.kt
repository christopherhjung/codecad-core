package com.codecad.core

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.test.Corner
import com.codecad.core.test.Edge
import com.codecad.core.test.Node
import com.codecad.core.test.generateFaces
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

class Event(
    val p: PointD,
    val line: LineD,
    val isLeft: Boolean
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        if (p.x == other.p.x) return p.y.compareTo(other.p.y)
        return p.x.compareTo(other.p.x)
    }
}


fun removeIntersections(arr: List<LineD>): List<LineD> {

    val events = LinkedList<Event>()

    val ordered = mutableListOf<LineD>()
    for(line in arr){
        if (line.p0.x > line.p1.x) {
            ordered.add(LineD(line.p1, line.p0))
        }else{
            ordered.add(line)
        }
    }

    for (line in ordered) {
        events.add(Event(line.p0, line, true))
        events.add(Event(line.p1, line, false))
    }

    events.sort()

    val active = HashMap<LineD, Event>()
    val splittingPoints = HashMap<LineD, MutableList<PointD>>()
    for (event in events) {
        if (event.isLeft) {
            for (other in active.values) {
                val intersection = findIntersection(other.line, event.line)
                if (intersection != null) {
                    splittingPoints.computeIfAbsent(event.line){ mutableListOf()}.add(intersection)
                    splittingPoints.computeIfAbsent(other.line){ mutableListOf()}.add(intersection)
                }
            }

            active[event.line] = event
        } else {
            active.remove(event.line)
        }
    }

    val result = mutableListOf<LineD>()

    for( line in ordered ){
        val splits = splittingPoints[line]
        if( splits != null ){
            splits.sortBy { it.x }
            var left = line.p0
            for( split in splits ){
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

fun findFaces(arr2: List<LineD>): List<PolygonFace> {
    val ordered = removeIntersections(arr2)
/*
    val ordered = mutableListOf<LineD>()
    for (line in arr) {
        ordered.add(
            if (line.p0.x > line.p1.x) {
                LineD(line.p1, line.p0)
            } else {
                line
            }
        )
    }*/

    val pointMap = HashMap<PointD, Corner>()
    val edges = HashSet<Edge>()

    for (line in ordered) {
        val left = pointMap.computeIfAbsent(line.p0) { Corner(Node(it)) }
        val right = pointMap.computeIfAbsent(line.p1) { Corner(Node(it)) }

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

                if((point - a.point).crossZ(b.point - a.point) > 0){
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
