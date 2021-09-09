@file:Suppress("KotlinDeprecation")

package com.codecad.core.test

import com.codecad.common.Line
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


data class Node(val point : PointD)

data class Corner(val node: Node){
    val edges = mutableListOf<Edge>()

    fun addEdge(edge: Edge){
        if(edge.source.node !== node){
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
}




class PlaneEvent(val volume : Volume, val face: EdgedFace, val point: PointD, val start : Boolean)
class PlaneSlice(val face: EdgedFace, val plane: Plane, var start: Edge? = null, var end: Edge? = null, var startPosition : Node? = null, var endPosition: Node? = null)

class EdgeSlice(val a: Node? = null, val b: Node? = null, val plane: Plane?){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeSlice) return false

        if (!(a == other.a && b == other.b || a == other.b && b == other.a)) return false
        if (plane != other.plane) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (( a?.hashCode() ?: 0 ) + 1) *  ((b?.hashCode() ?: 0) + 1)
        result = 31 * result + (plane?.hashCode() ?: 0)
        return result
    }
}

class Intersection(val edge : Edge, val position: Node)

val edgeSlices = HashMap<EdgeSlice, Node>()

fun findIntersections(face: EdgedFace, plane: Plane) : List<Intersection>{
    val result = mutableListOf<Intersection>()
    for(edge in face){
        val start = edge.source.node
        val end = edge.target.node

        val edgeSlice = EdgeSlice(start, end, plane)

        if(edgeSlices.containsKey(edgeSlice)){
            val intersectionPosition = edgeSlices[edgeSlice]
            result.add(Intersection(edge, intersectionPosition!!))
        }else{
            val startDistance = plane.distanceTo(start.point)
            val endDistance = plane.distanceTo(end.point)
            if( startDistance * endDistance <= 0 && startDistance != endDistance){
                val intersection = when {
                    abs(startDistance) < 1e-8 -> start
                    abs(endDistance) < 1e-8 -> end
                    else -> Node(start.point + ( end.point - start.point ) *
                            startDistance / ( startDistance - endDistance ))
                }

                edgeSlices[edgeSlice] = intersection
                result.add(Intersection(edge, intersection))
            }
        }
    }
    return result
}


fun computePlaneSlices(base : ConnectedFaces, tool : ConnectedFaces ) :  List<PlaneSlice>{

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


    val planes = HashMap<EdgedFace, Plane>()

    fun faceToPlane(face: EdgedFace) : Plane{
        if(planes.containsKey(face)){
            return planes[face]!!
        }

        val plane = face.toPlane()
        planes[face] = plane
        return plane
    }

    fun buildEventQueue(volume: ConnectedFaces) : TreeSet<PlaneEvent>{
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

    val active = HashMap<EdgedFace, PlaneEvent>()

    data class Event(val intersection : Intersection, val offset: Double, val index: Int)

    val testComparator = ChainComparator.Builder<Event>()
        .withComparable { it.offset }
        .withComparable { it.index }
        .build()

    val resultSlices = mutableListOf<PlaneSlice>()

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

                val leftIntersections = findIntersections(event.face, rightPlane)//.sortedBy { line.direction.dot(it.position.point) }
                val rightIntersections = findIntersections(other.face, leftPlane)//.sortedBy { line.direction.dot(it.position.point) }

                if(leftIntersections.isEmpty() || rightIntersections.isEmpty()){
                    continue
                }

                val intersectionEvents = TreeSet(testComparator)

                for(intersection in leftIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.position.point), 0))
                }
                for(intersection in rightIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.position.point), 1))
                }

                val inside = BooleanArray(2){false}
                val last = Array<Intersection?>(2){null}

                fun createSlice(i : Int) : PlaneSlice{
                    return when(i){
                        0 -> PlaneSlice(event.face, rightPlane)
                        1 -> PlaneSlice(other.face, leftPlane)
                        else -> throw RuntimeException("")
                    }
                }

                val slices = Array(2){ createSlice(it) }

                var started: Intersection? = null
                var finishSegment : Intersection? = null


                for(intersectionEvent in intersectionEvents){
                    val currentIndex = intersectionEvent.index
                    val otherIndex = 1 - currentIndex
                    inside[currentIndex] = !inside[currentIndex]
                    last[currentIndex] = intersectionEvent.intersection

                    val currentSlice = slices[currentIndex]
                    val otherSlice = slices[otherIndex]

                    if(inside[currentIndex] && inside[ otherIndex ] ){
                        currentSlice.start = intersectionEvent.intersection.edge
                        currentSlice.startPosition = intersectionEvent.intersection.position

                        if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                            otherSlice.start = last[otherIndex]!!.edge
                        }

                        otherSlice.startPosition = currentSlice.startPosition
                        started = intersectionEvent.intersection
                    }else if(!inside[intersectionEvent.index] ){
                        if(started != null){
                            println("${started.position} to ${intersectionEvent.intersection.position}")

                            currentSlice.end = intersectionEvent.intersection.edge
                            currentSlice.endPosition = intersectionEvent.intersection.position

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)

                            finishSegment = intersectionEvent.intersection
                            started = null
                            continue
                        }else if(finishSegment != null){
                            if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                                currentSlice.end = last[otherIndex]!!.edge
                            }

                            currentSlice.endPosition = finishSegment.position

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)
                        }
                    }

                    finishSegment = null
                }

            }
        }
    }

    return resultSlices
}

