package com.codecad.core.brep.curve

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr

class BSplineControlPoint(val point : Vec3Expr, val weight: Expr)

class BSpline(var points : List<BSplineControlPoint>) : Curve(){
    override fun move(offset: Vec3Expr): Curve {
        return BSpline(points.map { BSplineControlPoint(it.point + offset, it.weight) })
    }
}