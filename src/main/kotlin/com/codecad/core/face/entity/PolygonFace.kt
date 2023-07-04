package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD

class PolygonFace(val positions: List<PointD>, val type: FaceType, val plane: Plane) : Face(), HasSide {
    val holes = mutableSetOf<PolygonFace>()
    var area: Double = 0.0
    override var side: Side = Side.Unknown

    override fun generateTriangles()  : List<TriangleFace>{
        return generateTriangles(positions, holes.map { it.positions })
    }

    override fun toPlane() : Plane {
        return plane
    }

    override fun toString(): String {
        return "PolygonFace(positions=$positions)"
    }
}