package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr

class Vertex<T : Any>(val point: T){
    override fun toString(): String {
        return point.toString()
    }
}