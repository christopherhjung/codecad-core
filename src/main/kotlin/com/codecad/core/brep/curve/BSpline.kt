package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec3Expr

class BSpline(var point : Vec3Expr, var direction : Vec3Expr) : Curve(){
    companion object{
        fun fromTo(start : Vec3Expr, end : Vec3Expr) : BSpline {
            return BSpline(start, (end - start).normalized())
        }
    }

    override fun move(offset: Vec3Expr): Curve {
        return BSpline(point + offset, direction)
    }
}