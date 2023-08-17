package com.codecad.core.face

/*
import com.codecad.core.face.entity.sketch.SketchVertex
class RoutedFace(val root : SketchEdge) : Face(), Iterable<SketchEdge>{

    override val points: Iterable<PointD>
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

    fun points() : Iterable<PointD>{
        return Iterable {
            var current : SketchEdge = root
            var first = true
            object : Iterator<PointD>{
                override fun hasNext(): Boolean {
                    return first || current != root
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

    override fun generateTriangles(): List<TriangleFace> {
        return generateTriangles(root.pointsIter().toList(), children.map { it.points.toList() })
    }
}*/
