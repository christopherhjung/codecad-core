package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec
import com.codecad.core.ast.vec.Vec3

class BSplineControlPoint<T : Vec<T>>(val point : T, val weight: Double)

class BSpline<T : Vec<T>>(var points : Array<BSplineControlPoint<T>>) : Curve<T>(){
    val degree : Int = points.size

    override fun move(offset: T): BSpline<T> {
        return BSpline(points.map {
            BSplineControlPoint(it.point + offset, it.weight)
        }.toTypedArray())
    }
}