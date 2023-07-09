package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD

class PolygonFace(val positions: List<PointD>, override val type: FaceType, override val plane: Plane) : Face() {
    override var children : MutableList<PolygonFace> = mutableListOf()
    var area: Double = 0.0

    override fun generateTriangles()  : List<TriangleFace>{
        return generateTriangles(positions, children.map { it.positions })
    }

    override fun toString(): String {
        return "PolygonFace(positions=$positions)"
    }

    override val points: Iterable<PointD>
        get() = positions
}
