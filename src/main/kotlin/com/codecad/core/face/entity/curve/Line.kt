package com.codecad.core.face.entity.curve

import com.codecad.core.ast.vec.Vec3Expr

class Line(var point : Vec3Expr, var direction : Vec3Expr) : Curve(){


    companion object{
        fun fromTo(start : Vec3Expr, end : Vec3Expr) : Line{
            return Line(start, end - start)
        }
    }
}