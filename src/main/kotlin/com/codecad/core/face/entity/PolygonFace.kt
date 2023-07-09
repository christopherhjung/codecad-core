package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD

class PolygonFace(val positions: List<PointD> ) : Face() {

    override fun generateTriangles()  : List<TriangleFace>{
        return generateTriangles(positions, children.map { it.points.toList() })
    }

    override fun toString(): String {
        return "PolygonFace(positions=$positions)"
    }

    override val points: Iterable<PointD>
        get() = positions
}
