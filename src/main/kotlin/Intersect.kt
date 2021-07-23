import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.sign


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

    val s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y)
    val t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y)

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

class Node(
    val p: PointD
) : Comparable<Node> {
    val edges = mutableListOf<Edge>()
    val leftEdges = mutableListOf<Edge>()

    override fun compareTo(other: Node): Int {
        if (p.x == other.p.x) return p.y.compareTo(other.p.y)
        return p.x.compareTo(other.p.x)
    }
}

class Edge(val source : Node, val target: Node){
    var connection: Edge? = null
    lateinit var opposite: Edge
    //var inner = false
    var index = -1
}

fun Edge.orientationTo(other: Edge) : Int{
    return (target.p - source.p).cross(other.target.p - other.source.p).sign.toInt()
}

fun Edge.orientationTo(other: PointD) : Int{
    return (target.p - source.p).cross(other - source.p).sign.toInt()
}

fun rotateComparator() : Comparator<Edge>{
    return Comparator{
        a,b -> a.orientationTo(b)
    }
}

fun ArrayList<Edge>.search(point: PointD) : Int{
    var left = 0
    var right = size

    while(left < right){
        val middlePos = (right - left) / 2
        val middle = this[middlePos]

        if( middle.orientationTo(point) > 0 ){
            left = middlePos
        }else{
            right = middlePos
        }
    }

    return left
}

fun findFaces(arr: List<LineD>): List<LineD> {
    removeIntersections(arr)

    val ordered = mutableListOf<LineD>()
    for(line in arr){
        if (line.p0.x > line.p1.x) {
            ordered.add(LineD(line.p1, line.p0))
        }else{
            ordered.add(line)
        }
    }

    val pointMap = HashMap<PointD, Node>()

    for (line in ordered) {
        val left = pointMap.computeIfAbsent(line.p0){Node(it)}
        val right = pointMap.computeIfAbsent(line.p1){Node(it)}

        val a = Edge(left, right)
        val b = Edge(right, left)

        a.opposite = b
        b.opposite = a

        left.edges.add(a)
        right.edges.add(b)
    }


    val nodes = pointMap.values.filter { it.leftEdges.size + it.edges.size >= 2 }.sorted()

    for (node in nodes) {
        node.edges.sortWith { a, b ->
            if(a.source.p.x < 0 && b.target.p.x < 0 ){
                if(a.source.p.y < 0 && b.target.p.y > 0 ){
                    return@sortWith 1
                }else if(a.source.p.x > 0 && b.target.p.x < 0 ){
                    return@sortWith -1
                }
            }

            a.orientationTo(b)
        }
    }

    val scanline = ArrayList<Edge>()

    for(node in nodes){
        for(i in node.edges.indices){
            val top = node.edges[i]
            val bottom = node.edges[(i+1)%node.edges.size]
            bottom.connection = top
        }
    }

    for(node in nodes){
        for(edge in edges){

        }

        val pos = scanline.search(node.p)

        var outside = true
        if(pos != 0){
            val a = scanline[pos - 1]
            val b = scanline[pos]

            outside = a.index != b.index
        }

        scanline.addAll(pos, node.rightEdges)
    }





    return emptyList()
}

fun main() {
    val a = PointD(0.0,0.0)
    val b = PointD(1.0,1.0)
    val c = PointD(2.0,0.0)
    val d = PointD(1.0,-1.0)

    val e = PointD(0.2,0.0)
    val f = PointD(0.7,0.5)
    val g = PointD(0.7,-0.5)



    findFaces(listOf(
        LineD(a, d),
        LineD(a, b),
        LineD(b, c),
        LineD(d, c),

        LineD(e, f),
        LineD(e, g),
        LineD(g, f),
    ))
}
