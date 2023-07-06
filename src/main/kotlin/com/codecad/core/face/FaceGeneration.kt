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
    val pointMap = HashMap<PointD, Corner>()
    fun corner(point: PointD) : Corner {
        return pointMap.computeIfAbsent(point) { Corner(it) }
    }

    val sections = cutLines(lines)
    val edges = HashSet<Edge>()
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

    finalizeCorners(pointMap.values)
    return generateFaces(edges)
}


fun finalizeCorners(corners : Collection<Corner>){
    for(corner in corners){
        corner.edges.sortWith(RotaryComparator)

        for((top, bottom) in corner.edges.rollover()){
            assert(top.twin.target === bottom.source)
            top.twin.next = bottom
        }
    }
}

fun generateFaces(edges: Collection<Edge>) : List<PolygonFace>{
    val faces = mutableListOf<PolygonFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val next = queue.first()
        queue.remove(next)

        var area = 0.0
        val points = mutableListOf<PointD>()
        var current = next

        while(true){
            val pos = current.target.point
            points.add(pos)
            area += current.source.point.crossZ(pos)
            if(current.target === next.source) break

            current = current.next!!
            queue.remove(current)
        }

        val type = if(area < 0) FaceType.Hole else FaceType.Surface
        val face = PolygonFace(points, type, Plane.UNKNOWN)
        face.area = area / 2
        faces.add(face)
    }

    return nestFaces(faces)
}


fun nestFaces(faces: List<PolygonFace>) : List<PolygonFace>{
    fun getLeftmostPoint(polygonFace: PolygonFace) : PointD {
        return polygonFace.positions.minByOrNull { it.x }!!
    }

    val surfaces = faces.filter { it.type == FaceType.Surface }

    val line2face = HashMap<LineD, PolygonFace>()
    val lines = surfaces.flatMap { surface -> surface.positions.rollover().map {
        val line = LineD(it.first, it.second)
        line2face[line] = surface
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
            val face = line2face[line]
            face?.let {
                it.area -= hole.area
                it.holes.add(hole)
            }
        }
    }

    return surfaces
}

fun unionFaces(faces: List<PolygonFace>) : List<PolygonFace>{
    return faces
}