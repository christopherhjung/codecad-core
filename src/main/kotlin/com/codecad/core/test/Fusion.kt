//@file:Suppress("KotlinDeprecation")

package com.codecad.core.test

import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*

data class Corner(val point: PointD){
    val edges = mutableListOf<Edge>()

    fun addEdge(edge: Edge){
        if(edge.source.point !== point){
            throw RuntimeException("ss")
        }

        edges.add(edge)
    }
}

enum class FaceType{
    Surface, Hole
}

enum class Side{
    Unknown, Outside, Inside
}

data class Edge(val source: Corner,
           val target : Corner){
    var next: Edge? = null
    lateinit var twin : Edge
    var side : Side = Side.Unknown

    companion object{
        fun withAdd(source: Corner, target: Corner) : Edge{
            val edge = Edge(source, target)
            source.addEdge(edge)
            return edge
        }

        fun withAdd(source: PointD, target: PointD) : Edge{
            return withAdd(Corner(source), Corner(target))
        }

        fun twinEachOther(left: Edge, right: Edge){
            left.twin = right
            right.twin = left
        }
    }

    fun points() : Iterable<PointD>{
        return Iterable {
            var start : Edge = this
            var current : Edge = this
            var first = true
            object : Iterator<PointD>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): PointD {
                    first = false
                    val result =  current.source.point
                    current = current.next!!
                    return result
                }
            }
        }
    }

    fun corners() : Iterable<Corner>{
        return Iterable {
            var start : Edge = this
            var current : Edge = this
            var first = true
            object : Iterator<Corner>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): Corner {
                    first = false
                    val result = current.source
                    current = current.next!!
                    return result
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Edge) return false

        if (source != other.source) return false
        if (target != other.target) return false

        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + target.hashCode()
        return result
    }
}

fun finishCorners(corners : Collection<Corner>, plane: Plane){
    var comparator: RotaryComparator? = null

    for(corner in corners){
        if(corner.edges.size > 2){
            if(comparator == null){
                comparator = RotaryComparator(plane)
            }
            corner.edges.sortBy(comparator)
        }

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
