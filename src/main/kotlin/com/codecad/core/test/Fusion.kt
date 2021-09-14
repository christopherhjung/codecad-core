//@file:Suppress("KotlinDeprecation")

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

    fun containsTarget(corner: Corner) : Edge?{
        for(cornerEdge in edges){
            if(cornerEdge.target === corner){
                return cornerEdge
            }
        }
        return null
    }

    fun addEdge(edge: Edge){
        if(edge.source.node !== node){
            throw RuntimeException("ss")
        }

        if(edge.source.node.point.distanceTo(PointD(0.5,0.2, 0.0)) < 0.01 &&
            edge.target.node.point.distanceTo(PointD(0.2,0.2, 0.0)) < 0.01){
            println("sss")
        }

        for(cornerEdge in edges){
            if(cornerEdge.target === edge.target){
                throw RuntimeException("ss")
            }
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

enum class Owner{
    Unknown, Base, Tool
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

        fun withAdd(source: Node, target: Node) : Edge{
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
                    val result =  current.source.node.point
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




class PlaneEvent(val volume : RoutedVolume, val face: RoutedFace, val point: PointD, val start : Boolean)
class PlaneSlice(val face: RoutedFace, val plane: Plane, var start: Edge? = null, var end: Edge? = null, var startPosition : Node? = null, var endPosition: Node? = null)

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

fun findIntersections(face: RoutedFace, plane: Plane) : List<Intersection>{
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


class PlaneSliceResult(val slices: List<PlaneSlice>, val uncut: Set<RoutedFace>)

fun computePlaneSlices( base : RoutedVolume, tool : RoutedVolume ) :  PlaneSliceResult{
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


    fun buildEventQueue(volume: RoutedVolume) : List<PlaneEvent>{
        val events = mutableListOf<PlaneEvent>()
        for(face in volume.faces){
            val points = face.points().toList()
            val min = points.minWithOrNull(pointListComparator)!!
            val max = points.maxWithOrNull(pointListComparator)!!
            events.add(PlaneEvent(volume, face, min, true))
            events.add(PlaneEvent(volume, face, max, false))
        }
        return events
    }

    val uncutFaces = (base.faces + tool.faces).toMutableSet()

    val baseEventQueue = (buildEventQueue(base) + buildEventQueue(tool)).sortedWith(planeComparator)

    val activeBase = HashMap<RoutedFace, PlaneEvent>()
    val activeTool = HashMap<RoutedFace, PlaneEvent>()

    data class Event(val intersection : Intersection, val offset: Double, val index: Int)

    val testComparator = ChainComparator.Builder<Event>()
        .withComparable { it.offset }
        .withComparable { it.index }
        .build()

    val resultSlices = mutableListOf<PlaneSlice>()

    for(event in baseEventQueue) {
        val (currentActive, otherActive) = when{
            event.volume === base -> Pair(activeBase, activeTool)
            event.volume === tool -> Pair(activeTool, activeBase)
            else -> throw RuntimeException("ss")
        }

        if(event.start){
            currentActive[event.face] = event

            val leftPlane = event.face.plane
            for (other in otherActive.values) {
                val rightPlane = other.face.plane

                if(leftPlane.normal == rightPlane.normal){
                    continue
                }

                val leftIntersections = findIntersections(event.face, rightPlane)
                val rightIntersections = findIntersections(other.face, leftPlane)

                if(leftIntersections.isEmpty() || rightIntersections.isEmpty()){
                    continue
                }

                val intersectionEvents = TreeSet(testComparator)

                val line = Line.fromPlanes(leftPlane, rightPlane)
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

                val sizeBefore = resultSlices.size

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
                            currentSlice.end = intersectionEvent.intersection.edge
                            currentSlice.endPosition = intersectionEvent.intersection.position

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)

                            finishSegment = intersectionEvent.intersection
                            started = null
                            continue
                        }else if(finishSegment != null){
                            if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                                currentSlice.end = intersectionEvent.intersection.edge
                            }

                            currentSlice.endPosition = finishSegment.position

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)
                        }
                    }

                    finishSegment = null
                }


                val sizeAfter = resultSlices.size

                if(sizeBefore != sizeAfter){
                    uncutFaces.remove(event.face)
                    uncutFaces.remove(other.face)
                }
            }
        }else{
            currentActive.remove(event.face)
        }
    }

    return PlaneSliceResult(resultSlices, uncutFaces)
}

class FaceAssignment(val baseFaces : MutableList<Face> = mutableListOf(), val toolFaces : MutableList<Face> = mutableListOf())


class FaceOperations(val slices: MutableList<PlaneSlice> = mutableListOf(), val corners: MutableList<Corner> = mutableListOf(), val edges: MutableList<Edge> = mutableListOf())

