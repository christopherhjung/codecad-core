package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.rollover
enum class FaceBoundSense{
    Inside, Outside
}

class FaceBound(var edgeLoop : EdgeLoop, var sense: FaceBoundSense)

class EdgeLoop(var edge : Edge) : Iterable<EdgeLoop>{
    var orientation : Boolean = false
    lateinit var twin : EdgeLoop

    lateinit var next : EdgeLoop
    lateinit var prev : EdgeLoop

    override fun iterator(): Iterator<EdgeLoop> {
        return EdgeLoopIterator(this)
    }

    companion object{
        fun of(edge: Edge) : EdgeLoop {
            val loop = EdgeLoop(edge)
            loop.next = loop
            loop.prev = loop
            return loop
        }

        fun of(vararg edges: Edge) : EdgeLoop {
            return of(edges.toList())
        }

        fun of(edges: Iterable<Edge>) : EdgeLoop {
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
            val edges = arrayListOf<Edge>()

            for((lhs, rhs) in vertices.map { Vertex(it) }.rollover() ){
                edges.add(Edge.line(lhs, rhs))
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