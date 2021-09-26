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


class Node(val point : PointD){
    override fun toString(): String {
        return "Node(point=$point)"
    }
}

class Corner(val node: Node){
    val edges = mutableListOf<Edge>()

    fun containsTarget(target: Node) : Edge?{
        for(cornerEdge in edges){
            if(cornerEdge.target === target){
                return cornerEdge
            }
        }
        return null
    }

    fun addEdge(edge: Edge){
        if(edge.source !== node){
            throw RuntimeException("ss")
        }

        if(edge.source.point.distanceTo(PointD(0.2,-0.5, 1.0)) < 0.01 &&
            edge.target.point.distanceTo(PointD(0.2,-0.5, 0.0)) < 0.01){
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

class Edge(val source: Node, val target : Node){
    var prev: Edge? = null
    var next: Edge? = null
    lateinit var twin : Edge

    fun connect(other: Edge){
        if(target != other.source){
            throw RuntimeException("--")
        }

        if(next != null || other.prev != null){
            println("--")
        }

        next = other
        other.prev = this
    }

    companion object{
        fun twinEachOther(left: Edge, right: Edge){
            if(left.source !== right.target || right.source !== left.target){
                throw RuntimeException("--")
            }

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

    fun edges() : Iterable<Edge>{
        return Iterable {
            var start : Edge = this
            var current : Edge = this
            var first = true
            object : Iterator<Edge>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): Edge {
                    first = false
                    val result =  current
                    current = current.next!!
                    return result
                }
            }
        }
    }

    override fun toString(): String {
        return "Edge(source=$source, target=$target)"
    }


}




class PlaneEvent(val volume : RoutedVolume, val face: RoutedFace, val point: PointD, val start : Boolean)
class PlaneSlice(

    ){
    var baseEntry: Int = 0
    var startNode : Node? = null
    var endNode: Node? = null
    val entries = Array(2){ PlaneSliceEntry() }

    val baseFace: RoutedFace
        get() = entries[baseEntry].face!!
    val toolFace: RoutedFace
        get() = entries[1 - baseEntry].face!!
    val startBaseEdge: Edge?
        get() = entries[baseEntry].startEdge
    val endBaseEdge: Edge?
        get() = entries[baseEntry].endEdge
    val startToolEdge: Edge?
        get() = entries[1 - baseEntry].startEdge
    val endToolEdge: Edge?
        get() = entries[1 - baseEntry].endEdge
}

class PlaneSliceEntry{
    var face: RoutedFace? = null
    var startEdge: Edge? = null
    var endEdge: Edge? = null
}

class EdgeSlice(val a: Node , val b: Node , val plane: Plane?){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeSlice) return false

        if (!(a === other.a && b === other.b || a === other.b && b === other.a)) return false
        if (plane !== other.plane) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ( a.hashCode() + 1) *  (b.hashCode() + 1)
        result = 31 * result + (plane?.hashCode() ?: 0)
        return result
    }
}

class Intersection(val edge : Edge, val position: Node)

val edgeSlices = HashMap<EdgeSlice, Node>()

fun findIntersections(face: RoutedFace, plane: Plane) : List<Intersection>{
    val result = mutableListOf<Intersection>()
    for(edge in face){
        val start = edge.source
        val end = edge.target

        val edgeSlice = EdgeSlice(start, end, plane)

        if(edgeSlices.containsKey(edgeSlice)){
            val intersectionPosition = edgeSlices[edgeSlice]
            result.add(Intersection(edge, intersectionPosition!!))
        }else{
            val startDistance = plane.distanceTo(start.point)
            val endDistance = plane.distanceTo(end.point)

            val intersection = when {
                abs(startDistance) < 1e-8 -> start
                abs(endDistance) < 1e-8 -> end
                else -> {
                    if( startDistance * endDistance <= 0 && startDistance != endDistance) {
                        Node(
                            start.point + (end.point - start.point) *
                                    startDistance / (startDistance - endDistance)
                        )
                    }else{
                        null
                    }
                }
            }

            if(intersection != null){
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

    for(leftActive in baseEventQueue) {
        val (currentActive, otherActive) = when{
            leftActive.volume === base -> Pair(activeBase, activeTool)
            leftActive.volume === tool -> Pair(activeTool, activeBase)
            else -> throw RuntimeException("ss")
        }

        if(leftActive.start){
            currentActive[leftActive.face] = leftActive

            val basePlane = leftActive.face.plane
            for (rightActive in otherActive.values) {
                val toolPlane = rightActive.face.plane

                if(basePlane.normal == toolPlane.normal){
                    continue
                }

                val leftIntersections = findIntersections(leftActive.face, toolPlane)
                val rightIntersections = findIntersections(rightActive.face, basePlane)

                if(leftIntersections.isEmpty() || rightIntersections.isEmpty()){
                    continue
                }

                val intersectionEvents = TreeSet(testComparator)

                val line = Line.fromPlanes(basePlane, toolPlane)
                for(intersection in leftIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.position.point), 0))
                }
                for(intersection in rightIntersections){
                    intersectionEvents.add(Event(intersection, line.direction.dot(intersection.position.point), 1))
                }

                val inside = BooleanArray(2){false}
                val last = Array<Intersection?>(2){null}

                var slice = PlaneSlice()

                var started: Intersection? = null
                var finishSegment : PlaneSlice? = null

                val sizeBefore = resultSlices.size

                for(intersectionEvent in intersectionEvents){
                    val currentIndex = intersectionEvent.index
                    val otherIndex = 1 - currentIndex
                    inside[currentIndex] = !inside[currentIndex]
                    last[currentIndex] = intersectionEvent.intersection

                    var currentEntry = slice.entries[currentIndex]
                    //var otherEntry = slice.entries[otherIndex]

                    if(inside[currentIndex] && inside[ otherIndex ] ){
                        slice = PlaneSlice()
                        currentEntry = slice.entries[currentIndex]
                        val otherEntry = slice.entries[otherIndex]

                        slice.baseEntry = intersectionEvent.index
                        slice.entries[0].face = if(intersectionEvent.index == 0) leftActive.face else rightActive.face
                        slice.entries[1].face = if(intersectionEvent.index == 1) rightActive.face else leftActive.face

                        currentEntry.startEdge = intersectionEvent.intersection.edge
                        slice.startNode = intersectionEvent.intersection.position

                        if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                            otherEntry.startEdge = last[otherIndex]!!.edge
                        }

                        started = intersectionEvent.intersection
                    }else if(!inside[intersectionEvent.index] ){
                        if(started != null){
                            currentEntry.endEdge = intersectionEvent.intersection.edge
                            slice.endNode = intersectionEvent.intersection.position

                            resultSlices.add(slice)

                            finishSegment = slice
                            started = null
                            continue
                        }else if(finishSegment != null){
                            if(slice.endNode!!.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                                currentEntry.endEdge = intersectionEvent.intersection.edge
                            }
                        }
                    }

                    finishSegment = null
                }


                val sizeAfter = resultSlices.size

                if(sizeBefore != sizeAfter){
                    uncutFaces.remove(leftActive.face)
                    uncutFaces.remove(rightActive.face)
                }
            }
        }else{
            currentActive.remove(leftActive.face)
        }
    }

