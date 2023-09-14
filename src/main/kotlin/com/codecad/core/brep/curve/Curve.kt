package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec3


abstract class Curve(){
    abstract fun move(offset : Vec3) : Curve
    open fun invert() : Curve{
        return this
    }
}

