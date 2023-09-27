package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Vertex

object RotaryEdgeComparator : Comparator<SketchEdge>{
    override fun compare(lhs: SketchEdge, rhs: SketchEdge): Int {
        return Vec2.rotaryCmp(Vec2.DirX, lhs.target.point - lhs.source.point, rhs.target.point - rhs.source.point)
    }
}

class RotaryVertexComparator(private val center : Vec2, private val reference : Vec2 = Vec2.DirX) : Comparator<Vertex<Vec2>>{
    override fun compare(lhs: Vertex<Vec2>, rhs: Vertex<Vec2>): Int {
        return Vec2.rotaryCmp(reference, lhs.point - center, rhs.point - center)
    }
}

