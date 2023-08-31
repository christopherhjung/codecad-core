package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr

class Line(var origin : Vec3Expr, var direction : Vec3Expr) : Curve(){
    companion object{
        fun fromTo(start : Vec3Expr, end : Vec3Expr) : Line {
            return Line(start, (end - start).normalized())
        }
    }

    override fun move(offset: Vec3Expr): Curve {
        return Line(origin + offset, direction)
    }

    fun project(point: Vec3Expr) : Vec3Expr{
        val offset = point - origin
        return origin + direction * ( offset.dot(direction) / direction.squaredLength() )
    }

    fun distanceTo(point: Vec3Expr) : Expr {
        return (point - project(point)).length()
    }
}