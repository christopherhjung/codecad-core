package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr

class Line(var origin : Vec3, var direction : Vec3) : Curve(){
    companion object{
        fun fromTo(start : Vec3, end : Vec3) : Line {
            return Line(start, (end - start).normalized())
        }
    }

    override fun move(offset: Vec3): Curve {
        return Line(origin + offset, direction)
    }

    fun projectPoint(point: Vec3) : Vec3{
        val offset = point - origin
        return origin + Vec3.project(offset, direction)
    }

    fun alignWorkplane(point: Vec3) : Workplane{
        val center = projectPoint(point)
        val radial = point - center
        return Workplane(center, direction, radial.normalized())
    }

    fun distanceTo(point: Vec3) : Double {
        return (point - projectPoint(point)).length()
    }
}