fun applyPlaneSlices(slices : List<PlaneSlice>){

    val map = HashMap<EdgedFace, MutableList<PlaneSlice>>()

    for(planeSlice in slices){
        map.computeIfAbsent(planeSlice.face){ mutableListOf() }.add(planeSlice)
    }

    val outers = mutableListOf<PolygonFace>()

    for((face, slices) in map.entries){
        val plane = Plane.fromPoints(face.points().toList())

        val nodeMap = mutableMapOf<Node, Corner>()

        val edgeList = mutableSetOf<Edge>()
        val ignoreEdges = mutableSetOf<Edge>()

        for(edge in face.edges()){
            edgeList.add(edge)
            edgeList.add(edge.twin)
        }

        val corners = mutableSetOf<Corner>()
        corners.addAll(face.corners())

        for( corner in face.corners() ){
            nodeMap[corner.node] = corner
        }

        fun getOrCreateCorner(node: Node) : Corner{
            return nodeMap.computeIfAbsent(node){
                val corner = Corner(node)
                corners.add(corner)
                corner
            }
        }

        fun createEdge(source: Corner, target: Corner) : Edge{
            val edge = Edge(source, target)
            edgeList.add(edge)
            return edge
        }

        val edgeMap = mutableMapOf<Edge, MutableList<Corner>>()

        for( slice in slices ){
            val startCorner = getOrCreateCorner(slice.startPosition!!)
            val endCorner = getOrCreateCorner(slice.endPosition!!)

            if(slice.start?.source?.node === slice.startPosition ){
                if(slice.end?.target?.node === slice.endPosition){
                    println("nocut")
                    continue
                }
            }else if(slice.start?.target?.node === slice.startPosition){
                if(slice.end?.source?.node === slice.endPosition){
                    println("nocut")
                    continue
                }
            }

            val edge = createEdge(startCorner, endCorner)
            edge.twin = createEdge(endCorner, startCorner)
            edge.twin.twin = edge

            val direction = endCorner.node.point - startCorner.node.point

            val test = slice.plane.normal.cross(direction).dot(plane.normal)

            if(test > 0){
                edge.side = Side.Outside
                edge.twin.side = Side.Inside
            }else{
                edge.side = Side.Inside
                edge.twin.side = Side.Outside
            }

            startCorner.addEdge(edge)
            endCorner.addEdge(edge.twin)

            if(slice.start != null){
                edgeMap.computeIfAbsent(slice.start!!) { mutableListOf()}.add(startCorner)
            }

            if(slice.end != null) {
                edgeMap.computeIfAbsent(slice.end!!) { mutableListOf() }.add(endCorner)
            }
        }

        for((edge, corners) in edgeMap.entries){
            val direction = (edge.target.node.point - edge.source.node.point).normalized()
            val sortedCorners = corners.sortedBy { it.node.point.dot(direction) }.toMutableList()
            sortedCorners.add(edge.target)

            nodeMap[edge.source.node] = edge.source
            nodeMap[edge.target.node] = edge.target

            var current = edge.source
            for(corner in sortedCorners){
                val newEdge = createEdge(current, corner)
                newEdge.twin = createEdge(corner, current)
                newEdge.twin.twin = newEdge
                current.addEdge(newEdge)
                corner.addEdge(newEdge.twin)
                current = corner
            }

            edge.source.edges.remove(edge)
            edge.target.edges.remove(edge.twin)
            ignoreEdges.add(edge)
            ignoreEdges.add(edge.twin)
        }

        var comparator: RotaryComparator? = null

        for(corner in corners){
            if(corner.edges.size > 2){
                if(comparator == null){
                    comparator = RotaryComparator(plane)
                }
                corner.edges.sortBy(comparator)
            }

            for(i in corner.edges.indices){
                val top = corner.edges[i]
                val bottom = corner.edges[(i+1)%corner.edges.size]
                top.twin.next = bottom
            }
        }

        edgeList.removeAll(ignoreEdges)
        val faces = generateFaces(plane, edgeList)
        val outer = combineFaces(plane, faces)

        outers.addAll(outer)
    }


    println("hello")
}

