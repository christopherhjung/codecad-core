package com.codecad.core.face


import com.codecad.core.ast.vec.Vec2
import com.codecad.core.sketch.FaceType
import com.codecad.core.sketch.SketchEdge
import com.codecad.core.sketch.SketchVertex

class SketchEdgeLoop(val root : SketchEdge) : Iterable<SketchEdge>{
    var type = FaceType.Surface
    var children :  MutableList<SketchEdgeLoop> = arrayListOf()
    var area = 0.0

    val points: Iterable<Vec2>
        get() = points()

    override fun iterator(): Iterator<SketchEdge> {
        var current : SketchEdge = root
        var first = true

        return object : Iterator<SketchEdge>{
            override fun hasNext(): Boolean {
                return first || current != root
            }

            override fun next(): SketchEdge {
                first = false
                val result =  current//.source!!//.node.p
                current = current.next!!
                return result
            }
        }
    }

    fun points() : Iterable<Vec2>{
        return Iterable {
            var current : SketchEdge = root
            var first = true
            object : Iterator<Vec2>{
                override fun hasNext(): Boolean {
                    return first || current != root
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

    fun corners() : Iterable<SketchVertex>{
        return Iterable {
            var current : SketchEdge = root
            var first = true
            object : Iterator<SketchVertex>{
                override fun hasNext(): Boolean {
                    return first || current != root
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
}
