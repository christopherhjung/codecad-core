package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.WorkplaneExpr

open class Ellipse(workplane: Workplane, val major: Double, val minor: Double) : Conic(workplane) {
    override fun move(offset: Vec3): Curve {
        return Ellipse(workplane.move(offset), major, minor)
    }

    override fun invert(): Curve {
        return Ellipse(workplane.invert(), major, minor)
    }
}