class SpliceOp(val face: RoutedFace, val corners : MutableList<Corner> = mutableListOf())

fun applyPlaneSlices(slices: List<PlaneSlice> , assignmentTable : Map<RoutedFace, Owner>) : FaceAssignment{

    val map = HashMap<RoutedFace, FaceOperations>()

    for(planeSlice in slices){
        map.computeIfAbsent(planeSlice.face){ FaceOperations() }.slices.add(planeSlice)
    }

    val result = FaceAssignment()

    val edgeMap = mutableMapOf<Edge, SpliceOp>()

    for((face, ops) in map.entries){
        val nodeMap = mutableMapOf<Node, Corner>()

        ops.edges.addAll(face.edges())
        ops.corners.addAll(face.corners())

        for( corner in face.corners() ){
            nodeMap[corner.node] = corner
        }

        fun getOrCreateCorner(node: Node) : Corner{
            return nodeMap.computeIfAbsent(node){
                val corner = Corner(node)
                ops.corners.add(corner)
                corner
            }
        }

        fun createEdge(source: Corner, target: Corner) : Edge{
            if(abs(face.plane.distanceTo(target.node.point)) > 1e-8 ){
                println("error")
            }
            val edge = Edge(source, target)
            ops.edges.add(edge)
            return edge
        }

        for( slice in ops.slices ){
            if(slice.start?.source?.node === slice.startPosition ){
                if(slice.end?.target?.node === slice.endPosition){
                    continue
                }
            }else if(slice.start?.target?.node === slice.startPosition){
                if(slice.end?.source?.node === slice.endPosition){
                    continue
                }
            }

            //edge insert
            val startCorner = getOrCreateCorner(slice.startPosition!!)
            val endCorner = getOrCreateCorner(slice.endPosition!!)

            val edge = createEdge(startCorner, endCorner)
            val twin = createEdge(endCorner, startCorner)
            startCorner.addEdge(edge)
            endCorner.addEdge(twin)
            Edge.twinEachOther(edge, twin)

            //side detection
            val direction = endCorner.node.point - startCorner.node.point
            val test = direction.cross(slice.plane.normal).dot(slice.face.plane.normal)

            if(test > 0){
                edge.side = Side.Outside
                twin.side = Side.Inside
            }else{
                edge.side = Side.Inside
                twin.side = Side.Outside
            }

            //hit edge split queueing
            if(slice.start != null){
                if(slice.start?.target !== startCorner && slice.start?.source !== startCorner){
                    edgeMap.computeIfAbsent(slice.start!!) { SpliceOp(face) }.corners.add(startCorner)
                }
            }

            if(slice.end != null) {
                if(slice.end?.target !== endCorner && slice.end?.source !== endCorner){
                    edgeMap.computeIfAbsent(slice.end!!) { SpliceOp(face) }.corners.add(endCorner)
                }
            }
        }


        val plane = face.plane
        //edge split

        //edgeList.removeAll(ignoreEdges)

        /*
        val faces = generateFaces(corners, edgeList, face.plane)

        if(assignmentTable[face] == Owner.Tool){
            result.toolFaces.addAll(faces)
        }else{
            result.baseFaces.addAll(faces)
        }*/
    }



    val leftSources = mutableMapOf<Corner, Edge>()
    val rightSources = mutableMapOf<Corner, Edge>()

    edgeMap.keys.forEach {
        if(leftSources.containsKey(it.target)){
            rightSources
        }else{
            leftSources
        }[it.source] =  it
    }



    for(leftEdge in leftSources.values){

        val rightEdge = rightSources[leftEdge.target]!!

        val leftSplice = edgeMap[leftEdge]!!
        val rightSplice = edgeMap[rightEdge]!!

        val leftCorners = leftSplice.corners.toMutableList()
        val rightCorners = rightSplice.corners.toMutableList()

        val leftOps = map[leftSplice.face]!!
        val rightOps = map[rightSplice.face]!!

        val leftDirection = (leftEdge.target.node.point - leftEdge.source.node.point)//.normalized()
        leftCorners.add(leftEdge.source)
        leftCorners.add(leftEdge.target)
        leftCorners.sortBy { it.node.point.dot(leftDirection) }

        rightCorners.add(rightEdge.source)
        rightCorners.add(rightEdge.target)
        rightCorners.sortBy { it.node.point.dot(leftDirection) }

        leftEdge.source.edges.remove(leftEdge)
        leftEdge.target.edges.remove(leftEdge.twin)

        rightEdge.source.edges.remove(rightEdge)
        rightEdge.target.edges.remove(rightEdge.twin)

        for((leftCorner, rightCorner) in leftCorners.zipWithNext().zip(rightCorners.zipWithNext())){
            val (leftFirst, leftSecond) = leftCorner
            val (rightFirst, rightSecond) = rightCorner

            val leftSegment = Edge(leftFirst, leftSecond)
            leftFirst.addEdge(leftSegment)

            val rightSegment = Edge(rightSecond, rightFirst)
            rightSecond.addEdge(rightSegment)

            leftOps.edges.add(leftSegment)
            rightOps.edges.add(rightSegment)

            Edge.twinEachOther(leftSegment, rightSegment)
        }


        println("ss")
    }

    for( (face, ops) in map ){
        val face = generateFaces(ops.corners, ops.edges, face.plane)

        val test = face
    }


    return result
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
                println("test")
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
        println("-----------------------")
        val next = queue.first()
        queue.remove(next)

        var area = PointD.ZERO
        val points = mutableListOf<Node>()
        var current = next

        var side = Side.Unknown

        println(System.identityHashCode(next.source))
        println("--------")
        val edges = mutableListOf<Edge>()
        while(true){
            edges.add(current)
            points.add(current.target.node)

            area = area + current.source.node.point.cross(current.target.node.point)


            print(current.source)
            print(System.identityHashCode(current.source))
            print(" -- ")
            println(current.source.node.point)


            print(current.target)
            print(System.identityHashCode(current.target))
            print(" -- ")
            println(current.source.node.point)

            if(current.target === next.source){
                break
            }

            if(points.size > 100){
                println("upps")
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

        val face = PolygonFace(points, plane)
        face.area = area.length() / 2
        face.clockwise = area.dot(plane.normal) < 0
        face.side = side
        sideMap[face] = side

        faces.add(face)
    }

    return combineFaces(faces, plane)
}

fun getLeftmostPoint(polygonFace: PolygonFace, direction: PointD) : Node {
    return polygonFace.positions.minByOrNull { it.point.dot(direction) }!!
}

fun generateOwnerAssignment(base: RoutedVolume, tool: RoutedVolume) : Map<RoutedFace, Owner>{
    val map = mutableMapOf<RoutedFace, Owner>()
    for( face in base.faces ){
        map[face] = Owner.Base
    }

    for( face in tool.faces ){
        map[face] = Owner.Tool
    }
    return map
}

fun estimateSides(faces: List<Face>){
    class Entry(val a: Node, val b : Node){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false
            return (a == other.a && b == other.b || a == other.b && b == other.a)
        }

        override fun hashCode(): Int {
            return (a.hashCode() + 1) * (b.hashCode() + 1)
        }
    }

    val map = mutableMapOf<Entry, Side>()
    val unknowns = mutableSetOf<HasSide>()

    for(face in faces){
        if(face is HasSide){
            if(face.side != Side.Unknown){
                if(face is PolygonFace){
                    for( loop in face.holes + face ){
                        for( (left, right) in loop.positions.rollover() ){
                            val entry = Entry(left, right)
                            map[entry] = face.side
                        }
                    }
                }else if(face is ConvexFace){
                    for( (left, right) in face.positions.rollover() ){
                        val entry = Entry(left, right)
                        map[entry] = face.side
                    }
                }
            }else{
                unknowns.add(face)
            }
        }
    }

    var progress = true
    while(progress){
        progress = false
        for(face in unknowns.toList()){
            if(face is PolygonFace){
                for( loop in face.holes + face ){
                    for( (left, right) in loop.positions.rollover() ){
                        val entry = Entry(left, right)
                        val side = map[entry]
                        if(side != null){
                            face.side = side
                            break
                        }
                    }


                    if(face.side != Side.Unknown){
                        break
                    }
                }

                if(face.side != Side.Unknown){
                    for( loop in face.holes + face ){
                        for( (left, right) in loop.positions.rollover() ){
                            val entry = Entry(left, right)
                            map[entry] = face.side
                        }
                    }
                }
            }else if(face is ConvexFace){
                for( (left, right) in face.positions.rollover() ){
                    val entry = Entry(left, right)
                    val side = map[entry]
                    if(side != null){
                        face.side = side
                        break
                    }
                }

                if(face.side != Side.Unknown){
                    for( (left, right) in face.positions.rollover() ){
                        val entry = Entry(left, right)
                        map[entry] = face.side
                    }
                }
            }

            if(face.side != Side.Unknown){
                progress = true
                unknowns.remove(face)
            }else{
                println("sss")
            }
        }
    }

    if(unknowns.size != 0){
        throw RuntimeException("Unknown side")
    }
}

