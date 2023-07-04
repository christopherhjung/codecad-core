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



fun finishCorners(corners : Collection<Corner>, plane: Plane){
    val comparator = RotaryComparator(plane)

    for(corner in corners){
        corner.edges.sortWith(comparator)

        for((top, bottom) in corner.edges.rollover()){
            if( top.twin.target === bottom.source ){
                top.twin.next = bottom
            }else{
                //throw Error("not matching")
            }
        }
    }
}

fun generateFaces(corners: Collection<Corner>, edges: Collection<Edge>, plane: Plane) : List<PolygonFace>{
    val sideMap = mutableMapOf<PolygonFace, Side>()

    finishCorners(corners, plane)
    val faces = mutableListOf<PolygonFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val next = queue.first()
        queue.remove(next)

        var area = PointD.ZERO
        val points = mutableListOf<PointD>()
        var current = next

        var side = Side.Unknown

        val edges = mutableListOf<Edge>()
        while(true){
            edges.add(current)
            points.add(current.target.point)

            area = area + current.source.point.cross(current.target.point)

            if(current.target === next.source){
                break
            }

            if(current.side != Side.Unknown){
                if(side != Side.Unknown ) {
                    if(side != current.side){
                        println("upps")
                        //throw RuntimeException("ss")
                    }
                }else{
                    side = current.side
                }
            }

            current = current.next!!
            queue.remove(current)
        }

        val type = if(area.dot(plane.normal) < 0) FaceType.Hole else FaceType.Surface
        val face = PolygonFace(points, type, plane)
        face.area = area.length() / 2
        face.side = side
        sideMap[face] = side

        faces.add(face)
    }

    return combineFaces(faces)
}

fun getLeftmostPoint(polygonFace: PolygonFace) : PointD {
    return polygonFace.positions.minByOrNull { it.x }!!
}

//TODO
fun combineFaces(faces: List<PolygonFace>) : List<PolygonFace>{

    val leftMostMap = mutableMapOf<PolygonFace, PointD>()
    fun getLeftmost(face: PolygonFace) : PointD{
        return leftMostMap.computeIfAbsent(face) {getLeftmostPoint(face)}
    }

    val faces = faces.sortedBy { getLeftmost(it).x }
    val holes = faces.filter { it.type == FaceType.Hole }
    val surfaces = faces.filter { it.type == FaceType.Surface }
/*
    val events = events(surfaces.flatMap { it.positions })

    for(hole in holes) {
        var maxUnitOffset: Double = -Double.MAX_VALUE
        var closestFace: PolygonFace? = null

        val holePoint = hole.positions.first().point

        for (surface in surfaces) {
            for ((source, target) in surface.positions.rollover()) {
                val toSource = holePoint - source.point
                val toTarget = holePoint - target.point

                val c = directedPlane.first.cross(toSource).dot(directedPlane.normal)
                val d = directedPlane.first.cross(toTarget).dot(directedPlane.normal)

                if (c * d > 0) {
                    continue
                }

                val sourceUnitOffset = source.point.dot(directedPlane.first)
                val targetUnitOffset = target.point.dot(directedPlane.first)

                val minCurrentUnitOffset = min(sourceUnitOffset, targetUnitOffset)
                if (minCurrentUnitOffset + 1e-8 >= holePoint) {
                    continue
                }

                val maxCurrentUnitOffset = max(sourceUnitOffset, targetUnitOffset)

                if (maxCurrentUnitOffset > maxUnitOffset) {
                    closestFace = surface
                    maxUnitOffset = maxCurrentUnitOffset
                }
            }
        }


    }*/

   // return faces.filter { it.type == FaceType.Surface }
    return arrayListOf()
}
