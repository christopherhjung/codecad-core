package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2

data class SketchEdge(val source: SketchVertex, val target : SketchVertex){
    var next: SketchEdge? = null
    lateinit var twin : SketchEdge

    companion object{
        fun twinEachOther(left: SketchEdge, right: SketchEdge){
            left.twin = right
            right.twin = left
        }

        val ZERO = run{
            val vertex = SketchVertex(Vec2.ZERO)
            val edge = SketchEdge(vertex, vertex)
            edge.next = edge
            edge
        }
    }

    fun points() : List<Vec2>{
        val points = arrayListOf<Vec2>()
        var curr = this

        while(true){
            val currPos = curr.target.point
            points.add(currPos)
            if(curr.target === this.source) break
            curr = curr.next!!
        }

        return points
    }

    fun loop() : Iterable<SketchEdge>{
        return Iterable {
            val start : SketchEdge = this
            var current : SketchEdge = this
            var first = true
            object : Iterator<SketchEdge>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): SketchEdge {
                    first = false
                    val result =  current
                    current = current.next!!
                    return result
                }
            }
        }
    }

    fun pointsIter() : Iterable<Vec2>{
        return Iterable {
            val start : SketchEdge = this
            var current : SketchEdge = this
            var first = true
            object : Iterator<Vec2>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): Vec2 {
                    first = false
                    val result =  current.source.point
                    current = current.next!!
                    return result
                }
            }
        }
    }

    fun vertices() : Iterable<SketchVertex>{
        return Iterable {
            var start : SketchEdge = this
            var current : SketchEdge = this
            var first = true
            object : Iterator<SketchVertex>{
                override fun hasNext(): Boolean {
                    return first || current != start
                }

                override fun next(): SketchVertex {
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
        if (other !is SketchEdge) return false

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