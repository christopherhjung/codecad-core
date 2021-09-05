package com.codecad.core.test

import com.codecad.common.Line
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import java.util.*
import kotlin.collections.HashMap


class Node(val p : PointD){

}

class Corner(val node: Node){

}

class Edge(val source: Corner? = null,
           val target : Corner? = null){
    var next: Edge? = null
}

class Volume(val faces: List<VolumeFace>){

}

class VolumeFace(val init : Edge) : Iterable<Edge>{
    override fun iterator(): Iterator<Edge> {
        var current : Edge = init
        var first = true

        return object : Iterator<Edge>{
            override fun hasNext(): Boolean {
                return first || current != init
            }

            override fun next(): Edge {
                first = false
                val result =  current//.source!!//.node.p
                current = current.next!!
                return result
            }
        }
    }

    fun points() : Iterable<PointD>{
        return Iterable {
            var current : Edge = init
            var first = true
            object : Iterator<PointD>{
                override fun hasNext(): Boolean {
                    return first || current != init
                }

                override fun next(): PointD {
                    first = false
                    val result =  current.source!!.node.p
                    current = current.next!!
                    return result
                }
            }
        }
    }
}




class PlaneEvent(val volume : Volume, val face: VolumeFace, val point: PointD, val start : Boolean){

}

class PlaneEdge(val origin: PointD, val target: PointD){
    val next: PlaneEdge? = null
}


fun main() {
    val list = listOf(
        PointD(-0.5,-0.5,0.0),
        PointD(0.5,-0.5,0.0),
        PointD(0.5,0.5,0.0),
        PointD(-0.5,0.5,0.0)
    )

    val list2 = listOf(
        PointD(-0.2,-0.2,0.0),
        PointD(0.2,-0.2,0.0),
        PointD(0.2,0.2,0.0),
        PointD(-0.2,0.2,0.0)
    )


    val base = generateFaces(Extrude(PolygonFace(list), Const(1.0)))
    val tool = generateFaces(Extrude(PolygonFace(list2), Const(2.0)))

    sweepingPlane(base, tool)
}

fun generateFaces(extrude: Extrude) : Volume{
    val face = extrude.polygonFace
    val height = extrude.height.value

    val map = HashMap<PointD, Node>()
    val faces = mutableListOf<VolumeFace>()
    val inverted = height > 0

    val offsetVector = PointD(0.0,0.0, height)

    fun getOrAdd(x: Double, y: Double, z: Double) : Node{
        val new = PointD(x,y,z)
        return map.computeIfAbsent(new){Node(new)}
    }

    fun getOrAdd(new : PointD) : Node{
        return map.computeIfAbsent(new){Node(new)}
    }

    fun generateEdges(points : List<PointD>, invert: Boolean = false) : List<Edge>{
        val edges = points.map { Corner(getOrAdd(it)) }.rollover().map { (a,b) -> if(invert) Edge(a,b) else Edge(b,a) }
        edges.rollover().forEach{ (left, right) ->
            if(invert){
                right.next = left
            }else{
                left.next = right
            }
        }

        faces.add(VolumeFace(edges.first()))
        return edges
    }

    generateEdges(face.points, inverted)
    generateEdges(face.points.map { it + offsetVector }, !inverted)

    fun iterate(parent: PolygonFace){
        for( (left, right) in parent.points.rollover() ){
            val list = mutableListOf(
                PointD(left.x, left.y, 0.0),
                PointD(left.x, left.y, height),
                PointD(right.x, right.y, height),
                PointD(right.x, right.y, 0.0)
            )

            generateEdges(list, inverted)
        }

        for( child in parent.children){
            iterate(child)
        }
    }

    iterate(face)

    return Volume(faces)
}

class Intersection(val edge : Edge, val point: PointD)

fun findIntersections(face: VolumeFace, plane: Plane) : List<Intersection>{
    val result = mutableListOf<Intersection>()
    for(edge in face){
        val start = edge.source!!.node.p
        val end = edge.target!!.node.p
        val startDistance = plane.distanceTo(start)
        val endDistance = plane.distanceTo(end)
        if( startDistance * endDistance <= 0 && startDistance != endDistance){
            val k = startDistance / ( startDistance - endDistance )
            val intersection = start + ( end - start ) * k
            result.add(Intersection(edge, intersection))
        }
    }
    return result
}


fun test(){
    val a = PolygonFace(listOf(
        PointD(-0.5,-0.5,0.0),
        PointD(0.5,-0.5,0.0),
        PointD(0.5,0.5,0.0),
        PointD(-0.5,0.5,0.0)
    ))

    val b = PolygonFace(listOf(
        PointD(0.0,-0.3,-0.5),
        PointD(0.0,0.7,-0.5),
        PointD(0.0,0.7,0.5),
        PointD(0.0,-0.3,0.5)
    ))
}

