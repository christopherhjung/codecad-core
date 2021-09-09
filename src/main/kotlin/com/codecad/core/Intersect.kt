package com.codecad.core

import com.codecad.common.LineD
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.test.Corner
import com.codecad.core.test.Edge
import com.codecad.core.test.Node
import com.codecad.core.test.generateFaces
import java.util.*


fun findIntersection(line1: LineD, line2: LineD): PointD? {
    val p0_x = line1.p0.x
    val p0_y = line1.p0.y
    val p1_x = line1.p1.x
    val p1_y = line1.p1.y
    val p2_x = line2.p0.x
    val p2_y = line2.p0.y
    val p3_x = line2.p1.x
    val p3_y = line2.p1.y

    val s1_x = p1_x - p0_x
    val s1_y = p1_y - p0_y
    val s2_x = p3_x - p2_x
    val s2_y = p3_y - p2_y

    val a = 1 / (-s2_x * s1_y + s1_x * s2_y)
    val s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) * a
    val t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) * a

    val epsilon = 1e-5
    if (s - epsilon > 0 && s + epsilon < 1 && t - epsilon > 0 && t + epsilon < 1) {
        val x = p0_x + (t * s1_x)
        val y = p0_y + (t * s1_y)
        return PointD(x, y)
    }

    return null
}

class Event(
    val p: PointD,
    val line: LineD,
    val isLeft: Boolean
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        if (p.x == other.p.x) return p.y.compareTo(other.p.y)
        return p.x.compareTo(other.p.x)
    }
}


fun removeIntersections(arr: List<LineD>): List<LineD> {

    val events = LinkedList<Event>()

    val ordered = mutableListOf<LineD>()
    for(line in arr){
        if (line.p0.x > line.p1.x) {
            ordered.add(LineD(line.p1, line.p0))
        }else{
            ordered.add(line)
        }
    }

    for (line in ordered) {
        events.add(Event(line.p0, line, true))
        events.add(Event(line.p1, line, false))
    }

    events.sort()

    val active = HashMap<LineD, Event>()
    val splittingPoints = HashMap<LineD, MutableList<PointD>>()
    for (event in events) {
        if (event.isLeft) {
            for (other in active.values) {
                val intersection = findIntersection(other.line, event.line)
                if (intersection != null) {
                    splittingPoints.computeIfAbsent(event.line){ mutableListOf()}.add(intersection)
                    splittingPoints.computeIfAbsent(other.line){ mutableListOf()}.add(intersection)
                }
            }

            active[event.line] = event
        } else {
            active.remove(event.line)
        }
    }

    val result = mutableListOf<LineD>()

    for( line in ordered ){
        val splits = splittingPoints[line]
        if( splits != null ){
            splits.sortBy { it.x }
            var left = line.p0
            for( split in splits ){
                result.add(LineD(left, split))
                left = split
            }
            result.add(LineD(left, line.p1))
        }else{
            result.add(line)
        }
    }

    return result
}

fun findFaces(arr2: List<LineD>): List<PolygonFace> {
    val arr = removeIntersections(arr2)

    val ordered = mutableListOf<LineD>()
    for (line in arr) {
        ordered.add(
            if (line.p0.x > line.p1.x) {
                LineD(line.p1, line.p0)
            } else {
                line
            }
        )
    }

    val pointMap = HashMap<PointD, Corner>()
    val edges = HashSet<Edge>()

    for (line in ordered) {
        val left = pointMap.computeIfAbsent(line.p0) { Corner(Node(it)) }
        val right = pointMap.computeIfAbsent(line.p1) { Corner(Node(it)) }

        val a = Edge(left, right)
        val b = Edge(right, left)

        a.twin = b
        b.twin = a

        left.edges.add(a)
        right.edges.add(b)
        edges.add(a)
        edges.add(b)
    }

    return generateFaces(Plane.XY, edges)
}

/*
data class Node(
    val p: PointD
) : Comparable<Node> {
    val edges = mutableListOf<Edge>()

    override fun compareTo(other: Node): Int {
        if (p.x == other.p.x) return p.y.compareTo(other.p.y)
        return p.x.compareTo(other.p.x)
    }
}*/

/*
data class Edge(val source : Node, val target: Node){
    var next: Edge? = null
    lateinit var twin: Edge
    var polygonFace: PolygonFace? = null

    //var inner = false
    var index = -1
}*/

