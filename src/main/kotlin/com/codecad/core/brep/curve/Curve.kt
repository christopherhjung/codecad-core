package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec3Expr


abstract class Curve(){
    abstract fun move(offset : Vec3Expr) : Curve
}

