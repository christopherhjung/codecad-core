package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr

data class Vertex(val point: Vec3){
    override fun toString(): String {
        return point.toString()
    }
}