package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.rollover
enum class FaceBoundSense{
    Inside, Outside
}

class FaceBound(var edgeLoop : EdgeLoop, var sense: FaceBoundSense)
enum class EdgeOrientation{
    Forward, Backward;

    fun invert() : EdgeOrientation{
        return when(this){
            Forward -> Backward
            Backward -> Forward
        }
    }
}
class OrientedEdge(val edge : Edge, val orientation : EdgeOrientation = EdgeOrientation.Forward){
    val start get() = if(orientation == EdgeOrientation.Forward) edge.bound?.start else edge.bound?.end
    val end get() = if(orientation == EdgeOrientation.Forward) edge.bound?.end else edge.bound?.start
    val bound get() = if(orientation == EdgeOrientation.Forward) edge.bound else edge.bound?.let {
            EdgeBound(it.end, it.start, it.sense.invert())
        }


    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is OrientedEdge &&
                edge == other.edge &&
                orientation == other.orientation
    }

    override fun hashCode(): Int {
        var result = edge.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }
}

class EdgeLoop(var edge : OrientedEdge) : Iterable<EdgeLoop>{
    lateinit var prev : EdgeLoop
    lateinit var next : EdgeLoop
    var orientation : Boolean = false

    override fun iterator(): Iterator<EdgeLoop> {
        return EdgeLoopIterator(this)
    }

    companion object{
        fun of(edge: Edge) : EdgeLoop {
            val loop = EdgeLoop(OrientedEdge(edge))
            loop.next = loop
            loop.prev = loop
            return loop
        }

        fun forward(vararg edges: Edge) : EdgeLoop {
            return of(edges.map { OrientedEdge(it, EdgeOrientation.Forward) })
        }

        fun of(vararg edges: OrientedEdge) : EdgeLoop {
            return of(edges.toList())
        }

        fun of(edges: Iterable<OrientedEdge>) : EdgeLoop {
            val iterator = edges.iterator()
            if(!iterator.hasNext()) throw RuntimeException("One is required")

            val firstEdge = iterator.next()
            val firstLoop = EdgeLoop(firstEdge)
            var prevEdgeLoop = firstLoop
            var currentEdgeLoop = firstLoop

            while(iterator.hasNext()){
                val currentEdge = iterator.next()
                currentEdgeLoop = EdgeLoop(currentEdge)
                currentEdgeLoop.prev = prevEdgeLoop
                prevEdgeLoop.next = currentEdgeLoop
                prevEdgeLoop = currentEdgeLoop
            }

            firstLoop.prev = currentEdgeLoop
            currentEdgeLoop.next = firstLoop
            return firstLoop
        }

        fun polygon(vararg vertices: Vec3Expr) : EdgeLoop{
            return polygon(vertices.map { Vertex(it) })
        }

        fun polygon(vararg vertices: Vertex) : EdgeLoop{
            return polygon(vertices.asIterable())
        }

        fun polygon(vertices: Iterable<Vertex>) : EdgeLoop{
            val edges = arrayListOf<OrientedEdge>()

            for((lhs, rhs) in vertices.asIterable().rollover() ){
                edges.add(OrientedEdge(Edge.line(lhs, rhs)))
            }

            return of(*edges.toTypedArray())
        }
    }
}

class EdgeLoopIterator(val init : EdgeLoop) : Iterator<EdgeLoop>{
    var first = true
    var current = init
    override fun hasNext(): Boolean {
        return first || init !== current
    }

    override fun next(): EdgeLoop {
        val result = current
        current = current.next
        first = false
        return result
    }

}