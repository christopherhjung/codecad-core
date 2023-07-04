package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD

class PolygonFace(val positions: List<PointD>, override val type: FaceType, override val plane: Plane) : Face() {
    val holes = mutableSetOf<PolygonFace>()
    var area: Double = 0.0

    override fun generateTriangles()  : List<TriangleFace>{
        return generateTriangles(positions, holes.map { it.positions })
    }

    override fun toString(): String {
        return "PolygonFace(positions=$positions)"
    }
}