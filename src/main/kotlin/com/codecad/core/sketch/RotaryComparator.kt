package com.codecad.core.sketch

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

