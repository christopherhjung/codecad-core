package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec3

class BSplineControlPoint(val point : Vec3, val weight: Double)

class BSpline(var points : Array<BSplineControlPoint>) : Curve(){
    val degree : Int = points.size

    override fun move(offset: Vec3): BSpline {
        return BSpline(points.map {
            BSplineControlPoint(it.point + offset, it.weight)
        }.toTypedArray())
    }
}