fun generateFaces(plane: Plane, edges: Collection<Edge>) : List<PolygonFace>{
    val faces = mutableListOf<PolygonFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val next = queue.first()
        queue.remove(next)

        var area = PointD.ZERO
        val points = mutableListOf<Node>()
        var current = next
        val face = PolygonFace(points)

        var side = Side.Unknown
        while(true){
            points.add(current.target.node)

            area = area + current.source.node.point.cross(current.target.node.point)

            if(current.target === next.source){
                break
            }

            if(current.side != Side.Unknown){
                if(side != Side.Outside){
                    side = current.side
                }
            }

            current = current.next!!
            queue.remove(current)
        }

        val areaVolume = area.length() / 2
        val clockwise = area.dot(plane.normal) < 0

        face.area = areaVolume
        face.clockwise = clockwise
        face.side = side

        faces.add(face)
    }

    return faces
}

fun getLeftmostPoint(direction: PointD, polygonFace: PolygonFace) : PointD {
    var leftMostOffset: Double = Double.MAX_VALUE
    var leftMost: PointD? = null
    for( point in polygonFace.positions ){
        val leftOffset = point.point.dot(direction)
        if(leftMostOffset > leftOffset){
            leftMostOffset = leftOffset
            leftMost = point.point
        }
    }
    return leftMost!!
}



fun main() {
    val plane = Plane.fromPoints(PointD(0.0,0.0,1.0),PointD(1.0,0.0,1.0),PointD(1.0,1.0,1.0))
    println(plane.normal)
    println(plane.distance)
    val list = listOf(
        PointD(-0.5,-0.5),
        PointD(0.5,-0.5),
        PointD(0.5,0.5),
        PointD(-0.5,0.5)
    )

    val list2 = listOf(
        PointD(-0.2,-0.2),
        PointD(0.2,-0.2),
        PointD(0.2,0.2),
        PointD(-0.2,0.2)
    )

    val base = Extrude(PolygonFace(list.map { Node(it) }), Const(1.0)).extrude()
    val tool = Extrude(PolygonFace(list2.map { Node(it) }), Const(2.0)).extrude()

    val slices = computePlaneSlices(base, tool)
    applyPlaneSlices(slices)
}

fun combineFaces(plane: Plane, faces: List<PolygonFace>) : List<PolygonFace>{
    val directedPlane = DirectedPlane.from(plane)

    val leftMostMap = mutableMapOf<PolygonFace, PointD>()
    fun getLeftmost(face: PolygonFace) : PointD{
        return leftMostMap.computeIfAbsent(face) {getLeftmostPoint(directedPlane.first, face)}
    }

    val orderedFaces = faces.sortedBy { getLeftmost(it).dot(directedPlane.first) }
    val inners = orderedFaces.filter { it.clockwise }

    for(innerFace in inners) {
        var maxUnitOffset: Double = -Double.MAX_VALUE
        var closestFace: PolygonFace? = null

        val leftmost = getLeftmost(innerFace)
        val pointUnitOffset = leftmost.dot(directedPlane.first)

        for (outerFace in orderedFaces) {
            if (outerFace === innerFace) {
                break
            }

            for ((source, target) in outerFace.positions.rollover()) {
                val toSource = leftmost - source.point
                val toTarget = leftmost - target.point

                val c = directedPlane.first.cross(toSource).dot(directedPlane.plane.normal)
                val d = directedPlane.first.cross(toTarget).dot(directedPlane.plane.normal)

                if (c * d > 0) {
                    continue
                }

                val sourceUnitOffset = source.point.dot(directedPlane.first)
                val targetUnitOffset = target.point.dot(directedPlane.first)

                val minCurrentUnitOffset = min(sourceUnitOffset, targetUnitOffset)

                if (minCurrentUnitOffset + 1e-8 >= pointUnitOffset) {
                    continue
                }

                val maxCurrentUnitOffset = max(sourceUnitOffset, targetUnitOffset)

                if (maxCurrentUnitOffset > maxUnitOffset) {
                    closestFace = outerFace
                    maxUnitOffset = maxCurrentUnitOffset
                }

            }
        }

        if (closestFace != null){
            if (closestFace.parent != null) {
                closestFace.parent!!.children.add(innerFace)
                innerFace.parent = closestFace.parent!!
            } else {
                closestFace.children.add(innerFace)
                innerFace.parent = closestFace
            }
        }
    }

    return orderedFaces.filter { !it.clockwise }
}



