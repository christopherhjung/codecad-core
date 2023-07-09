package com.codecad.core.face.entity

import com.codecad.common.PointD


data class Edge(val source: Corner, val target : Corner){
    var next: Edge? = null
    lateinit var twin : Edge

    companion object{
        fun withAdd(source: Corner, target: Corner) : Edge {
            val edge = Edge(source, target)
            source.addEdge(edge)
            return edge
        }

        fun withAdd(source: PointD, target: PointD) : Edge {
            return withAdd(Corner(source), Corner(target))
        }

        fun twinEachOther(left: Edge, right: Edge){
            left.twin = right
            right.twin = left
        }

        val ZERO = run{
            val corner = Corner(PointD.ZERO)
            val edge = Edge(corner, corner)
            edge.next = edge
            edge
        }
    }

    fun points() : List<PointD>{
        val points = arrayListOf<PointD>()
        var curr = this

        while(true){
            val currPos = curr.target.point
            points.add(currPos)
            if(curr.target === this.source) break
            curr = curr.next!!
        }

        return points
    }

    fun loop() : Iterable<Edge>{
        return Iterable {
            val start : Edge = this
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

    fun pointsIter() : Iterable<PointD>{
        return Iterable {
            val start : Edge = this
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