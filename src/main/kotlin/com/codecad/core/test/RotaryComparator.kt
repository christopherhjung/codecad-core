package com.codecad.core.test

import com.codecad.common.Plane
import kotlin.math.atan2

class RotaryComparator(plane: Plane) : (Edge) -> Double{
    private val directedPlane = DirectedPlane.from(plane)

    override fun invoke(p1: Edge): Double {
        val aDirection = (p1.target.point - p1.source.point)//.normalized()
        return atan2(aDirection.dot(directedPlane.first), aDirection.dot(directedPlane.second))
    }
}
