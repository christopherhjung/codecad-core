package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec3Expr

class Line(var point : Vec3Expr, var direction : Vec3Expr) : Curve(){
    companion object{
        fun fromTo(start : Vec3Expr, end : Vec3Expr) : Line {
            return Line(start, (end - start).normalized())
        }
    }

    override fun move(offset: Vec3Expr): Curve {
        return Line(point + offset, direction)
    }
}