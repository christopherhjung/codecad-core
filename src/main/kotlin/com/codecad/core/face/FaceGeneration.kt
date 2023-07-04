//@file:Suppress("KotlinDeprecation")

package com.codecad.core.face

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.face.entity.*
import java.util.HashMap
import java.util.HashSet





fun findFaces(lines: List<LineD>): List<PolygonFace> {
    val sections = cutLines(lines)

    val pointMap = HashMap<PointD, Corner>()
    val edges = HashSet<Edge>()

    fun corner(point: PointD) : Corner {
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


fun finishCorners(corners : Collection<Corner>, plane: Plane){
    val comparator = RotaryComparator(plane)

    for(corner in corners){
        corner.edges.sortWith(comparator)

        for((top, bottom) in corner.edges.rollover()){
            assert(top.twin.target === bottom.source)
            top.twin.next = bottom
        }
    }
}

fun generateFaces(corners: Collection<Corner>, edges: Collection<Edge>, plane: Plane) : List<PolygonFace>{
    finishCorners(corners, plane)
    val faces = mutableListOf<PolygonFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val next = queue.first()
        queue.remove(next)

        var area = PointD.ZERO
        val points = mutableListOf<PointD>()
        var current = next

        val edges = mutableListOf<Edge>()
        while(true){
            edges.add(current)
            points.add(current.target.point)

            area = area + current.source.point.cross(current.target.point)

            if(current.target === next.source){
                break
            }

            current = current.next!!
            queue.remove(current)
        }

        val type = if(area.dot(plane.normal) < 0) FaceType.Hole else FaceType.Surface
        val face = PolygonFace(points, type, plane)
        face.area = area.length() / 2
        faces.add(face)
    }

    return combineFaces(faces)
}



//TODO
fun combineFaces(faces: List<PolygonFace>) : List<PolygonFace>{
    fun getLeftmostPoint(polygonFace: PolygonFace) : PointD {
        return polygonFace.positions.minByOrNull { it.x }!!
    }

    val surfaces = faces.filter { it.type == FaceType.Surface }

    val map = HashMap<LineD, PolygonFace>()
    val lines = surfaces.flatMap { surface -> surface.positions.rollover().map {
        val line = LineD(it.first, it.second)
        map[line] = surface
        line
    } }
    val events = events(lines)

    faces.filter { it.type == FaceType.Hole }.forEach { hole ->
        val holePos = getLeftmostPoint(hole)

        var line : LineD? = null
        for(event in events){
            if(event.pos.x > holePos.x){
                break
            }

            val currLine = event.line
            if(event.origin && (currLine.p0.y > holePos.y) == (currLine.p1.y < holePos.y) && currLine.p0 !== holePos && currLine.p1 !== holePos){
                line = currLine
            }
        }

        if(line != null && line.p0.y > line.p1.y){
            map[line]?.holes?.add(hole)
        }
    }

    return surfaces
}
