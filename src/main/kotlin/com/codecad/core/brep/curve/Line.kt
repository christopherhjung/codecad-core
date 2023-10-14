package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr

class Line<T : Vec<T>>(var origin : T, var direction : T) : Curve<T>(){
    companion object{
        fun <T : Vec<T>> fromTo(start : T, end : T) : Line<T> {
            val diff = end - start
            if(diff.length() < 1e-10){
                throw RuntimeException("sxx")
            }
            return Line((start + end) / 2.0, diff.normalized())
        }
    }

    override fun move(offset: T): Curve<T> {
        return Line(origin + offset, direction)
    }

    fun projectPoint(point: T) : T{
        val offset = point - origin
        return origin + offset.projectOn(direction)
    }

    fun alignWorkplane(point: T) : Workplane<T>{
        val center = projectPoint(point)
        val radial = point - center
        return Workplane(center, direction, radial.normalized())
    }

    fun distanceTo(point: T) : Double {
        return (point - projectPoint(point)).length()
    }

    override fun toString(): String {
        return "Line(origin=$origin, direction=$direction)"
    }
}