fun addVolumes(base: Volume, tool: Volume) : FacedVolume{
    val base = RoutedVolume.from(base)
    val tool = RoutedVolume.from(tool)
    val result = computePlaneSlices(base, tool)

    val ownerAssignment = generateOwnerAssignment(base, tool)
    val faceAssignment = applyPlaneSlices(result.slices, ownerAssignment)

    for(uncut in result.uncut){
        if(ownerAssignment[uncut] == Owner.Tool){
            faceAssignment.toolFaces.add(uncut.original!!)
        }else{
            faceAssignment.baseFaces.add(uncut.original!!)
        }
    }

    estimateSides(faceAssignment.toolFaces )
    estimateSides(faceAssignment.baseFaces)

    val baseFaces = faceAssignment.baseFaces.filter { if(it is HasSide) it.side != Side.Inside else false }
    val toolFaces = faceAssignment.toolFaces.filter { if(it is HasSide) it.side == Side.Inside else false }

    return FacedVolume(baseFaces + toolFaces.map {
        when (it) {
            is PolygonFace -> PolygonFace(it.positions.reversed(), it.plane.flip())
            is ConvexFace -> ConvexFace(it.positions.reversed())
            else -> TODO()
        }
    })
}

fun combineFaces(faces: List<PolygonFace>, plane: Plane) : List<PolygonFace>{
    val directedPlane = DirectedPlane.from(plane)

    val leftMostMap = mutableMapOf<PolygonFace, PointD>()
    fun getLeftmost(face: PolygonFace) : PointD{
        return leftMostMap.computeIfAbsent(face) {getLeftmostPoint(face, directedPlane.first).point}
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

                val c = directedPlane.first.cross(toSource).dot(directedPlane.normal)
                val d = directedPlane.first.cross(toTarget).dot(directedPlane.normal)

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
            val target = closestFace.parent ?: closestFace
            target.holes.add(innerFace)
            innerFace.parent = target

            if(target.side == Side.Unknown){
                if(innerFace.side != Side.Unknown){
                    target.side = innerFace.side

                    for( hole in target.holes ){
                        hole.side = target.side
                    }
                }
            }else{
                if(innerFace.side != Side.Unknown){
                    if(innerFace.side != target.side){
                        throw RuntimeException("what")
                    }
                }else{
                    innerFace.side = target.side
                }
            }
        }
    }

    return orderedFaces.filter { !it.clockwise }
}

