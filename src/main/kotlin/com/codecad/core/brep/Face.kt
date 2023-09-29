package com.codecad.core.brep

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.surface.Surface


class SketchFace(var bounds : List<FaceBound<Vec2>>)

class Face(var surface : Surface, var bounds : List<FaceBound<Vec3>>){
    init {
        finish()
    }

    fun finish(){
        bounds.forEach { bound ->
            bound.loop.forEach {
                it.face = this
            }
        }
    }
}

