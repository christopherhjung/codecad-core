package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr

open class Circle(workplane: Workplane, val radius: Double) : Conic(workplane) {
    override fun move(offset: Vec3): Curve {
        return Circle(workplane.move(offset), radius)
    }

    override fun invert(): Curve {
        return Circle(workplane.invert(), radius)
    }

    fun rightmostPoint() : Vec3{
        return workplane.unproject(radius, 0.0)
    }
}