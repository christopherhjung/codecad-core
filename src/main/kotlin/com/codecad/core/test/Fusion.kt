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
class PlaneSlice(val face: RoutedFace, val plane: Plane, var startEdge: Edge? = null, var endEdge: Edge? = null, var startNode : Node? = null, var endNode: Node? = null){

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

            val leftPlane = leftActive.face.plane
            for (rightActive in otherActive.values) {
                val rightPlane = rightActive.face.plane

                if(leftPlane.normal == rightPlane.normal){
                    continue
                }

                val leftIntersections = findIntersections(leftActive.face, rightPlane)
                val rightIntersections = findIntersections(rightActive.face, leftPlane)

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
                        0 -> PlaneSlice(leftActive.face, rightPlane)
                        1 -> PlaneSlice(rightActive.face, leftPlane)
                        else -> throw RuntimeException("")
                    }
                }

                val slices = Array(2){ createSlice(it) }

                var started: Intersection? = null
                var finishSegment : PlaneSlice? = null

                val sizeBefore = resultSlices.size

                for(intersectionEvent in intersectionEvents){
                    val currentIndex = intersectionEvent.index
                    val otherIndex = 1 - currentIndex
                    inside[currentIndex] = !inside[currentIndex]
                    last[currentIndex] = intersectionEvent.intersection

                    val currentSlice = slices[currentIndex]
                    val otherSlice = slices[otherIndex]

                    if(inside[currentIndex] && inside[ otherIndex ] ){
                        currentSlice.startEdge = intersectionEvent.intersection.edge
                        currentSlice.startNode = intersectionEvent.intersection.position

                        if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                            otherSlice.startEdge = last[otherIndex]!!.edge
                        }

                        otherSlice.startNode = currentSlice.startNode
                        started = intersectionEvent.intersection
                    }else if(!inside[intersectionEvent.index] ){
                        if(started != null){
                            currentSlice.endEdge = intersectionEvent.intersection.edge
                            currentSlice.endNode = intersectionEvent.intersection.position

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)

                            finishSegment = currentSlice
                            started = null
                            continue
                        }else if(finishSegment != null){
                            if(last[otherIndex]!!.position.point.squaredDistanceTo(intersectionEvent.intersection.position.point) < 1e-8){
                                currentSlice.endEdge = intersectionEvent.intersection.edge
                            }

                            currentSlice.endNode = finishSegment.endNode

                            resultSlices.add(currentSlice)
                            slices[currentIndex] = createSlice(currentIndex)
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
class FaceInformation(val slices: MutableList<PlaneSlice> = mutableListOf(), val nodes: MutableList<Node> = mutableListOf(), val edges: MutableList<Edge> = mutableListOf())



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

    fun addCut(edge: Edge, node: Node, branch: Edge, plane: Plane){
        if(node != branch.source){
            throw RuntimeException("")
        }
/*
        if(edge.source.point == node.point || edge.target.point == node.point){
            throw RuntimeException("--")
        }*/

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

        cuts[node] = branch
    }
}

