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
        val start = queue.first()
        queue.remove(start)

        var area = 0.0
        val points = mutableListOf<PointD>()
        var curr = start

        while(true){
            val currPos = curr.target.point
            points.add(currPos)
            area += curr.source.point.crossZ(currPos)
            if(curr.target === start.source) break
            curr = curr.next!!
            queue.remove(curr)
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
    val finder = FaceFinder(surfaces)

    faces.filter { it.type == FaceType.Hole }.forEach { hole ->
        val holePos = getLeftmostPoint(hole)

        finder.find(holePos)?.let {
            it.area -= hole.area
            it.holes.add(hole)
        }
    }

    return surfaces
}

class FaceFinder(surfaces: List<PolygonFace>){
    private val line2face = HashMap<LineD, PolygonFace>()
    private val lines = run{
        surfaces.flatMap { surface -> surface.positions.rollover().map {
            val line = LineD(it.first, it.second)
            line2face[line] = surface
            line
        }}
    }

    private val events = events(lines)

    fun find(pos : PointD) : PolygonFace?{
        var line : LineD? = null
        for(event in events){
            if(event.pos.x > pos.x){
                break
            }

            val currLine = event.line
            if(event.origin && (currLine.p0.y > pos.y) == (currLine.p1.y < pos.y) && currLine.p0 !== pos && currLine.p1 !== pos){
                line = currLine
            }
        }

        return if(line != null && line.p0.y > line.p1.y){
            line2face[line]
        }else null
    }
}

