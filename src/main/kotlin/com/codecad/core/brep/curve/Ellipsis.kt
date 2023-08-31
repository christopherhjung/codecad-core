package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.WorkplaneExpr

open class Ellipsis(workplane: WorkplaneExpr, val major: Expr, val minor: Expr) : Conic(workplane) {
    override fun move(offset: Vec3Expr): Curve {
        return Ellipsis(workplane.move(offset), major, minor)
    }

    override fun invert(): Curve {
        return Ellipsis(workplane.invert(), major, minor)
    }
}