    return PlaneSliceResult(resultSlices, uncutFaces)
}

class FaceAssignment(val baseFaces : MutableList<RoutedFace> = mutableListOf(), val toolFaces : MutableList<RoutedFace> = mutableListOf())


class EdgeCut(val edge: Edge){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeCut) return false

        return edge === other.edge || edge === other.edge.twin
    }

    override fun hashCode(): Int {
        return (System.identityHashCode(edge) + 1) * (System.identityHashCode(edge.twin) + 1)
    }
}

class CutInformation(){
    var forward: Edge? = null
    val forwardCuts = mutableMapOf<Node, Edge>()
    val backwardCuts = mutableMapOf<Node, Edge>()
    var forwardPlane : Plane? = null
    var backwardPlane : Plane? = null

    fun addCut(edge: Edge, branch: Edge, plane: Plane){
        val cuts = if(forward == null){
            forwardPlane = plane
            forward = edge
            forwardCuts
        }else if(forward === edge){
            forwardCuts
        }else if(forward!!.twin === edge){
            backwardPlane = plane
            backwardCuts
        }else{
            throw RuntimeException("error")
        }

        cuts[branch.source] = branch
    }
}

fun invertFace(volume : RoutedVolume) : RoutedVolume{
    //val edges = (face.edges() + face.holes.filterIsInstance<RoutedFace>().map { it.edges() }.flatten()).toMutableSet()

    val mapping = mutableMapOf<Edge, Edge>()
    val faces = mutableListOf<RoutedFace>()

    for( face in volume.faces ){
        val root = face.root

        for( (org, new) in root.edges().map {
            val newEdge = Edge(it.target, it.source)
            mapping[it] = newEdge
            Pair(it, newEdge)
        } ){
            new.next = mapping[org.prev]
            new.prev = mapping[org.next]

            val twin = mapping[org.twin]
            if(twin != null){
                Edge.twinEachOther(new, twin)
            }
        }

        faces.add(RoutedFace(mapping[root]!!, face.plane.flip()))
    }

    return RoutedVolume(faces)
}

