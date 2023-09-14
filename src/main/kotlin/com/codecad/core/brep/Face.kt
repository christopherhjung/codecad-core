package com.codecad.core.brep

import com.codecad.core.brep.surface.Surface


class Face(var surface : Surface, var bounds : List<FaceBound>){
    init {
        bounds.forEach { bound ->
            bound.edgeLoop.forEach {
                it.face = this
            }
        }
    }
}

