package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.brep.unproject

open class Circle<T : Vec<T>>(workplane: Workplane<T>, val radius: Double) : Conic<T>(workplane) {
    override fun move(offset: T): Curve<T> {
        return Circle(workplane.move(offset), radius)
    }

    override fun invert(): Curve<T> {
        return Circle(workplane.invert(), radius)
    }
}

fun Circle<Vec3>.rightmostPoint() : Vec3{
    return workplane.unproject(radius, 0.0)
}