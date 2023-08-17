package com.codecad.core.face.entity

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3

open class ConvexFace( val positions: List<Vec3> ) : Face() {
    override fun generateTriangles()  : List<TriangleFace>{
        val result = mutableListOf<TriangleFace>()
        for( i in 0 until positions.size - 2 ){
            result.add(TriangleFace(positions[0], positions[i + 1], positions[i + 2]))
        }

        return result
    }

    override val points: Iterable<Vec3>
        get() = positions
}