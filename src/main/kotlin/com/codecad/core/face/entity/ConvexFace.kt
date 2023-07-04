package com.codecad.core.face.entity

import com.codecad.common.Plane
import com.codecad.common.PointD

open class ConvexFace( val positions: List<PointD> ) : Face() {
    override fun generateTriangles()  : List<TriangleFace>{
        val result = mutableListOf<TriangleFace>()
        for( i in 0 until positions.size - 2 ){
            result.add(TriangleFace(positions[0], positions[i + 1], positions[i + 2]))
        }

        return result
    }

    override val plane: Plane
        get() = Plane.fromPoints(positions[0], positions[1], positions[2])
}