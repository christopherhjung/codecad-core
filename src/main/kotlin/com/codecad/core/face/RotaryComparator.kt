package com.codecad.core.face

import com.codecad.common.Plane
import com.codecad.core.face.entity.DirectedPlane
import com.codecad.core.face.entity.Edge
import kotlin.math.atan2


object RotaryComparator : Comparator<Edge>{
    private fun absoluteAngle(p1: Edge): Double {
        val aDirection = p1.target.point - p1.source.point
        return atan2(aDirection.x, aDirection.y)
    }

    override fun compare(lhs: Edge, rhs: Edge): Int {
        return absoluteAngle(lhs).compareTo(absoluteAngle(rhs))
    }
}


class RotaryComparator3D(plane: Plane) : Comparator<Edge>{
    private val directedPlane = DirectedPlane.from(plane)

    private fun absoluteAngle(p1: Edge): Double {
        val aDirection = p1.target.point - p1.source.point
        return atan2(aDirection.dot(directedPlane.first), aDirection.dot(directedPlane.second))
    }

    override fun compare(lhs: Edge, rhs: Edge): Int {
        return absoluteAngle(lhs).compareTo(absoluteAngle(rhs))
    }
}