fun sweepingPlane( base : Volume, tool : Volume ){

    val planeComparator = ChainComparator.Builder<PlaneEvent>()
        .withComparable { it.point.x }
        .withComparable { it.point.y }
        .withComparable { it.point.z }
        .withComparable(true) { it.start }
        .withComparator{ a,b ->
            if(a.start && b.start){
                if(a.volume === base && b.volume === tool){
                    1
                }else if(a.volume === b.volume){
                    1
                }else{
                    -1
                }
            }else if(!a.start && !b.start){
                if(a.volume === tool && b.volume === base){
                    1
                }else if(a.volume === b.volume){
                    1
                }else{
                    -1
                }
            }else{
                b.start.compareTo(a.start)
            }
        }
        .withDefault(1)
        .build()

    val pointListComparator = ChainComparator.Builder<PointD>()
        .withComparable { it.x }
        .withComparable { it.y }
        .withComparable { it.z }
        .build()


    val planes = HashMap<VolumeFace, Plane>()

    fun faceToPlane(face: VolumeFace) : Plane{
        if(planes.containsKey(face)){
            return planes[face]!!
        }

        val a = face.init
        val b = a.next
        val c = b?.next
        val plane = Plane.fromPoints(a.source!!.node.p, b!!.source!!.node.p, c!!.source!!.node.p)
        planes[face] = plane
        return plane
    }

    fun buildEventQueue(volume: Volume) : TreeSet<PlaneEvent>{
        val events = TreeSet(planeComparator)
        for(face in volume.faces){
            val points = face.points().toList()
            val min = points.minWithOrNull(pointListComparator)!!
            val max = points.maxWithOrNull(pointListComparator)!!
            events.add(PlaneEvent(volume, face, min, true))
            events.add(PlaneEvent(volume, face, max, false))
        }
        return events
    }

    val baseEventQueue = buildEventQueue(base)
    val toolEventQueue = buildEventQueue(tool)

    baseEventQueue.addAll(toolEventQueue)


    val active = HashMap<VolumeFace, PlaneEvent>()


    data class Event(val intersection : Intersection, val offset: Double, val start: Boolean, val index: Int)

    val testComparator = ChainComparator.Builder<Event>()
        .withComparable { it.offset }
        .withComparable { it.index }
        .withComparable { it.start }
        /*.withComparator{ a,b ->
            if(a.start && b.start){
                if(a.index == 1 && b.index == 0){
                    1
                }else if(a.index == b.index){
                    1
                }else{
                    -1
                }
            }else if(!a.start && !b.start){
                if(a.index == 0 && b.index == 1){
                    1
                }else if(a.index == b.index){
                    1
                }else{
                    -1
                }
            }else{
                b.start.compareTo(a.start)
            }
        }*/
        .build()


    while (baseEventQueue.isNotEmpty()) {
        val event = baseEventQueue.pollFirst()!!

        if(event.volume === base){
            if(event.start){
                active[event.face] = event
            }else{
                active.remove(event.face)
            }
            continue
        }

        if (event.start) {
            val leftPlane = faceToPlane(event.face)
            for (other in active.values) {
                val rightPlane = faceToPlane(other.face)
                val line = Line.fromPlanes(leftPlane, rightPlane)

                if(leftPlane.normal == rightPlane.normal){
                    continue
                }

                val rightIntersections = findIntersections(event.face, rightPlane).sortedBy { line.direction.dot(it.point) }
                val leftIntersections = findIntersections(other.face, leftPlane).sortedBy { line.direction.dot(it.point) }

                if(leftIntersections.isEmpty() || rightIntersections.isEmpty()){
                    continue
                }

                val intersectionEvents = TreeSet(testComparator)

                var start = true
                for(intersection in leftIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.point), start, 0))
                    start = !start
                }
                start = true
                for(intersection in rightIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.point), start, 1))
                    start = !start
                }

                val last = BooleanArray(2){false}

                var started: Intersection? = null
                for(intersectionEvent in intersectionEvents){
                    last[intersectionEvent.index] = intersectionEvent.start

                    if(intersectionEvent.start && last[ 1 - intersectionEvent.index ] ){
                        started = intersectionEvent.intersection
                    }else if(!intersectionEvent.start && started != null){
                        println("${started.point} to ${intersectionEvent.intersection.point}")
                        started = null
                    }
                }
            }

        }
    }
}

