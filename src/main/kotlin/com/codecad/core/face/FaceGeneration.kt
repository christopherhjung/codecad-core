//@file:Suppress("KotlinDeprecation")

package com.codecad.core.face

import com.codecad.common.LineD
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.face.entity.*
import java.util.*
import kotlin.math.abs

fun createFaceTree(lines: List<LineD>): RoutedFace {
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

fun generateFaces(edges: Collection<Edge>) : RoutedFace{
    val visited = hashSetOf<Edge>()

    val holes = arrayListOf<RoutedFace>()
    for( root in edges ){
        if(visited.contains(root)) continue

        var hole : RoutedFace? = null
        val surfaces = arrayListOf<Face>()
        val queue = LinkedList<Edge>()

        queue.add(root)
        while( queue.isNotEmpty() ){
            val start = queue.pollFirst()
            if(!visited.add(start)) continue

            val area = computeArea(start)
            val routedFace = RoutedFace(start)
            routedFace.area = abs(area)

            if(area > 0){
                surfaces.add(routedFace)
            }else if(hole == null){
                hole = routedFace
            }else{
                throw Error("double hole!")
            }

            for( curr in start.loop() ){
                visited.add(curr)
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

fun nestHoles(holes : MutableList<RoutedFace>) : RoutedFace{
    holes.sortByDescending { it.area }

    val rootSurface = RoutedFace(Edge.ZERO)
    for( hole in holes ){
        nestHoles(hole, rootSurface)
    }

    return rootSurface
}

fun nestHoles(hole : RoutedFace, parentSurface: Face){
    for( rootHole in parentSurface.children){
        if(rootHole.area <= hole.area) continue

        for( surface in rootHole.children ){
            if(surface.area <= hole.area) continue

            if(isPointInPolygon(hole.root.source.point, surface.points)){
                nestHoles(hole, surface)
                return
            }
        }
    }

    parentSurface.area -= hole.area
    parentSurface.children.add(hole)
}

fun collectSurfaces(rootSurface: Face) : List<Face>{
    val surfaces = mutableListOf<Face>()
    collectSurfaces(rootSurface, surfaces)
    return surfaces
}

fun collectSurfaces(parentSurface : Face, surfaces : MutableList<Face>){
    for( hole in parentSurface.children ){
        surfaces.addAll(hole.children)
        for( surface in hole.children ){
            collectSurfaces(surface, surfaces)
        }
    }
}


fun Face.toPolygonFace() : PolygonFace{
    if(this is PolygonFace) return this

    val points = points.toList()

    val result = PolygonFace(points)
    result.type = type
    result.area = area

    val list = arrayListOf<Face>()
    result.children = list
    for( hole in children ){
        list.add(hole.toPolygonFace())
    }
    return result
}
/*
fun unionFaces(faces: List<PolygonFace>) : List<PolygonFace>{
    val pointMap = HashMap<PointD, Corner>()
    fun corner(point: PointD) : Corner {
        return pointMap.computeIfAbsent(point) { Corner(it) }
    }

    val edges = mutableListOf<Edge>()

    val test = hashSetOf<String>()


    for(face in faces){
        for((p0, p1) in face.positions.rollover() ){
            val edge = Edge(corner(p0), corner(p1))

            edges.add(edge)
        }
    }
}*/

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

