package com.codecad.core.sketch

import com.codecad.core.Utils
import com.codecad.core.ast.vec.Vec2
import kotlin.math.atan2


object RotaryComparator : Comparator<SketchEdge>{
    private fun absoluteAngle(p1: SketchEdge): Double {
        val aDirection = p1.target.point - p1.source.point
        return atan2(aDirection.x, aDirection.y)
    }
    override fun compare(lhs: SketchEdge, rhs: SketchEdge): Int {
        return absoluteAngle(lhs).compareTo(absoluteAngle(rhs))
    }
}

class RotaryVec2Comparator(private val center : Vec2, private val reference : Vec2 = Vec2.DirX) : Comparator<Vec2>{
    val refAngle = reference.absoluteAngle() - 1e-10


    override fun compare(o1: Vec2, o2: Vec2): Int {
        val pAng1 = Utils.normalizeAngle(o1.absoluteAngle(center) - refAngle)
        val pAng2 = Utils.normalizeAngle(o2.absoluteAngle(center) - refAngle)
        return pAng1.compareTo(pAng2)
    }

    /*
    override fun compare(lhs: Vec2, rhs: Vec2): Int {
        val lhs = (lhs - center).normalized()
        val rhs = (rhs - center).normalized()
        val first = reference.crossZ(lhs)
        val second = lhs.crossZ(rhs)
        val third = rhs.crossZ(reference)
        return (first + second + third).compareTo(0.0)
    }*/
}