/*

fun main() {



    val leftFrontBottom = Node(PointD(-0.5,-0.5))
    val rightFrontBottom = Node(PointD(0.5,-0.5))
    val rightBackBottom = Node(PointD(0.5,0.5))
    val leftBackBottom = Node(PointD(-0.5,0.5))

    val leftFrontTop = Node(PointD(-0.5,-0.5, 1.0))
    val rightFrontTop = Node(PointD(0.5,-0.5, 1.0))
    val rightBackTop = Node(PointD(0.5,0.5, 1.0))
    val leftBackTop = Node(PointD(-0.5,0.5, 1.0))

    val leftBottom = Node(PointD(0.4, 0.0, -1.0))
    val rightBottom = Node(PointD(0.6, 0.0, -1.0))
    val leftTop = Node(PointD(0.4, 0.0, 1.0))
    val rightTop = Node(PointD(0.6, 0.0, 1.0))

    val bottom = listOf(
        leftBackBottom,
        rightBackBottom,
        rightFrontBottom,
        leftFrontBottom,
    )

    val top = listOf(
        leftFrontTop,
        rightFrontTop,
        rightBackTop,
        leftBackTop,
    )

    val right = listOf(
        rightFrontBottom,
        rightBackBottom,
        rightBackTop,
        rightFrontTop
    )

    val tool = listOf(
        leftBottom,
        rightBottom,
        rightTop,
        leftTop
    )

    val baseVolume = FacedVolume(
        listOf(
            PolygonFace(bottom, Plane.fromPoints(bottom.map { it.point })),
            PolygonFace(top, Plane.fromPoints(top.map { it.point })),
            PolygonFace(right, Plane.fromPoints(right.map { it.point })),
        )
    )

    val toolVolume = FacedVolume(
        listOf(
            PolygonFace(tool, Plane.fromPoints(tool.map { it.point })),
        )
    )

    val result = addVolumes(baseVolume, toolVolume)

    println("finish")
}

*/
/*
fun main() {
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

    val base = Extrude(PolygonFace(list.map { Node(it) }), DirectedPlane.XY,   Const(1.0))
    val tool = Extrude(PolygonFace(list2.map { Node(it) }), DirectedPlane.XY,  Const(2.0))

    val result = addVolumes(base, tool)

    println("finish")
}
*/