fun applyPlaneSlices(slices: List<PlaneSlice> , assignmentTable : Map<Plane, Owner>) : FaceAssignment{

    val map = HashMap<RoutedFace, MutableList<PlaneSlice>>()

    for(planeSlice in slices){
        map.computeIfAbsent(planeSlice.face){ mutableListOf() }.add(planeSlice)
    }

    val edgeCuts = mutableMapOf<EdgeCut, CutInformation>()
    val connects = mutableMapOf<Node, MutableList<Edge>>()
    val edges = mutableMapOf<Plane, MutableSet<Edge>>()
    val sideAssignment = mutableMapOf<Edge, Side>()

    fun check(edge: Edge?, node: Node, cut: Edge, plane: Plane){
        if(edge != null) {
            if(edge.target !== node && edge.source !== node){
                edgeCuts.computeIfAbsent(EdgeCut(edge)) { CutInformation() }
                    .addCut(edge, node, cut, plane)
            }
        }else{
            connects.computeIfAbsent(cut.source){ mutableListOf()}.add(cut)
        }
    }

    for((face, slices) in map.entries){
        edges.computeIfAbsent(face.plane){ mutableSetOf() }.addAll(face.edges())

        for( hole in face.holes ){
            val hole = RoutedFace.from(hole)
            edges.computeIfAbsent(face.plane){ mutableSetOf() }.addAll(hole.edges())
        }

        fun createEdge(source: Node, target: Node) : Edge{
            if(abs(face.plane.distanceTo(target.point)) > 1e-8 ){
                println("error")
            }
            return Edge(source, target)
        }

        for( slice in slices ){
            if(slice.startEdge?.source === slice.startNode!! ){
                if(slice.endEdge?.target === slice.endNode!!){
                    continue
                }
            }else if(slice.startEdge?.target === slice.startNode!!){
                if(slice.endEdge?.source === slice.endNode!!){
                    continue
                }
            }

            //edge insert
            val startNode = slice.startNode!!
            val endNode = slice.endNode!!

            val forward = createEdge(startNode, endNode)
            val backward = createEdge(endNode, startNode)

            forward.connect(backward)
            backward.connect(forward)

            edges.computeIfAbsent(face.plane){ mutableSetOf() }.add(forward)
            edges.computeIfAbsent(face.plane){ mutableSetOf()}.add(backward)

            Edge.twinEachOther(forward, backward)

            //side detection
            val direction = endNode.point - startNode.point
            val forwardIsOutside = direction.cross(slice.plane.normal).dot(slice.face.plane.normal)  > 0

            if(sideAssignment.containsKey(forward) ||sideAssignment.containsKey(backward)){
                throw RuntimeException("--")
            }

            if(forwardIsOutside){
                sideAssignment[forward] = Side.Outside
                sideAssignment[backward] = Side.Inside
            }else{
                sideAssignment[forward] = Side.Inside
                sideAssignment[backward] = Side.Outside
            }

            check(slice.startEdge, startNode, forward, face.plane)
            check(slice.endEdge, endNode, backward, face.plane)
        }
    }

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

    val result = FaceAssignment()
    for((plane, planeEdges) in edges){
        if(assignmentTable[plane] == Owner.Base){
            println("tool")
        }

        val faces = generateFaces(planeEdges, plane)

        for( face in faces ){
            propagateSides(face, sideAssignment )
        }

        if(assignmentTable[plane] == Owner.Tool){
            result.toolFaces.addAll(faces)
        }else{
            result.baseFaces.addAll(faces)
        }
    }

    resolveMissingSideAssignment(result.toolFaces)
    resolveMissingSideAssignment(result.baseFaces)

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

fun resolveMissingSideAssignment( faces: List<RoutedFace> ){
    val assignedFaces = faces.filter{it.side == Side.Unknown}

    println(assignedFaces)
    /*
    val overallEdges = faces.map { it.edges() + it.holes.filterIsInstance<RoutedFace>().map { it.edges() }.flatten() }.flatten()

    val assignedEdges = overallEdges.filter { it.side != Side.Unknown }.toMutableSet()

    while(assignedEdges.isNotEmpty()){
        val assignedEdge = assignedEdges.first()
        assignedEdges.remove(assignedEdge)

        if( assi ){

        }

    }*/
}

fun propagateSides(face: RoutedFace, sideAssignment: MutableMap<Edge, Side>) {
    val edges = face.edges() + face.holes.filterIsInstance<RoutedFace>().map { it.edges() }.flatten()
    val existsOutside = edges.any { sideAssignment[it] == Side.Outside }
    val existsInside = edges.any { sideAssignment[it] == Side.Inside }

    if(!existsInside && !existsOutside){
        return
    }

    if(existsInside && existsOutside){
        throw RuntimeException("--")
    }

    val side = if(existsInside){
        Side.Inside
    }else{
        Side.Outside
    }
/*
    edges.forEach{
        sideAssignment[it] = side
    }*/

    face.side = side
}
/*
fun propagateSides2(edges: Collection<Edge>) {

    val edgeSet = edges.toList()

    val hasSide = edgeSet.filter { it.side != Side.Unknown }.toMutableSet()
    val visited = hasSide.toMutableSet()

    fun add(edge: Edge, side: Side){
        if(!visited.contains(edge)){
            if(edge.side != Side.Unknown && edge.side != side){
                println(side)
                throw RuntimeException("--")
            }

            edge.side = side
            visited.add(edge)
            hasSide.add(edge)
        }
    }

    while(hasSide.isNotEmpty()){
        var sidedEdge = hasSide.first()
        hasSide.remove(sidedEdge)

        add(sidedEdge.next!!, sidedEdge.side)
        add(sidedEdge.prev!!, sidedEdge.side)
    }
}*/

fun generateFaces(edges: Collection<Edge>, plane: Plane) : List<RoutedFace>{
    val faces = mutableListOf<RoutedFace>()
    val queue = edges.toMutableSet()
    while(queue.isNotEmpty()){
        val start = queue.first()
        queue.remove(start)

        val area = PointD()
        var current = start

        var counter = 0
        while(true){
            if(counter > 10000){
                throw RuntimeException("--")
            }

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


fun addVolumes(base: Volume, tool: Volume) : FacedVolume{
    val base = RoutedVolume.from(base)
    val tool = RoutedVolume.from(tool)
    val result = computePlaneSlices(base, tool)

    val ownerAssignment = generateOwnerAssignment(base, tool)
    val faceAssignment = applyPlaneSlices(result.slices, ownerAssignment)

    for(uncut in result.uncut){
        uncut.side = Side.Outside
        if(ownerAssignment[uncut.plane] == Owner.Tool){
            faceAssignment.toolFaces
        }else{
            faceAssignment.baseFaces
        }.add(uncut)
    }

    //estimateSides(faceAssignment.toolFaces )
    //estimateSides(faceAssignment.baseFaces)

    val baseFaces = faceAssignment.baseFaces.filter { it.side != Side.Inside }
    val toolFaces = faceAssignment.toolFaces.filter { it.side == Side.Inside }

    return FacedVolume(baseFaces + toolFaces.map {  RoutedFace(invertEdges(it.root), it.plane.flip()) })
       /* when (it) {
            //is PolygonFace -> PolygonFace(it.positions.reversed(), it.plane.flip())
            //is ConvexFace -> ConvexFace(it.positions.reversed())
            //is RoutedFace -> RoutedFace(invertEdges(it.root), it.plane.flip())
            else -> TODO()
        }
    })*/
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