fun applyPlaneSlices(slices: List<PlaneSlice> , assignmentTable : Map<Plane, Owner>) : MutableList<RoutedFace>{

    val map = HashMap<RoutedFace, MutableList<PlaneSlice>>()

    for(planeSlice in slices){
        map.computeIfAbsent(planeSlice.entries[0].face!!){ mutableListOf() }.add(planeSlice)
    }

    val edgeCuts = mutableMapOf<EdgeCut, CutInformation>()
    val connects = mutableMapOf<Node, MutableList<Edge>>()
    val edges = mutableMapOf<Plane, MutableSet<Edge>>()

    val cuttedFace = mutableSetOf<RoutedFace>()

    fun check(edge: Edge?, cut: Edge, plane: Plane){
        if(edge != null) {
            if(edge.target !== cut.source && edge.source !== cut.source){
                edgeCuts.computeIfAbsent(EdgeCut(edge)) { CutInformation() }
                    .addCut(edge, cut, plane)
            }
        }
        connects.computeIfAbsent(cut.source){ mutableListOf()}.add(cut)
    }


    fun work(startNode: Node, endNode: Node,
             startEdge: Edge?, endEdge: Edge?,
             leftFace: RoutedFace, rightFace: RoutedFace,
             keepOutside: Boolean) : Edge{
        if(startEdge?.source === startNode ){
            if(endEdge?.target === endNode){
                return startEdge.prev!!
            }
        }else if(startEdge?.target === startNode){
            if(endEdge?.source === endNode){
                return endEdge.next!!
            }
        }

        //side detection
        val direction = endNode.point - startNode.point
        val baseForwardIsOutside = direction.cross(rightFace.plane.normal).dot(leftFace.plane.normal) > 0

        val edge: Edge
        if(keepOutside xor baseForwardIsOutside){
            edge = Edge(endNode, startNode)

            if(endEdge != null ){
                cuttedFace.add(leftFace)
            }

            check(endEdge, edge, leftFace.plane)
        }else{
            edge = Edge(startNode, endNode)

            if(startEdge != null ){
                cuttedFace.add(leftFace)
            }



            check(startEdge, edge, leftFace.plane)
        }

        edges.computeIfAbsent(leftFace.plane){ mutableSetOf()}.add(edge)

        return edge
    }

   // for((face, slices) in map.entries){
        /*edges.computeIfAbsent(face.plane){ mutableSetOf() }.addAll(face.edges())

        for( hole in face.holes ){
            val hole = RoutedFace.from(hole)
            edges.computeIfAbsent(face.plane){ mutableSetOf() }.addAll(hole.edges())
        }*/



        for( slice in slices ){
            val first = work(slice.startNode!!, slice.endNode!!, slice.startBaseEdge, slice.endBaseEdge, slice.baseFace, slice.toolFace, true)
            val second = work(slice.startNode!!, slice.endNode!!, slice.startToolEdge, slice.endToolEdge, slice.toolFace, slice.baseFace, false)

            Edge.twinEachOther(first, second)
        }
    //}

    val test = map.keys - cuttedFace

    for(cut in edgeCuts.values){
        val forwardEdge = cut.forward!!
        val leftDirection = (forwardEdge.target.point - forwardEdge.source.point)//.normalized()
        val nodes = (cut.forwardCuts.keys + cut.backwardCuts.keys + listOf(forwardEdge.source, forwardEdge.target)).sortedBy { it.point.dot(leftDirection) }

        if(cut.forwardPlane != null){
            edges[cut.forwardPlane!!]!!.remove(forwardEdge)
        }

        if(cut.backwardPlane != null){
            edges[cut.backwardPlane!!]!!.remove(forwardEdge.twin)
        }

        var lastForward: Edge = forwardEdge.prev!!
        var lastBackward: Edge = forwardEdge.twin.next!!
        nodes.lookahead().map { (leftFirst, leftSecond) ->
            val forward = Edge(leftFirst, leftSecond)
            val backward = Edge(leftSecond, leftFirst)

            Edge.twinEachOther(forward, backward)

            val forwardBranch = cut.forwardCuts[leftSecond]
            lastForward = if(forwardBranch != null){
                lastForward.connect(forward)
                forward.connect(forwardBranch)
                forwardBranch.twin
            }else{
                lastForward.connect(forward)
                forward
            }

            val backwardBranch = cut.backwardCuts[leftSecond]
            lastBackward = if(backwardBranch != null){
                backward.connect(lastBackward)
                backwardBranch.twin.connect(backward)
                backwardBranch
            }else{
                backward.connect(lastBackward)
                backward
            }

            if(lastForward.target != leftSecond){
                throw RuntimeException("--")
            }

            if(lastBackward.source != leftSecond){
                throw RuntimeException("--")
            }

            forward
        }

        lastForward.connect(forwardEdge.next!!)
        forwardEdge.twin.prev!!.connect(lastBackward)
    }

    for( edges in connects.values ){
        if(edges.size != 2){
            throw RuntimeException("--")
        }

        rotaryConnect(edges)
    }

    val result = mutableListOf<RoutedFace>()
    for((plane, planeEdges) in edges){
        val faces = generateFaces(planeEdges, plane)
        result.addAll(faces)
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
                top.twin.connect(bottom)
            }else{
                TODO("error?")
            }
        }
    }
}

