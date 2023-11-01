package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.curve.Circle
import com.codecad.core.rollover
import kotlin.math.sin

enum class FaceBoundKind{
    OuterBound, InnerBound;

    fun invert() : FaceBoundKind{
        return when(this){
            OuterBound -> InnerBound
            InnerBound -> OuterBound
        }
    }
}

class FaceBound<T : Vec<T>>(var loop : Loop<T>, var sense: FaceBoundKind){

}
enum class EdgeOrientation{
    Forward, Backward;

    fun invert() : EdgeOrientation{
        return when(this){
            Forward -> Backward
            Backward -> Forward
        }
    }
}

class OrientedEdge<T : Vec<T>>(val edge : Edge<T>, val orientation : EdgeOrientation = EdgeOrientation.Forward){
    val start get() = if(orientation == EdgeOrientation.Forward) edge.bound.start else edge.bound.end
    val end get() = if(orientation == EdgeOrientation.Forward) edge.bound.end else edge.bound.start
    val bound get() = if(orientation == EdgeOrientation.Forward) edge.bound else edge.bound.let {
            EdgeBound(it.end, it.start, it.sense.invert())
        }

    fun normalized() : Edge<T>{
        return if(orientation == EdgeOrientation.Forward){
            edge
        }else{
            Edge(edge.curve.invert(), edge.bound.align())
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is OrientedEdge<*> &&
                edge == other.edge &&
                orientation == other.orientation
    }

    override fun hashCode(): Int {
        var result = edge.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun toString(): String {
        return "OrientedEdge(start=$start, end=$end)"
    }
}


fun Loop<Vec3>.computeArea() : Double{
    var polygonArea = Vec3.Zero

    for( loop in this ){
        val orientedEdge = loop.edge
        val edge = orientedEdge.edge
        val bound = orientedEdge.bound
        val curve = edge.curve

        if(bound == null){
            val radius = (curve as Circle).radius
            return Math.PI * radius * radius
        }

        val start = bound.start.point
        val end = bound.end.point

        if(curve is Circle){
            val center = curve.workplane.origin
            val radius = curve.radius

            //val angle = start.angleTo(end)


        }

        polygonArea += start.cross(end)
    }

    return polygonArea.length() * 0.5
}

fun Loop<Vec2>.computeAreaVec2(ignoreCurve : Boolean = false) : Double{
    var polygonArea = 0.0

    if(isClosed()){
        val curve = edge.edge.curve as Circle
        val radius = curve.radius
        val bound = edge.bound
        return if(bound.sense == Sense.Same){
            Math.PI * radius * radius
        }else{
            -Math.PI * radius * radius
        }
    }

    for( loop in this ){
        val orientedEdge = loop.edge
        val edge = orientedEdge.edge
        val bound = edge.bound
        var partialArea = 0.0

        when(val curve = edge.curve){
            is Circle -> {
                if(!ignoreCurve){
                    val sense = bound.sense

                    val center = curve.workplane.origin
                    val radius = curve.radius

                    val startVec = bound.start.point - center
                    val endVec = bound.end.point - center
                    val angle = if(sense == Sense.Same){
                        startVec.angleTo(endVec)
                    }else{
                        endVec.angleTo(startVec)
                    }

                    val segmentArea = radius * radius * (angle - sin(angle))
                    partialArea = if(sense == Sense.Same){
                        segmentArea
                    }else{
                        -segmentArea
                    }
                }
            }
        }

        val start = bound.start.point
        val end = bound.end.point

        partialArea += start.crossZ(end)

        if(orientedEdge.orientation == EdgeOrientation.Forward){
            polygonArea += partialArea
        }else{
            polygonArea -= partialArea
        }
    }

    return polygonArea * 0.5
}


class Loop<T : Vec<T>>(var edge : OrientedEdge<T>) : Iterable<Loop<T>>{
    lateinit var prev : Loop<T>
    lateinit var next : Loop<T>
    lateinit var face : Face
    var marker : Marker = Marker()
    var twin : Loop<T>? = null

    fun isClosed() : Boolean{
        return this === next
    }

    override fun iterator(): Iterator<Loop<T>> {
        return LoopIterator(this)
    }

    fun <R : Vec<R>> project(block : (T) -> R) : Loop<R>{
        for( (lhs, rhs) in rollover() ){

        }

        return null!!
    }

    fun followedBy(next : Loop<T>){
        val bound = edge.bound
        val nextBound = next.edge.bound
        if(bound.end !== nextBound.start){
            throw RuntimeException("xxx")
        }
        this.next = next
        next.prev = this
    }

    fun twinWith(next : Loop<T>){
        this.twin = next
        next.twin = this
    }

    fun star() : Iterable<Loop<T>>{
        val loop = this
        return object : Iterable<Loop<T>>{
            override fun iterator(): Iterator<Loop<T>> {
                return StarLoopIterator(loop)
            }
        }
    }

    fun nextPoints(num: Int) : List<T>{
        val result = arrayListOf<T>()
        for( (idx, loop) in this.withIndex() ){
            if(idx == num){
                break
            }
            result.add(loop.edge.start.point)
        }
        return result
    }

    fun nextVertices(num: Int) : List<Vertex<T>>{
        val result = arrayListOf<Vertex<T>>()
        for( (idx, loop) in this.withIndex() ){
            if(idx == num){
                break
            }
            result.add(loop.edge.start)
        }
        return result
    }

    fun validate() : Boolean{
        val visited = hashSetOf<Vertex<T>>()

        for( loop in this ){
            if(!visited.add(loop.edge.start)){
                return false
            }
            if(loop.face != face){
                return false
            }
        }

        return true
    }

    fun toString(sb: StringBuilder){
        var sep = ""
        val visited = hashSetOf<OrientedEdge<T>>()
        for(loop in this){
            val orientedEdge = loop.edge
            val bound = orientedEdge.bound
            sb.append(sep)
                .append(bound.start)
                .append("->")
                .append(bound.end)
                .append(" -- ")
                .append(orientedEdge.edge.curve::class.simpleName)

            sep = "\n"

            if(!visited.add(orientedEdge)){
                sb.append("Error!!!")
                break
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    companion object{
        fun <T : Vec<T>> wireCircular(edge: Edge<T>) : Loop<T> {
            val loop = Loop(OrientedEdge(edge))
            loop.followedBy(loop)
            return loop
        }

        fun <T : Vec<T>> wrap(edge: Edge<T>) : Loop<T> {
            return Loop(OrientedEdge(edge, EdgeOrientation.Forward))
        }

        fun <T : Vec<T>> twin(edge: Edge<T>) : Loop<T> {
            val loop = Loop(OrientedEdge(edge, EdgeOrientation.Forward))
            val twinLoop = Loop(OrientedEdge(edge, EdgeOrientation.Backward))
            loop.twinWith(twinLoop)
            return loop
        }

        fun <T : Vec<T>> wireCircular(vararg vertices: Vertex<T>) : Loop<T> {
            val edges = vertices.toList().rollover().map { (lhs, rhs) ->
                OrientedEdge(Edge.line(lhs, rhs), EdgeOrientation.Forward)
            }

            return wireCircular(edges)
        }

        fun <T : Vec<T>> wireCircular(vararg edges: Edge<T>) : Loop<T> {
            return wireCircular(edges.map { OrientedEdge(it, EdgeOrientation.Forward) })
        }

        fun <T : Vec<T>> wireCircular(vararg edges: OrientedEdge<T>) : Loop<T> {
            return wireCircular(edges.toList())
        }

        fun <T : Vec<T>> combine(vararg loops: Loop<T>) : Loop<T> {
            return ofLoops(loops.toList())
        }

        fun <T : Vec<T>> wireCircular(edges: Iterable<OrientedEdge<T>>) : Loop<T> {
            val iterator = edges.iterator()
            if(!iterator.hasNext()) throw RuntimeException("One is required")

            val firstEdge = iterator.next()
            val firstLoop = Loop(firstEdge)
            var prevEdgeLoop = firstLoop
            var currentEdgeLoop = firstLoop

            while(iterator.hasNext()){
                currentEdgeLoop = Loop(iterator.next())
                prevEdgeLoop.followedBy(currentEdgeLoop)
                prevEdgeLoop = currentEdgeLoop
            }

            currentEdgeLoop.followedBy(firstLoop)
            return firstLoop
        }

        fun <T : Vec<T>> ofLoops(loops: Iterable<Loop<T>>) : Loop<T> {
            val iterator = loops.iterator()
            if(!iterator.hasNext()) throw RuntimeException("One is required")

            val firstLoop = iterator.next()
            var prevEdgeLoop = firstLoop
            var currentEdgeLoop = firstLoop

            while(iterator.hasNext()){
                currentEdgeLoop = iterator.next()
                prevEdgeLoop.followedBy(currentEdgeLoop)
                prevEdgeLoop = currentEdgeLoop
            }

            currentEdgeLoop.followedBy(firstLoop)
            return firstLoop
        }

        fun <T : Vec<T>> polygon(vararg vertices: T) : Loop<T>{
            return polygon(vertices.map { Vertex(it) })
        }

        fun <T : Vec<T>> polygon(vararg vertices: Vertex<T>) : Loop<T>{
            return polygon(vertices.asIterable())
        }

        fun <T : Vec<T>> polygon(vertices: Iterable<Vertex<T>>) : Loop<T>{
            val edges = arrayListOf<OrientedEdge<T>>()

            for((lhs, rhs) in vertices.asIterable().rollover() ){
                edges.add(OrientedEdge(Edge.line(lhs, rhs)))
            }

            return wireCircular(*edges.toTypedArray())
        }
    }
}

class StarLoopIterator<T : Vec<T>>(loop : Loop<T>) : Iterator<Loop<T>>{
    var backLoop = loop.twin!!
    var currentNext = loop.next

    override fun hasNext(): Boolean {
        return currentNext !== backLoop
    }

    override fun next(): Loop<T> {
        val result = currentNext
        currentNext = currentNext.twin!!.next
        return result
    }
}

class LoopIterator<T : Vec<T>>(val init : Loop<T>) : Iterator<Loop<T>>{
    var first = true
    var current = init
    var watchdog = 0
    override fun hasNext(): Boolean {
        return first || init !== current
    }

    override fun next(): Loop<T> {
        val result = current
        current = current.next
        first = false
        if(watchdog > 10000){
            //println("watchdog")
            throw RuntimeException("sss")
        }
        watchdog++
        return result
    }

}