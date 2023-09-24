package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.mesh.Solidify
import com.codecad.core.rollover

enum class FaceBoundKind{
    OuterBound, InnerBound
}

class FaceBound(var loop : Loop, var sense: FaceBoundKind){

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

class Loop(var edge : OrientedEdge) : Iterable<Loop>{
    lateinit var prev : Loop
    lateinit var next : Loop
    lateinit var face : Face
    var twin : Loop? = null

    fun isClosed() : Boolean{
        return prev === next
    }

    override fun iterator(): Iterator<Loop> {
        return LoopIterator(this)
    }

    fun star() : Iterable<Loop>{
        val loop = this
        return object : Iterable<Loop>{
            override fun iterator(): Iterator<Loop> {
                return StarLoopIterator(loop)
            }
        }
    }

    fun computeArea() : Double{
        var area = Vec3.Zero

        for( loop in this ){
            val edge = loop.edge
            val bound = edge.bound

            if(bound != null){
                area += bound.start.point.cross(bound.end.point)
            }else{
                val edge = edge.edge
                val curve = edge.curve


            }
        }

        return area.length() / 2
    }

    fun nextPoints(num: Int) : List<Vec3>{
        val result = arrayListOf<Vec3>()
        for( (idx, loop) in this.withIndex() ){
            if(idx == num){
                break
            }
            result.add(loop.edge.start!!.point)
        }
        return result
    }

    fun validate() : Boolean{
        val visited = hashSetOf<Vertex>()
        if(edge.bound == null){
            return true
        }

        for( loop in this ){
            if(!visited.add(loop.edge.start!!)){
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
        val visited = hashSetOf<Vertex>()
        for(edgeLoop in this){
            val orientedEdge = edgeLoop.edge
            val bound = orientedEdge.bound
            if(bound != null){
                sb.append(sep)
                    .append(bound.start)
                    .append("->")
                    .append(bound.end)

                /*
                if(bound.sense != Sense.None){
                    sb.append("(")
                        .append(bound.sense)
                        .append(")")
                }*/

                sep = "\n"
            }

            if(!visited.add(orientedEdge.start!!)){
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
        fun of(edge: Edge) : Loop {
            val loop = Loop(OrientedEdge(edge))
            loop.next = loop
            loop.prev = loop
            return loop
        }

        fun forward(vararg edges: Edge) : Loop {
            return of(edges.map { OrientedEdge(it, EdgeOrientation.Forward) })
        }

        fun of(vararg edges: OrientedEdge) : Loop {
            return of(edges.toList())
        }

        fun combine(vararg loops: Loop) : Loop {
            return ofLoops(loops.toList())
        }

        fun of(edges: Iterable<OrientedEdge>) : Loop {
            val iterator = edges.iterator()
            if(!iterator.hasNext()) throw RuntimeException("One is required")

            val firstEdge = iterator.next()
            val firstLoop = Loop(firstEdge)
            var prevEdgeLoop = firstLoop
            var currentEdgeLoop = firstLoop

            while(iterator.hasNext()){
                val currentEdge = iterator.next()
                currentEdgeLoop = Loop(currentEdge)
                currentEdgeLoop.prev = prevEdgeLoop
                prevEdgeLoop.next = currentEdgeLoop
                prevEdgeLoop = currentEdgeLoop
            }

            firstLoop.prev = currentEdgeLoop
            currentEdgeLoop.next = firstLoop
            return firstLoop
        }

        fun ofLoops(loops: Iterable<Loop>) : Loop {
            val iterator = loops.iterator()
            if(!iterator.hasNext()) throw RuntimeException("One is required")

            val firstLoop = iterator.next()
            var prevEdgeLoop = firstLoop
            var currentEdgeLoop = firstLoop

            while(iterator.hasNext()){
                currentEdgeLoop = iterator.next()
                currentEdgeLoop.prev = prevEdgeLoop
                prevEdgeLoop.next = currentEdgeLoop
                prevEdgeLoop = currentEdgeLoop
            }

            firstLoop.prev = currentEdgeLoop
            currentEdgeLoop.next = firstLoop
            return firstLoop
        }

        fun polygon(vararg vertices: Vec3) : Loop{
            return polygon(vertices.map { Vertex(it) })
        }

        fun polygon(vararg vertices: Vertex) : Loop{
            return polygon(vertices.asIterable())
        }

        fun polygon(vertices: Iterable<Vertex>) : Loop{
            val edges = arrayListOf<OrientedEdge>()

            for((lhs, rhs) in vertices.asIterable().rollover() ){
                edges.add(OrientedEdge(Edge.line(lhs, rhs)))
            }

            return of(*edges.toTypedArray())
        }
    }
}

class StarLoopIterator(loop : Loop) : Iterator<Loop>{
    var backLoop = loop.twin!!
    var currentNext = loop.next

    override fun hasNext(): Boolean {
        return currentNext !== backLoop
    }

    override fun next(): Loop {
        val result = currentNext
        currentNext = currentNext.twin!!.next
        return result
    }
}

class LoopIterator(val init : Loop) : Iterator<Loop>{
    var first = true
    var current = init
    var watchdog = 0
    override fun hasNext(): Boolean {
        return first || init !== current
    }

    override fun next(): Loop {
        val result = current
        current = current.next
        first = false
        if(watchdog > 10000){
            println("watchdog")
            init.toString()
            throw RuntimeException("sss")
        }
        watchdog++
        return result
    }

}