fun rotaryConnect(edges : Collection<Edge>){
    for((top, bottom) in edges.rollover()){
        top.twin.connect(bottom)
    }
}

fun generateFaces(edges: Collection<Edge>, plane: Plane) : List<RoutedFace>{
    val faces = mutableListOf<RoutedFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val start = queue.first()
        queue.remove(start)

        val area = PointD()
        var current = start

        val points = mutableListOf<PointD>()
        var counter = 0
        while(true){
            if(counter > 10000){
                throw RuntimeException("--")
            }


            points.add(current.source.point)
            area += current.source.point.cross(current.target.point)

            if(current.target === start.source){
                break
            }

            current = current.next!!
            queue.remove(current)
            counter++
        }

        //val plane = current.plane
        val face = RoutedFace(current, plane)
        face.area = area.length() / 2
        face.clockwise = area.dot(plane.normal) < 0

        faces.add(face)
    }

    return combineFaces(faces, plane)
}

fun getLeftmostPoint(polygonFace: PolygonFace, direction: PointD) : Node {
    return polygonFace.positions.minByOrNull { it.point.dot(direction) }!!
}

fun getLeftmostPoint(polygonFace: RoutedFace, direction: PointD) : Node {
    return polygonFace.nodes().minByOrNull { it.point.dot(direction) }!!
}

fun generateOwnerAssignment(base: RoutedVolume, tool: RoutedVolume) : Map<Plane, Owner>{
    val map = mutableMapOf<Plane, Owner>()
    for( face in base.faces ){
        if(map.containsKey(face.plane)){
            throw RuntimeException("--")
        }

        map[face.plane] = Owner.Base
    }

    for( face in tool.faces ){
        if(map.containsKey(face.plane)){
            throw RuntimeException("--")
        }

        map[face.plane] = Owner.Tool
    }
    return map
}

enum class VolumeOperation{
    And, Or, Sub
}

fun combineVolumes(base: Volume, tool: Volume, op: VolumeOperation) : RoutedVolume{
    val base = RoutedVolume.from(base)
    var tool = RoutedVolume.from(tool)

    tool = invertFace(tool)
    val result = computePlaneSlices(base, tool)

    val ownerAssignment = generateOwnerAssignment(base, tool)
    val resultVolume = applyPlaneSlices(result.slices, ownerAssignment)



    return RoutedVolume(resultVolume + result.uncut)
}

fun invertEdges(root: Edge) : Edge{
    var start: Edge? = null
    var last: Edge? = null
    for( edge in root.edges() ){
        val forward = Edge(edge.source, edge.target)
        val backward = Edge(edge.target, edge.source)

        Edge.twinEachOther(forward, backward)

        if(last != null){
            last.connect(forward)
            backward.connect(last.twin)
        }else{
            start = forward
        }

        last = forward
    }

    last!!.connect(start!!)
    start.twin.connect(last.twin)

    return start.twin
}

fun combineFaces(faces: List<RoutedFace>, plane: Plane) : List<RoutedFace>{
    val directedPlane = DirectedPlane.from(plane)

    val leftMostMap = mutableMapOf<RoutedFace, PointD>()
    fun getLeftmost(face: RoutedFace) : PointD{
        return leftMostMap.computeIfAbsent(face) {getLeftmostPoint(face, directedPlane.first).point}
    }

    val orderedFaces = faces.sortedBy { getLeftmost(it).dot(directedPlane.first) }
    val inners = orderedFaces.filter { it.clockwise }

    for(innerFace in inners) {
        var maxUnitOffset: Double = -Double.MAX_VALUE
        var closestFace: RoutedFace? = null

        val leftmost = getLeftmost(innerFace)
        val pointUnitOffset = leftmost.dot(directedPlane.first)

        for (outerFace in orderedFaces) {
            if (outerFace === innerFace) {
                break
            }

            for ((source, target) in outerFace.nodes().rollover()) {
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