/*
fun Edge.orientationTo(other: Edge) : Int{
    return (target.p - source.p).crossZ(other.target.p - other.source.p).sign.toInt()
}

fun Edge.orientationTo(other: PointD) : Int{
    return (target.p - source.p).crossZ(other - source.p).sign.toInt()
}

fun rotateComparator() : Comparator<Edge>{
    return Comparator{
        a,b -> a.orientationTo(b)
    }
}*/

/*
fun ArrayList<Edge>.search(point: PointD) : Int{
    var left = 0
    var right = size

    while(left < right){
        val middlePos = (right + left) / 2
        val middle = this[middlePos]
        val orientation = middle.orientationTo(point)

        if(orientation == 0){
            return middlePos
        }else if( orientation > 0 ){
            right = middlePos
        }else{
            left = middlePos + 1
        }
    }

    return left
}
*/

/*
fun findFaces(arr2: List<LineD>): List<PolygonFace> {
    val arr = removeIntersections(arr2)

    val ordered = mutableListOf<LineD>()
    for(line in arr){
        ordered.add(if (line.p0.x > line.p1.x) {
            LineD(line.p1, line.p0)
        }else{
            line
        })
    }

    val pointMap = HashMap<PointD, Corner>()
    val edges = HashSet<Edge>()

    for (line in ordered) {
        val left = pointMap.computeIfAbsent(line.p0){ Corner(Node(it)) }
        val right = pointMap.computeIfAbsent(line.p1){ Corner(Node(it)) }

        val a = Edge(left, right)
        val b = Edge(right, left)

        a.twin = b
        b.twin = a

        left.edges.add(a)
        right.edges.add(b)
        edges.add(a)
        edges.add(b)
    }

    for(node in pointMap.values){
        node.edges.sortBy {
            val aDirection = it.target.p - it.source.p
            atan2(aDirection.x, aDirection.y)
        }

        for(i in node.edges.indices){
            val top = node.edges[i]
            val bottom = node.edges[(i+1)%node.edges.size]
            top.twin.next = bottom
        }
    }

    val faces = mutableListOf<PolygonFace>()

    val queue = edges.toMutableList()
    while(queue.isNotEmpty()){
        val next = queue.first()
        queue.remove(next)

        var area = 0.0
        val points = mutableListOf<PointD>()
        var current = next
        val face = PolygonFace(points)
        while(true){
            points.add(current.target.p)
            current.polygonFace = face
            area += current.source.p.x * current.target.p.y -  current.target.p.x * current.source.p.y
            if(current.target === next.source){
                break
            }

            current = current.next!!
            queue.remove(current)
        }

        area /= 2

        face.area = abs(area)
        face.clockwise = area.sign < 0
        face.leftmost = getLeftmostPoint(face)
        println(area)
        faces.add(face)
    }

    val outers = mutableListOf<PolygonFace>()
    val inners = mutableListOf<PolygonFace>()

    for( face in faces ){
        if(!face.clockwise){
            outers.add(face)
        }else{
            inners.add(face)
        }
    }

    outers.sortByDescending { it.leftmost!!.x }

    val receivers = edges.filter { it.source.p.y > it.target.p.y }.sortedBy { min(it.source.p.x, it.target.p.x) }

    inners.sortBy { it.leftmost!!.x }

    for( inner in inners ){
        val leftmost = inner.leftmost!!

        var minValue: Double? = null
        var minEdge : Edge? = null
        for(edge in receivers){
            if(edge.source.p.y >= leftmost.y && edge.target.p.y <= leftmost.y){
                val pos = leftmost.x - (edge.target.p.x - (leftmost.y - edge.target.p.y) * (edge.target.p.x - edge.source.p.x) / (edge.source.p.y - edge.target.p.y))

                if( ( minValue == null || pos < minValue ) && pos > 0 ){
                    minValue = pos
                    minEdge = edge
                }
            }
        }

        val outer = minEdge?.polygonFace

        if(outer != null){
            val target = outer.parent ?: outer
            inner.parent = target
            target.children.add(inner)
        }
    }

    return outers
}

fun getLeftmostPoint(polygonFace: PolygonFace) : PointD {
    var leftMost: PointD? = null
    for( point in polygonFace.points ){
        if(leftMost == null || leftMost.x > point.x){
            leftMost = point
        }
    }
    return leftMost!!
}

*/
fun findFace(segments: List<LineD>, point: PointD) : PolygonFace?{
    val faces = findFaces(segments)

    for( face in faces ){
        val triangles = face.generateTriangles()

        for( triangle in triangles ){
            val points = triangle.points

            var found = true
            for( i in 0 until 3 ){
                val a = points[i]
                val b = points[(i + 1) % points.size]

                if((point - a.point).crossZ(b.point - a.point) > 0){
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
