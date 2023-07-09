//@file:Suppress("KotlinDeprecation")

package com.codecad.core.face

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.face.entity.*
import java.util.*
import kotlin.math.abs


fun findFaces(lines: List<LineD>): List<RoutedFace> {
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

fun computeArea(start : Edge) : Double{
    var curr = start
    var area = 0.0
    while(true){
        val currPos = curr.target.point
        area += curr.source.point.crossZ(currPos)
        if(curr.target === start.source) break
        curr = curr.next!!
    }

    return area / 2
}

fun generateFaces(edges: Collection<Edge>) : List<RoutedFace>{
    val visited = hashSetOf<Edge>()

    val holes = arrayListOf<RoutedFace>()
    for( root in edges ){
        if(visited.contains(root)) continue

        var hole : RoutedFace? = null
        val surfaces = arrayListOf<RoutedFace>()
        val queue = LinkedList<Edge>()

        queue.add(root)
        while( queue.isNotEmpty() ){
            val start = queue.pollFirst()
            if(!visited.add(start)) continue

            val area = computeArea(start)
            val routedFace = RoutedFace(start, mutableListOf(), abs(area), Plane.UNKNOWN)

            if(area > 0){
                surfaces.add(routedFace)
            }else if(hole == null){
                hole = routedFace
            }else{
                throw Error("error!!")
            }

            for( curr in start.loop() ){
                visited.add(curr)
            }

            for( curr in start.loop() ){
                val twin = curr.twin
                queue.add(twin)
            }
        }

        if(hole == null) throw Error("No hole found")
        hole.children = surfaces
        holes.add(hole)
    }

    return nestHoles(holes)
}

fun nestHoles(holes : MutableList<RoutedFace>) : List<RoutedFace>{
    holes.sortByDescending { it.area }

    val rootHoles = arrayListOf<RoutedFace>()
    for( hole in holes ){
        nestHoles(hole, rootHoles)
    }

    return collectSurfaces(rootHoles)
}

fun nestHoles(hole : RoutedFace, rootHoles: MutableList<RoutedFace>){
    for( rootHole in rootHoles ){
        if(rootHole.area <= hole.area) continue

        for( surface in rootHole.children ){
            if(surface.area <= hole.area) continue

            if(isPointInPolygon(hole.root.source.point, surface.points())){
                nestHoles(hole, surface.children)
                return
            }
        }
    }

    rootHoles.add(hole)
}

fun collectSurfaces(holes : List<RoutedFace>) : List<RoutedFace>{
    val surfaces = mutableListOf<RoutedFace>()
    collectSurfaces(holes, surfaces)
    return surfaces
}

fun collectSurfaces(holes : List<RoutedFace>, surfaces : MutableList<RoutedFace>){
    for( hole in holes ){
        surfaces.addAll(hole.children)
        for( surface in hole.children ){
            collectSurfaces(surface.children, surfaces)
        }
    }
}


fun RoutedFace.toPolygonFace() : PolygonFace{
    val points = mutableListOf<PointD>()
    var curr = this.root

    while(true){
        val currPos = curr.target.point
        points.add(currPos)
        if(curr.target === this.root.source) break
        curr = curr.next!!
    }

    val type = if(area < 0) FaceType.Hole else FaceType.Surface
    val face = PolygonFace(points, type, Plane.UNKNOWN)
    face.area = area

    val list = arrayListOf<PolygonFace>()
    face.children = list
    for( hole in children ){
        list.add(hole.toPolygonFace())
    }
    return face
}

class FaceFinder(val faces: List<Face>){
    fun find(pos : PointD) : Face?{
        for( face in faces ){
            if(face.isInside(pos)){
                return face
            }
        }

        return null
    }
}

fun isPointInPolygon(point: PointD, polygon: Iterable<PointD>): Boolean {
    var windingNumber = 0

    for ((p1, p2) in polygon.rollover()) {
        if (p1.y <= point.y) {
            if (p2.y > point.y && isLeft(p1, p2, point) > 0) {
                windingNumber++
            }
        } else {
            if (p2.y <= point.y && isLeft(p1, p2, point) < 0) {
                windingNumber--
            }
        }
    }

    return windingNumber != 0
}

private fun isLeft(p0: PointD, p1: PointD, p2: PointD): Double {
    return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y)
}

