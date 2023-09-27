package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec3


abstract class Curve<T : Vec<T>>(){
    abstract fun move(offset : T) : Curve<T>
    open fun invert() : Curve<T>{
        return this
    }
}

