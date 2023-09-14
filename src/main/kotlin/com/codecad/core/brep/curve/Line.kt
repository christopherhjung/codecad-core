package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.WorkplaneExpr

class Line(var origin : Vec3Expr, var direction : Vec3Expr) : Curve(){
    companion object{
        fun fromTo(start : Vec3Expr, end : Vec3Expr) : Line {
            return Line(start, (end - start).normalized())
        }
    }

    override fun move(offset: Vec3Expr): Curve {
        return Line(origin + offset, direction)
    }

    fun projectPoint(point: Vec3Expr) : Vec3Expr{
        val offset = point - origin
        return origin + Vec3Expr.project(offset, direction)
    }

    fun alignWorkplane(point: Vec3Expr) : WorkplaneExpr{
        val center = projectPoint(point)
        val radial = point - center
        return WorkplaneExpr(center, direction, radial.normalized())
    }

    fun distanceTo(point: Vec3Expr) : Expr {
        return (point - projectPoint(point)).length()
    }
}